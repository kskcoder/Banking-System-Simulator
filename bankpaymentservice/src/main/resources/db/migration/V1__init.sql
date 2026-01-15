CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    vendor_id VARCHAR(255) NOT NULL,
    from_account_number VARCHAR(64),
    to_account_number VARCHAR(64) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
