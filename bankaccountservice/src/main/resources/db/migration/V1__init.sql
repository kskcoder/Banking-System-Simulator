
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    userid BIGINT NOT NULL,
    accountnumber VARCHAR(64) NOT NULL UNIQUE,
    accounttype VARCHAR(50) NOT NULL,
    balance DOUBLE PRECISION NOT NULL
);
