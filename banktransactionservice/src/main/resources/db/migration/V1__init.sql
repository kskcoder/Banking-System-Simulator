CREATE TYPE transaction_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'RETRY', 'INSUFFICIENT_BALANCE', 'UNAUTHORISED', 'DEBIT_SUCCESS', 'DEBIT_FAILED', 'CREDIT_SUCCESS', 'CREDIT_FAILED', 'REPAY_PENDING', 'REPAY_SUCCESS', 'REPAY_FAILED');

CREATE TYPE transaction_type AS ENUM ('DEBIT', 'CREDIT', 'REPAY', 'INTEREST');

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    from_account VARCHAR(255),
    to_account VARCHAR(255) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    status transaction_status NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE transaction_ledger (
    id BIGSERIAL PRIMARY KEY,
    parent_transaction_id BIGINT,
    account_number VARCHAR(255) NOT NULL,
    counterparty VARCHAR(255),
    type transaction_type NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    balance_after DOUBLE PRECISION NOT NULL,
    status transaction_status NOT NULL,
    created_at TIMESTAMP NOT NULL
);
