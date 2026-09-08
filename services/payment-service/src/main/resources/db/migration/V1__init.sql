CREATE TABLE payments (
    id               uuid NOT NULL,
    customer_id      uuid NOT NULL,
    account_id       uuid NOT NULL,
    amount           numeric(19,2) NOT NULL,
    currency         varchar(3) NOT NULL,
    status           varchar(20) NOT NULL,
    reservation_id   uuid,
    failure_reason   varchar(255),
    created_at       timestamp(6) with time zone NOT NULL,
    updated_at       timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT ck_payments_status CHECK (status IN ('CREATED','PROCESSING','AUTHORIZED','CAPTURED','SETTLED','FAILED','RETRYING'))
);

CREATE TABLE payment_events (
    id           uuid NOT NULL,
    payment_id   uuid NOT NULL,
    event_type   varchar(30) NOT NULL,
    from_status  varchar(20),
    to_status    varchar(20) NOT NULL,
    created_at   timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_payment_events PRIMARY KEY (id),
    CONSTRAINT ck_payment_events_from_status CHECK (from_status IN ('CREATED','PROCESSING','AUTHORIZED','CAPTURED','SETTLED','FAILED','RETRYING')),
    CONSTRAINT ck_payment_events_to_status CHECK (to_status IN ('CREATED','PROCESSING','AUTHORIZED','CAPTURED','SETTLED','FAILED','RETRYING'))
);

CREATE TABLE idempotency_keys (
    idempotency_key      varchar(255) NOT NULL,
    request_fingerprint  varchar(64) NOT NULL,
    response_status      integer NOT NULL,
    response_body        text NOT NULL,
    created_at           timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_idempotency_keys PRIMARY KEY (idempotency_key)
);

CREATE TABLE outbox_events (
    id              uuid NOT NULL,
    aggregate_type  varchar(30) NOT NULL,
    aggregate_id    uuid NOT NULL,
    event_type      varchar(30) NOT NULL,
    routing_key     varchar(100) NOT NULL,
    payload         text NOT NULL,
    status          varchar(20) NOT NULL,
    attempts        integer NOT NULL,
    created_at      timestamp(6) with time zone NOT NULL,
    published_at    timestamp(6) with time zone,
    CONSTRAINT pk_outbox_events PRIMARY KEY (id),
    CONSTRAINT ck_outbox_events_status CHECK (status IN ('PENDING','PUBLISHED'))
);
