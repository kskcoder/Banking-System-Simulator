CREATE TYPE payment_type AS ENUM ('CARD', 'UPI');

CREATE TYPE payment_status AS ENUM ('INITIATED', 'OTP_SENT', 'OTP_VERIFIED', 'INCORRECT_OTP', 'INSUFFICIENT_BALANCE', 'FAILED', 'SUCCESS');

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    vendor_id VARCHAR(255) NOT NULL,
    from_account_number VARCHAR(64),
    to_account_number VARCHAR(64) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    type payment_type NOT NULL,
    status payment_status NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
