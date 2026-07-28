# outbox-starter

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

| Property                        | Default    | Description                                |
|---------------------------------|------------|--------------------------------------------|
| `dema.outbox.enabled`           | `true`     | Master switch for the whole starter        |
| `dema.outbox.topic`             | (required) | Kafka topic to publish to                  |
| `dema.outbox.poll-interval-ms`  | `1000`     | Relay poll interval                        |
| `dema.outbox.batch-size`        | `100`      | Rows per poll                              |
| `dema.outbox.max-attempts`      | `5`        | Event is dead after this many failed sends |
| `dema.outbox.send-timeout-ms`   | `10000`    | Max wait per Kafka send acknowledgement    |
| `dema.outbox.liquibase.enabled` | `true`     | Auto-create the `outbox_events` table      |

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
  `aggregate_id`, so per-aggregate order is preserved within a partition
  (absent retries — a failed send is retried on a later poll and can be
  overtaken).
- **Delivery is at-least-once:** a crash between a successful send and the
  transaction commit re-sends the batch on the next poll. Consumers must
  deduplicate, e.g. by `eventId` from the envelope.
- **Multi-replica safe:** the relay fetches with `FOR UPDATE SKIP LOCKED`, so
  multiple instances never publish the same row twice.
- **Poison pill isolation:** a failing send increments `attempts` and records
  `last_error` for that row only; following events still publish. Once
  `attempts >= max-attempts` the row is excluded from the relay (dead).
- **Inspect dead events:**
  ```sql
  select id, event_type, attempts, last_error from outbox_events
  where published_at is null and attempts >= 5;
  ```
- **Requeue a dead event:**
  ```sql
  update outbox_events set attempts = 0, last_error = null where id = '...';
  ```
