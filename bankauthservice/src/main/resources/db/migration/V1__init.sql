-- USER ROLE ENUM
CREATE TYPE user_type AS ENUM (
  'USER',
  'ADMIN',
  'INTERNAL_SERVICE'
);

-- OTP ENUMS
CREATE TYPE message_type AS ENUM (
  'LOGIN_OTP',
  'REGISTER_OTP',
  'PAYMENT_OTP',
  'CREDIT',
  'DEBIT'
);

CREATE TYPE otp_status AS ENUM (
  'PENDING',
  'VERIFIED',
  'MAX_ATTEMPTS',
  'EXPIRED'
);

-- USER TABLE
CREATE TABLE usercred (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role user_type NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- OTP TABLE
CREATE TABLE otp_entries (
    id BIGSERIAL PRIMARY KEY,
    otp_hash VARCHAR(255) NOT NULL,
    type message_type NOT NULL,
    reference_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL,
    max_attempts INT NOT NULL,
    status otp_status NOT NULL
);
