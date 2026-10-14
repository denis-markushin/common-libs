# outbox-starter

- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Schema](#schema)
- [Operations](#operations)

Transactional outbox for reliable event publication to Kafka. A producer writes
domain events to a database table inside the business transaction; a background
relay publishes them to Kafka and marks them published. Survives crashes, no
dual-write inconsistency.

## Quick start

1. Add the dependency:
   ```kotlin
   implementation("io.github.denis-markushin:outbox-starter")
   ```
2. Configure the topic:
   ```yaml
   dema:
     outbox:
       topic: my-service.events.v1
   ```
3. Provide a `KafkaTemplate<String, String>` bean. Spring Boot's default
   `KafkaTemplate<Object, Object>` does NOT match the injection point:
   ```kotlin
   @Bean
   fun outboxKafkaTemplate(pf: ProducerFactory<String, String>): KafkaTemplate<String, String> =
       KafkaTemplate(pf)
   ```
4. Implement `OutboxEvent` on your payloads:
   ```kotlin
   data class ProjectCreatedPayload(val projectId: UUID) : OutboxEvent {
     override val eventType get() = "ProjectCreated"
     override val aggregateType get() = "Project"
   }
   ```
5. Publish inside your business transaction:
   ```kotlin
   @Service
   class ProjectService(private val outbox: OutboxService) {
     @Transactional
     fun create(...) {
       // ... persist aggregate ...
       outbox.publish(aggregateId = project.id, payload = ProjectCreatedPayload(project.id))
     }
   }
   ```

## Configuration

| Property                        | Default    | Description                                                    |
|---------------------------------|------------|----------------------------------------------------------------|
| `dema.outbox.enabled`           | `true`     | Master switch for the whole starter                            |
| `dema.outbox.topic`             | (required) | Kafka topic to publish to                                      |
| `dema.outbox.poll-interval-ms`  | `1000`     | Relay poll interval; also the first pause after a failed poll  |
| `dema.outbox.batch-size`        | `100`      | Rows per poll                                                  |
| `dema.outbox.max-attempts`      | `5`        | Event is dead after this many failures of the event itself     |
| `dema.outbox.send-timeout-ms`   | `10000`    | Max wait per Kafka send acknowledgement                        |
| `dema.outbox.max-backoff-ms`    | `60000`    | Cap of the relay pause, which doubles per consecutive failure  |
| `dema.outbox.liquibase.enabled` | `true`     | Auto-create the `outbox_events` table                          |

## Schema

The starter auto-creates `outbox_events` on startup by applying its bundled
changelog (Liquibase context `outbox`) programmatically — it does NOT register a
`SpringLiquibase` bean, so your service's own Liquibase migrations keep running
normally. To manage the schema yourself, set `dema.outbox.liquibase.enabled=false`
and create the table with the columns: `id, aggregate_id, aggregate_type,
event_type, payload (jsonb), attempts, last_error, created_at (timestamptz),
published_at (timestamptz), seq (bigint identity)`.

## Operations

- **Relay thread:** the starter runs its own `outbox-relay` daemon thread
  (`SmartLifecycle`). It does not enable `@EnableScheduling` for your
  application and needs no scheduling setup on the consumer side. The relay
  runs its own JDBC transactions on the outbox `DataSource`, whatever
  transaction manager your application uses.
- **Ordering:** rows are published in `seq` (insertion) order and keyed by
  `aggregate_id`, so per-aggregate order is preserved within a partition. A
  failed send ends the poll and the next poll starts from that row, so later
  events never overtake it, and only one relay publishes at a time (see
  Multi-replica). `seq` is taken at insert, not at commit: of two transactions
  writing events of one aggregate at once, the one that commits first is
  published first, even with the higher `seq`. Serialize writes per aggregate
  to keep its order, e.g. update or `select ... for update` the aggregate row
  in the same transaction. Exception: a dead row is skipped.
- **Delivery is at-least-once:** a crash between a successful send and the
  transaction commit re-sends the batch on the next poll, and so does a send
  that timed out on our side but still reached the broker. Consumers must
  deduplicate, e.g. by `eventId` from the envelope.
- **Multi-replica:** every poll first takes a transaction-scoped advisory lock
  keyed by the `outbox_events` table and holds it until the batch commits. It
  uses the two-key form of `pg_try_advisory_xact_lock`, so it never meets the
  single-key locks applications take on their own ids. A relay that finds the
  lock taken skips that poll without pausing and tries again on the next one.
  Extra replicas add failover, not throughput. `FOR UPDATE SKIP LOCKED` still
  keeps two relays from sending the same row, including replicas on an older
  starter version during a rolling upgrade; order across replicas holds once
  all of them run this version. The lock ends with the transaction, so it
  works behind transaction-mode PgBouncer.
- **Vanished replica:** a host that disappears mid-batch (node crash, network
  partition) leaves its session open on the server, and the lock with it. The
  relay shortens TCP keepalive of its session for each poll, so the server
  drops that session within about 30 s (needs PostgreSQL 12+). Behind
  PgBouncer the server only sees the bouncer: set its `tcp_keepidle`,
  `tcp_keepintvl` and `tcp_keepcnt`, or an `idle_transaction_timeout` above
  `max.block.ms` plus `send-timeout-ms`.
- **Kafka outage:** the first failed send ends the poll and pauses the relay
  for `poll-interval-ms`; every consecutive failed poll doubles the pause up to
  `max-backoff-ms`, and the first clean poll lifts it. Any failure outside the
  event itself (broker unreachable or unresolvable, metadata or delivery
  timeout, `send-timeout-ms` expiring, authentication, ACL) records
  `last_error` but never counts toward `max-attempts`, so a broker outage of
  any length leaves events alive. A send interrupted by shutdown counts
  nothing either.
- **Producer timeouts:** `KafkaTemplate.send` blocks for up to the producer's
  `max.block.ms` (60 s by default) while metadata is missing, and the relay
  holds one database connection meanwhile. Keep `max.block.ms` at or below
  `send-timeout-ms`, e.g.
  ```yaml
  spring.kafka.producer.properties:
    max.block.ms: 5000
    delivery.timeout.ms: 10000
    request.timeout.ms: 9000
  ```
- **Poison pill:** a failure of the event itself (`RecordTooLargeException`,
  `RecordBatchTooLargeException`, `SerializationException`,
  `InvalidRecordException`) increments `attempts` and records `last_error`,
  then ends the poll and pauses the relay like an outage does. Once `attempts >= max-attempts`
  the row is excluded from the relay (dead) and the events behind it flow
  again. Keep `max-attempts` finite: an unbounded limit lets one poison event
  block the relay forever.
- **Inspect stuck events:** the head of the queue and why it is not published.
  ```sql
  select id, event_type, attempts, last_error from outbox_events
  where published_at is null and last_error is not null order by seq;
  ```
- **Inspect dead events:**
  ```sql
  select id, event_type, attempts, last_error from outbox_events
  where published_at is null and attempts >= 5;
  ```
- **Requeue a dead event:** it is published on a later poll, after the events
  that went out while it was dead.
  ```sql
  update outbox_events set attempts = 0, last_error = null where id = '...';
  ```
