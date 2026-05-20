CREATE TABLE clients (
  id VARCHAR(50) primary key,
  name VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE balances (
    id BIGSERIAL primary key,
    client_id VARCHAR(50) NOT NULL REFERENCES clients(id),
    currency VARCHAR(3) NOT NULL,
    amount NUMERIC(19,4) NOT NULL default 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL default NOW(),
    CONSTRAINT balances_client_currency_uq UNIQUE (client_id, currency),
    CONSTRAINT balances_amount_non_negative CHECK (amount >= 0)
);

CREATE TABLE conversions (
    id UUID PRIMARY KEY,
    client_id VARCHAR(50) NOT NULL REFERENCES clients(id),
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    source_amount NUMERIC(19,4) NOT NULL,
    target_amount NUMERIC(19, 4) NOT NULL,
    rate NUMERIC(19,8) NOT NULL,
    created_at TIMESTAMP NOT NULL default NOW()
);

CREATE INDEX conversions_client_id_created_at_idx
    ON conversions(client_id, created_at DESC);

CREATE INDEX conversions_created_at_idx
    ON conversions(created_at DESC);

CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    conversion_id   UUID         NOT NULL REFERENCES conversions(id),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);