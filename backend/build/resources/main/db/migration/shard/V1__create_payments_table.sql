CREATE TABLE payments (
    payment_id        UUID          PRIMARY KEY,
    store_id          VARCHAR(128)  NOT NULL,
    coffee_type       VARCHAR(32)   NOT NULL,
    price             NUMERIC(12,2) NOT NULL,
    currency          VARCHAR(3)    NOT NULL,
    loyalty_card_id   VARCHAR(128)  NOT NULL,
    idempotency_key   VARCHAR(128)  NOT NULL,
    remote_payment_id VARCHAR(128),
    created_at        TIMESTAMP     NOT NULL,
    UNIQUE (store_id, idempotency_key)
);

CREATE INDEX idx_payments_store_id ON payments(store_id);
