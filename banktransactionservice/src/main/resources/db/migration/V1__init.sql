CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    from_account VARCHAR(255),
    to_account VARCHAR(255) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE transaction_ledger (
    id BIGSERIAL PRIMARY KEY,
    parent_transaction_id BIGINT,
    account_number VARCHAR(255) NOT NULL,
    counterparty VARCHAR(255),
    type VARCHAR(50) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    balance_after DOUBLE PRECISION NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
