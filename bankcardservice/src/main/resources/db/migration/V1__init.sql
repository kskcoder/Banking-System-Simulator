
CREATE TABLE cards (
    id BIGSERIAL PRIMARY KEY,
    card_number VARCHAR(255) NOT NULL UNIQUE,
    cvv VARCHAR(255) NOT NULL,
    last_digits VARCHAR(4) NOT NULL,
    expiry_date VARCHAR(7) NOT NULL,
    card_limit DOUBLE PRECISION NOT NULL,
    status VARCHAR(50) NOT NULL,
    account_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
