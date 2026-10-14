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

| Property                        | Default    | Description                                                      |
|---------------------------------|------------|------------------------------------------------------------------|
| `dema.outbox.enabled`           | `true`     | Master switch for the whole starter                              |
| `dema.outbox.topic`             | (required) | Kafka topic to publish to                                        |
| `dema.outbox.poll-interval-ms`  | `1000`     | Relay poll interval; also the wait after a first failed poll     |
| `dema.outbox.batch-size`        | `100`      | Rows per poll                                                    |
| `dema.outbox.max-attempts`      | `5`        | Event is dead after this many failures of the event itself       |
| `dema.outbox.send-timeout-ms`   | `10000`    | Max wait per Kafka send acknowledgement                          |
| `dema.outbox.max-backoff-ms`    | `60000`    | Cap of the wait, which doubles per failed poll; `0` turns it off |
| `dema.outbox.liquibase.enabled` | `true`     | Auto-create the `outbox_events` table                            |

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
  application and needs no scheduling setup on the consumer side.
- **Ordering:** rows are published in `seq` (insertion) order and keyed by
  `aggregate_id`, so per-aggregate order is preserved within a partition. A
  failed send ends the poll and the next poll starts from that row, so later
  events never overtake it. Exceptions: a dead row is skipped, and concurrent
  relays drain separate batches, so events of one aggregate split across them
  can interleave. Relays run concurrently with several replicas, or when your
  code calls `OutboxPublisher.publish()` while the relay thread polls.
- **Delivery is at-least-once:** a crash between a successful send and the
  transaction commit re-sends the batch on the next poll, and so does a send
  that timed out on our side but still reached the broker. Consumers must
  deduplicate, e.g. by `eventId` from the envelope.
- **Multi-replica safe:** the relay fetches with `FOR UPDATE SKIP LOCKED`, so
  multiple instances never publish the same row concurrently.
- **Kafka outage:** the first failed send ends the poll. After a poll that
  delivered nothing the relay retries in `poll-interval-ms`, its usual
  interval, and waits twice as long after each further such poll, up to
  `max-backoff-ms`; a poll that delivers anything lifts the pause. Any failure
  outside the event itself (broker unreachable or unresolvable, metadata or
  delivery timeout, `send-timeout-ms` expiring, authentication, ACL) records
  `last_error` but never counts toward `max-attempts`, so a broker outage of
  any length leaves events alive. A send interrupted by shutdown records
  nothing. The pause holds only for the relay thread: a `publish()` call from
  your code runs at once and leaves the pause as it is.
- **Producer timeouts:** a failed send holds the relay's database connection
  for up to the producer's `max.block.ms` (60 s by default) when
  `KafkaTemplate.send` itself blocks on missing metadata or a full buffer, or
  for up to `send-timeout-ms` while it waits for the acknowledgement. Lower
  `max.block.ms` to a few seconds, e.g.
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
