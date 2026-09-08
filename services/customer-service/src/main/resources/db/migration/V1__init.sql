CREATE TABLE customers (
    id             uuid NOT NULL,
    name           varchar(255) NOT NULL,
    email          varchar(255) NOT NULL,
    document       varchar(255) NOT NULL,
    password_hash  varchar(255) NOT NULL,
    role           varchar(20) NOT NULL,
    created_at     timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_email UNIQUE (email),
    CONSTRAINT uk_customers_document UNIQUE (document),
    CONSTRAINT ck_customers_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
);
