--liquibase formatted sql

--changeset denis.markushin:outbox-starter-schema context:outbox
create table outbox_events (
    id             uuid                      not null primary key,
    aggregate_id   uuid                      not null,
    aggregate_type text                      not null,
    event_type     text                      not null,
    payload        jsonb                     not null,
    attempts       int       default 0       not null,
    last_error     text,
    created_at     timestamptz default now() not null,
    published_at   timestamptz
);
create index idx_outbox_unpublished on outbox_events (created_at) where published_at is null;

comment on table outbox_events is 'Transactional outbox for reliable Kafka publication';
comment on column outbox_events.id is 'Outbox event primary key';
comment on column outbox_events.aggregate_id is 'Identifier of the aggregate that produced the event';
comment on column outbox_events.aggregate_type is 'Type of the aggregate that produced the event';
comment on column outbox_events.event_type is 'Type of the event stored in this row';
comment on column outbox_events.payload is 'Serialized event body published to Kafka';
comment on column outbox_events.attempts is 'Failed publish attempts; row is dead once attempts >= max-attempts';
comment on column outbox_events.last_error is 'Last publish error message for a dead/failing event';
comment on column outbox_events.created_at is 'Timestamp when the event was enqueued';
comment on column outbox_events.published_at is 'Timestamp when the event was published to Kafka; null while unpublished';
