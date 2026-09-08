CREATE TABLE accounts (
    id               uuid NOT NULL,
    customer_id      uuid NOT NULL,
    balance          numeric(19,2) NOT NULL,
    reserved_amount  numeric(19,2) NOT NULL,
    currency         varchar(3) NOT NULL,
    created_at       timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id)
);

CREATE TABLE reservations (
    id           uuid NOT NULL,
    account_id   uuid NOT NULL,
    payment_id   uuid NOT NULL,
    amount       numeric(19,2) NOT NULL,
    status       varchar(20) NOT NULL,
    created_at   timestamp(6) with time zone NOT NULL,
    updated_at   timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_reservations PRIMARY KEY (id),
    CONSTRAINT uk_reservations_payment_id UNIQUE (payment_id),
    CONSTRAINT ck_reservations_status CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED'))
);
