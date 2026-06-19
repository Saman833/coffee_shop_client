CREATE TABLE batch_request (
    request_id        UUID          PRIMARY KEY,
    store_id          VARCHAR(128)  NOT NULL,
    status            VARCHAR(32)   NOT NULL,
    total_items       INTEGER       NOT NULL,
    processed_items   INTEGER       NOT NULL DEFAULT 0,
    succeeded_items   INTEGER       NOT NULL DEFAULT 0,
    replayed_items    INTEGER       NOT NULL DEFAULT 0,
    failed_items      INTEGER       NOT NULL DEFAULT 0,
    failure_reason    TEXT,
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP     NOT NULL
);

CREATE TABLE batch_item (
    item_id           UUID          PRIMARY KEY,
    request_id        UUID          NOT NULL REFERENCES batch_request(request_id) ON DELETE CASCADE,
    store_id          VARCHAR(128)  NOT NULL,
    idempotency_key   VARCHAR(128)  NOT NULL,
    coffee_type       VARCHAR(32)   NOT NULL,
    price             NUMERIC(12,2) NOT NULL,
    currency          VARCHAR(3)    NOT NULL,
    loyalty_card_id   VARCHAR(128)  NOT NULL,
    status            VARCHAR(32)   NOT NULL,
    attempt_count     INTEGER       NOT NULL DEFAULT 0,
    last_error        TEXT,
    remote_payment_id VARCHAR(128),
    replayed          BOOLEAN       NOT NULL DEFAULT FALSE,
    lease_until       TIMESTAMP,
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP     NOT NULL
);

CREATE INDEX idx_batch_item_request_id ON batch_item(request_id);
CREATE INDEX idx_batch_item_status_lease ON batch_item(status, lease_until);
