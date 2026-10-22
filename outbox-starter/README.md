# outbox-starter

- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Schema](#schema)
- [Operations](#operations)
- [Metrics](#metrics)

Transactional outbox for reliable event publication to Kafka. A producer writes
domain events to a database table inside the business transaction; a background
relay publishes them to Kafka and marks them published. Survives crashes, no
dual-write inconsistency.

## Quick start

1. Add the dependency:
   ```kotlin
   implementation("io.github.denis-markushin:outbox-starter:x.x.x")
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

| Property                        | Default    | Description                                                                                |
|---------------------------------|------------|--------------------------------------------------------------------------------------------|
| `dema.outbox.enabled`           | `true`     | Master switch for the whole starter                                                        |
| `dema.outbox.topic`             | (required) | Kafka topic to publish to                                                                  |
| `dema.outbox.poll-interval-ms`  | `1000`     | Relay poll interval; also the wait after a first poll that found events but delivered none |
| `dema.outbox.batch-size`        | `100`      | Rows per poll                                                                              |
| `dema.outbox.max-attempts`      | `5`        | Event is dead after this many failures of the event itself                                 |
| `dema.outbox.send-timeout-ms`   | `10000`    | Max wait per Kafka send acknowledgement                                                    |
| `dema.outbox.max-backoff-ms`    | `60000`    | Cap of that wait, which doubles per such poll in a row; `0` turns it off                   |
| `dema.outbox.liquibase.enabled` | `true`     | Auto-create the `outbox_events` table                                                      |

Metrics need no property: with `spring-boot-starter-actuator` the starter
registers its [gauges](#metrics), without Micrometer it skips them.

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
- **Kafka outage:** the first failed send ends the poll. After a poll that
  found events but delivered none the relay retries in `poll-interval-ms`, its
  usual interval, and waits twice as long after each further such poll, up to
  `max-backoff-ms`; a poll that delivers anything, or finds nothing to send,
  lifts the pause. Any failure outside the event itself (broker unreachable or
  unresolvable, metadata or delivery timeout, `send-timeout-ms` expiring,
  authentication, ACL) records `last_error` but never counts toward
  `max-attempts`, so a broker outage of any length leaves events alive. A send
  interrupted by shutdown records nothing. Alert on the [metrics](#metrics)
  rather than on the error logs.
- **Producer timeouts:** a failed send holds the relay's database connection
  for up to the producer's `max.block.ms` (60 s by default) when
  `KafkaTemplate.send` itself blocks on missing metadata or a full buffer,
  plus up to `send-timeout-ms` while it waits for the acknowledgement; every
  send of a batch can take that long. Lower `max.block.ms` to a few seconds,
  e.g.
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
  where published_at is null and attempts >= 5; -- your max-attempts
  ```
- **Requeue a dead event:** it is published on a later poll, after the events
  that went out while it was dead.
  ```sql
  update outbox_events set attempts = 0, last_error = null where id = '...';
  ```

## Metrics

With a Micrometer `MeterRegistry` in the context the starter publishes four
gauges; Prometheus names, as `/actuator/prometheus` shows them, in parentheses.
They come as a `MeterBinder` bean, which Spring Boot's metrics
auto-configuration (`spring-boot-starter-actuator`) binds to every registry;
a service that builds its `MeterRegistry` without it binds the
`outboxMetrics` bean itself. Turn them off with
`management.metrics.enable.outbox=false`.

| Gauge                                                           | Meaning                                                                                          |
|-----------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| `outbox.events.pending` (`outbox_events_pending`)               | Unpublished events the relay still tries to send; dead ones excluded                             |
| `outbox.events.oldest.age` (`outbox_events_oldest_age_seconds`) | Seconds the oldest pending event has waited; `0` when none is                                    |
| `outbox.events.dead` (`outbox_events_dead`)                     | Unpublished events with `attempts >= max-attempts`                                               |
| `outbox.relay.paused` (`outbox_relay_paused`)                   | `1` from a relay poll that found events but delivered none until one that delivers or finds none |

- **Sampling:** a scrape never touches the database. It reads the last
  sample and asks the `outbox-metrics` thread for a fresh one, so the
  `outbox_events_*` gauges lag one scrape behind the table. The first sample
  is taken at startup; until it lands, and after shutdown, they read `NaN`.
  While the database is down or slow the counts keep their last value and
  the age keeps growing from it, so a stuck head still trips its alert and
  the rest of the endpoint stays up.
- **Cost:** at most one query in flight, started by a scrape when none is
  running. It makes a single pass over the unpublished rows through the
  `idx_outbox_unpublished` partial index, so it follows the backlog, not the
  size of the table. The age comes from the database clock, so application
  clock drift does not skew it.
- **Replicas:** the three `outbox_events_*` gauges describe the shared table,
  so every replica reports the same value; aggregate them with `max` per
  service, never `sum`. `outbox_relay_paused` belongs to each replica's own
  relay; a replica that finds the relay lock taken keeps its last value, and
  with `max-backoff-ms: 0` the relay never pauses, so it stays `0`.
- **Alerts:** a stuck head shows as a growing age while the relay stays
  paused, a poison pill as a dead count above zero. Give the age rule a
  `for`: the first scrape after a gap in scraping reads the age grown since
  the last sample, which the next sample corrects. Keep `max-backoff-ms` well
  below the window of the paused rule: one poll that found events but delivered none holds
  the gauge at `1` for the whole wait, even if the broker recovers sooner.
  For example:
  ```yaml
  - alert: OutboxRelayStuck
    expr: max by (job) (outbox_events_oldest_age_seconds) > 300
    for: 2m
  - alert: OutboxRelayPaused
    expr: min_over_time(outbox_relay_paused[10m]) == 1
  - alert: OutboxDeadEvents
    expr: max by (job) (outbox_events_dead) > 0
    for: 2m
  ```
