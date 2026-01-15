CREATE TYPE account_type AS ENUM ('SAVINGS', 'CURRENT');

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    userid BIGINT NOT NULL,
    accountnumber VARCHAR(64) NOT NULL UNIQUE,
    accounttype account_type NOT NULL,
    balance DOUBLE PRECISION NOT NULL
);
