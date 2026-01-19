# Banking System Simulator - Low-Level Design (LLD)

## 1. Introduction

This document provides detailed low-level design specifications for each microservice, including database schemas, API contracts, service interactions, and implementation details.

## 2. Database Schemas

### 2.1 Auth Service Database (`bankauthdb`)

#### 2.1.1 `usercred` Table
```sql
CREATE TABLE usercred (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

**Fields**:
- `id`: Primary key, auto-increment
- `username`: Unique username for login
- `email`: User email address
- `password`: Hashed password (BCrypt)
- `phone`: User phone number
- `role`: User role (USER, ADMIN)
- `created_at`: Account creation timestamp
- `updated_at`: Last update timestamp

#### 2.1.2 `otp_entries` Table
```sql
CREATE TABLE otp_entries (
    id BIGSERIAL PRIMARY KEY,
    otp_hash VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL,
    max_attempts INT NOT NULL,
    status VARCHAR(50) NOT NULL
);
```

**Fields**:
- `id`: Primary key
- `otp_hash`: Hashed OTP value
- `type`: OTP type (LOGIN, PAYMENT, etc.)
- `reference_id`: Reference identifier (user ID, payment ID)
- `created_at`: OTP creation timestamp
- `expires_at`: OTP expiration timestamp
- `attempts`: Number of verification attempts
- `max_attempts`: Maximum allowed attempts
- `status`: OTP status (ACTIVE, VERIFIED, EXPIRED, FAILED)

### 2.2 Account Service Database (`bankaccountdb`)

#### 2.2.1 `accounts` Table
```sql
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    userid BIGINT NOT NULL,
    accountnumber VARCHAR(64) NOT NULL UNIQUE,
    accounttype VARCHAR(50) NOT NULL,
    balance DOUBLE PRECISION NOT NULL
);
```

**Fields**:
- `id`: Primary key
- `userid`: Foreign key to usercred.id
- `accountnumber`: Unique account number (format: AC{random digits})
- `accounttype`: Account type (SAVINGS, CURRENT)
- `balance`: Current account balance

**Indexes**:
- Unique index on `accountnumber`
- Index on `userid` for faster lookups

### 2.3 Transaction Service Database (`banktransactiondb`)

#### 2.3.1 `transactions` Table
```sql
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transactionid VARCHAR(255) UNIQUE NOT NULL,
    fromaccountnumber VARCHAR(64),
    toaccountnumber VARCHAR(64) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    transactiontype VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    paymentid BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

**Fields**:
- `id`: Primary key
- `transactionid`: Unique transaction identifier
- `fromaccountnumber`: Source account number
- `toaccountnumber`: Destination account number
- `amount`: Transaction amount
- `transactiontype`: Type (DEBIT, CREDIT, TRANSFER)
- `status`: Status (PENDING, SUCCESS, FAILED, etc.)
- `paymentid`: Optional reference to payment record
- `created_at`: Transaction creation timestamp
- `updated_at`: Last update timestamp

**Indexes**:
- Unique index on `transactionid`
- Index on `fromaccountnumber`
- Index on `toaccountnumber`
- Index on `paymentid`

#### 2.3.2 `ledger` Table
```sql
CREATE TABLE ledger (
    id BIGSERIAL PRIMARY KEY,
    accountnumber VARCHAR(64) NOT NULL,
    transactionid VARCHAR(255) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    balance DOUBLE PRECISION NOT NULL,
    transactiontype VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

**Fields**:
- `id`: Primary key
- `accountnumber`: Account number
- `transactionid`: Reference to transaction
- `amount`: Transaction amount
- `balance`: Account balance after transaction
- `transactiontype`: Type (DEBIT, CREDIT)
- `created_at`: Entry creation timestamp

**Indexes**:
- Index on `accountnumber` for statement queries
- Index on `transactionid`
- Composite index on (`accountnumber`, `created_at`) for date range queries

### 2.4 Payment Service Database (`bankpaymentdb`)

#### 2.4.1 `payments` Table
```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    fromaccountnumber VARCHAR(64) NOT NULL,
    toaccountnumber VARCHAR(64) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    cardnumber VARCHAR(16) NOT NULL,
    status VARCHAR(50) NOT NULL,
    paymentmethod VARCHAR(50),
    vendorid VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

**Fields**:
- `id`: Primary key
- `fromaccountnumber`: Source account
- `toaccountnumber`: Destination account
- `amount`: Payment amount
- `cardnumber`: Card used for payment
- `status`: Payment status (PENDING, COMPLETED, FAILED, CANCELLED)
- `paymentmethod`: Payment method
- `vendorid`: External vendor identifier
- `created_at`: Payment creation timestamp
- `updated_at`: Last update timestamp

**Indexes**:
- Index on `fromaccountnumber`
- Index on `status`
- Index on `vendorid`

### 2.5 Card Service Database (`bankcarddb`)

#### 2.5.1 `cards` Table
```sql
CREATE TABLE cards (
    id BIGSERIAL PRIMARY KEY,
    userid BIGINT NOT NULL,
    accountnumber VARCHAR(64) NOT NULL,
    cardnumber VARCHAR(16) NOT NULL UNIQUE,
    cvv VARCHAR(3) NOT NULL,
    expirydate VARCHAR(7) NOT NULL,
    cardtype VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    limitamount DOUBLE PRECISION NOT NULL,
    usedamount DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

**Fields**:
- `id`: Primary key
- `userid`: Foreign key to usercred.id
- `accountnumber`: Associated account number
- `cardnumber`: Unique card number (16 digits)
- `cvv`: Card verification value
- `expirydate`: Expiry date (MM/YYYY format)
- `cardtype`: Card type (DEBIT, CREDIT)
- `status`: Card status (ACTIVE, INACTIVE, BLOCKED)
- `limitamount`: Spending limit
- `usedamount`: Current usage amount
- `created_at`: Card creation timestamp
- `updated_at`: Last update timestamp

**Indexes**:
- Unique index on `cardnumber`
- Index on `userid`
- Index on `accountnumber`

## 3. API Contracts

### 3.1 Auth Service APIs

#### 3.1.1 POST `/bankauthservice/auth/signup`
**Request Body**:
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "phone": "string"
}
```

**Response**: `201 Created`
```json
{
  "message": "User registered successfully",
  "userId": "long"
}
```

#### 3.1.2 POST `/bankauthservice/auth/login`
**Request Body**:
```json
{
  "username": "string",
  "password": "string"
}
```

**Response**: `200 OK`
```json
{
  "token": "string (JWT)",
  "userId": "long",
  "role": "string"
}
```

#### 3.1.3 POST `/bankauthservice/auth/generateotp`
**Headers**: `Authorization: Bearer <JWT>`

**Request Body**:
```json
{
  "type": "string"
}
```

**Response**: `200 OK`
```json
{
  "message": "OTP generated successfully"
}
```

#### 3.1.4 POST `/bankauthservice/auth/verifyotp`
**Headers**: `Authorization: Bearer <JWT>`

**Request Body**:
```json
{
  "otp": "string",
  "type": "string"
}
```

**Response**: `200 OK`
```json
{
  "message": "OTP verified successfully"
}
```

### 3.2 Account Service APIs

#### 3.2.1 POST `/bankaccountservice/accounts/create`
**Headers**: `Authorization: Bearer <JWT>`

**Request Body**:
```json
{
  "accountType": "SAVINGS | CURRENT"
}
```

**Response**: `201 Created`
```json
{
  "accountNumber": "string",
  "accountType": "string",
  "balance": 0.0
}
```

#### 3.2.2 GET `/bankaccountservice/accounts/{accountNumber}`
**Headers**: `Authorization: Bearer <JWT>`

**Response**: `200 OK`
```json
{
  "accountNumber": "string",
  "accountType": "string",
  "balance": 0.0,
  "userId": "long"
}
```

#### 3.2.3 GET `/bankaccountservice/accounts/user/{userId}`
**Headers**: `Authorization: Bearer <JWT>`

**Response**: `200 OK`
```json
[
  {
    "accountNumber": "string",
    "accountType": "string",
    "balance": 0.0
  }
]
```

### 3.3 Transaction Service APIs

#### 3.3.1 POST `/banktransactionservice/transactions/transfer`
**Headers**: `Authorization: Bearer <JWT>`

**Request Body**:
```json
{
  "fromAccountNumber": "string",
  "toAccountNumber": "string",
  "amount": 0.0
}
```

**Response**: `200 OK`
```json
{
  "transactionId": "string",
  "fromAccountNumber": "string",
  "toAccountNumber": "string",
  "amount": 0.0,
  "status": "string",
  "createdAt": "timestamp"
}
```

#### 3.3.2 GET `/banktransactionservice/transactions/history/{accountNumber}`
**Headers**: `Authorization: Bearer <JWT>`

**Query Parameters**:
- `page`: int (default: 0)
- `size`: int (default: 10)

**Response**: `200 OK`
```json
{
  "content": [
    {
      "transactionId": "string",
      "fromAccountNumber": "string",
      "toAccountNumber": "string",
      "amount": 0.0,
      "transactionType": "string",
      "status": "string",
      "createdAt": "timestamp"
    }
  ],
  "totalElements": "long",
  "totalPages": "int"
}
```

#### 3.3.3 GET `/banktransactionservice/transactions/statement/{accountNumber}`
**Headers**: `Authorization: Bearer <JWT>`

**Query Parameters**:
- `startDate`: date (ISO format)
- `endDate`: date (ISO format)

**Response**: `200 OK` (PDF file)

### 3.4 Payment Service APIs

#### 3.4.1 POST `/bankpaymentservice/payments/initiate`
**Headers**:
- `Authorization: Bearer <JWT>` (for internal)
- `X-External: 1` (for external vendors)
- `X-Vendor-Id: string`
- `X-Vendor-Secret: string`
- `X-Timestamp: string`
- `X-Signature: string`

**Request Body**:
```json
{
  "fromAccountNumber": "string",
  "toAccountNumber": "string",
  "amount": 0.0,
  "cardNumber": "string"
}
```

**Response**: `200 OK`
```json
{
  "paymentId": "long",
  "status": "PENDING",
  "message": "OTP sent to registered email"
}
```

#### 3.4.2 POST `/bankpaymentservice/payments/submitotp`
**Headers**: Same as initiate

**Request Body**:
```json
{
  "paymentId": "long",
  "otp": "string"
}
```

**Response**: `200 OK`
```json
{
  "paymentId": "long",
  "status": "COMPLETED",
  "transactionId": "string"
}
```

#### 3.4.3 POST `/bankpaymentservice/payments/demo/calculate-signature`
**Purpose**: Demo endpoint for signature calculation

**Request Body**: Payment request JSON (exact copy)

**Response**: `200 OK`
```json
{
  "signature": "string (Base64 HMAC-SHA256)"
}
```

### 3.5 Card Service APIs

#### 3.5.1 POST `/bankcardservice/cards/create`
**Headers**: `Authorization: Bearer <JWT>`

**Request Body**:
```json
{
  "accountNumber": "string",
  "cardType": "DEBIT | CREDIT"
}
```

**Response**: `201 Created`
```json
{
  "cardNumber": "string",
  "cvv": "string",
  "expiryDate": "string",
  "cardType": "string",
  "limitAmount": 0.0,
  "status": "INACTIVE"
}
```

#### 3.5.2 GET `/bankcardservice/cards/user/{userId}`
**Headers**: `Authorization: Bearer <JWT>`

**Response**: `200 OK`
```json
[
  {
    "cardNumber": "string",
    "expiryDate": "string",
    "cardType": "string",
    "status": "string",
    "limitAmount": 0.0,
    "usedAmount": 0.0
  }
]
```

## 4. Kafka Topics and Event Flows

### 4.1 Topic: `account-debit-topic`
**Producer**: Transaction Service  
**Consumer**: Account Service  
**Event Structure**:
```json
{
  "transactionId": "string",
  "fromAccountNumber": "string",
  "toAccountNumber": "string",
  "amount": 0.0,
  "transactionType": "DEBIT",
  "status": "PENDING",
  "paymentId": "long (optional)"
}
```

**Flow**:
1. Transaction Service publishes debit request
2. Account Service consumes and validates balance
3. If sufficient balance: debit account, publish success to `transaction-debit-topic`
4. If insufficient balance: publish failure to `transaction-debit-topic`

### 4.2 Topic: `account-credit-topic`
**Producer**: Transaction Service  
**Consumer**: Account Service  
**Event Structure**: Similar to debit, with `transactionType: "CREDIT"`

**Flow**:
1. Transaction Service publishes credit request
2. Account Service consumes and credits account
3. Publish response to `transaction-credit-topic`

### 4.3 Topic: `transaction-debit-topic`
**Producer**: Account Service  
**Consumer**: Transaction Service  
**Event Structure**: Same as debit request, with updated `status`

**Flow**:
1. Account Service publishes debit response
2. Transaction Service updates transaction record
3. If success and not cash withdrawal: create credit event for recipient
4. If payment-related: publish to `payment-message-topic`
5. Publish to `messaging-send-topic` for email notification

### 4.4 Topic: `transaction-credit-topic`
**Producer**: Account Service  
**Consumer**: Transaction Service  
**Event Structure**: Same as credit request, with updated `status`

**Flow**:
1. Account Service publishes credit response
2. Transaction Service updates transaction record
3. If payment-related: publish to `payment-message-topic`
4. Publish to `messaging-send-topic` for email notification

### 4.5 Topic: `payment-message-topic`
**Producer**: Transaction Service  
**Consumer**: Payment Service  
**Event Structure**:
```json
{
  "paymentId": "long",
  "status": "COMPLETED | FAILED",
  "transactionId": "string",
  "message": "string"
}
```

**Flow**:
1. Transaction Service publishes payment status
2. Payment Service consumes and updates payment record
3. Payment Service sends webhook callback to vendor

### 4.6 Topic: `messaging-send-topic`
**Producer**: Auth Service, Account Service, Transaction Service  
**Consumer**: Messaging Service  
**Event Structure**:
```json
{
  "email": "string",
  "type": "OTP | DEBIT | CREDIT",
  "message": "string",
  "otpNumber": "string (optional)"
}
```

**Flow**:
1. Service publishes message event
2. Messaging Service consumes
3. Sends email via SMTP (Gmail)

### 4.7 Topic: `transaction-debit-repaid-topic`
**Producer**: Account Service  
**Consumer**: Transaction Service  
**Purpose**: Handle repayment transactions when credit fails

### 4.8 Topic: `transaction-interest-topic`
**Producer**: Account Service  
**Consumer**: Transaction Service  
**Purpose**: Interest credit transactions

## 5. Service-to-Service Communication

### 5.1 Feign Client Interfaces

#### 5.1.1 Account Service → Auth Service
**Interface**: `AuthInterface`
```java
@FeignClient("bankauthservice")
public interface AuthInterface {
    @GetMapping("/auth/user/{userId}")
    ResponseEntity<User> getUserById(@PathVariable Long userId);
}
```

#### 5.1.2 Transaction Service → Account Service
**Interface**: `AccountInterface`
```java
@FeignClient("bankaccountservice")
public interface AccountInterface {
    @GetMapping("/accounts/{accountNumber}")
    ResponseEntity<Account> getAccount(@PathVariable String accountNumber);
}
```

#### 5.1.3 Payment Service → Multiple Services
- `AccountInterface`: Get account details
- `AuthInterface`: Verify user, validate OTP
- `CardInterface`: Validate card, check limits
- `TransactionInterface`: Initiate transactions

### 5.2 Feign Interceptor Pattern
All Feign clients use `FeignInterceptor` to:
1. Check for existing `Authorization` header (from original request)
2. If present: forward JWT token
3. If absent: add inter-service authentication headers:
   - `X-Internal-Auth`: Shared secret key
   - `X-User-Id`: Service identifier
   - `X-User-Role`: INTERNAL_SERVICE

### 5.3 Retry Mechanism
- **Max Retries**: 3 attempts
- **Backoff Strategy**: Exponential backoff (1s, 2s, 4s)
- **Retry Conditions**: Network errors, 5xx responses
- **Implementation**: ScheduledExecutorService with CompletableFuture

## 6. Security Implementation Details

### 6.1 JWT Token Structure
**Header**:
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload**:
```json
{
  "sub": "userId",
  "role": "USER | ADMIN",
  "iat": "issued_at_timestamp",
  "exp": "expiration_timestamp"
}
```

**Signature**: HMAC-SHA256(header + payload, secret)

### 6.2 API Gateway Security Filters

#### 6.2.1 JWT Filter (`JWTFilter`)
**Order**: 0  
**Functionality**:
1. Extract JWT from `Authorization: Bearer <token>` header
2. Validate token signature and expiration
3. Extract user ID and role
4. Add headers: `X-Internal-Auth`, `X-User-Id`, `X-User-Role`
5. Set authentication in SecurityContext

**Bypass Paths**:
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/webjars/**`
- `/bankpaymentservice/payments/demo/**`

#### 6.2.2 Vendor Signature Filter (`VendorSignatureValidationFilter`)
**Order**: -150  
**Functionality**:
1. Check for `X-External: 1` header
2. Extract vendor credentials: `X-Vendor-Id`, `X-Vendor-Secret`, `X-Timestamp`, `X-Signature`
3. Validate vendor secret against config
4. Verify HMAC-SHA256 signature:
   - Normalize request body (remove spaces, consistent formatting)
   - Concatenate: `timestamp + body`
   - Calculate: `HMAC-SHA256(concatenated_string, vendor_secret)`
   - Base64 encode
   - Compare with `X-Signature` header

#### 6.2.3 Cached Body Filter (`CachedBodyGlobalFilter`)
**Order**: -200  
**Functionality**:
1. Cache request body for signature validation
2. Only for payment endpoints: `/payments/initiate`, `/payments/submitotp`
3. Store body string in exchange attributes

### 6.3 Password Hashing
- **Algorithm**: BCrypt
- **Rounds**: 10 (default)
- **Storage**: Hashed value in database
- **Verification**: `BCrypt.checkpw(plainPassword, hashedPassword)`

### 6.4 OTP Security
- **Generation**: 6-digit random number
- **Storage**: Hashed using BCrypt
- **Expiration**: 5 minutes (configurable)
- **Max Attempts**: 3 attempts
- **Status Tracking**: ACTIVE → VERIFIED/EXPIRED/FAILED

## 7. Caching Strategy

### 7.1 Redis Cache Keys

#### 7.1.1 Account Service
- **Key Pattern**: `account:{accountNumber}`
- **TTL**: 5 minutes
- **Invalidation**: On balance update, account creation

#### 7.1.2 Card Service
- **Key Pattern**: `card:{cardNumber}`
- **TTL**: 10 minutes
- **Invalidation**: On limit update, card status change

### 7.2 Cache Implementation
- **Technology**: Spring Cache with Redis
- **Annotations**: `@Cacheable`, `@CacheEvict`, `@CachePut`
- **Configuration**: RedisTemplate with Jackson serialization

## 8. Error Handling

### 8.1 Global Exception Handler Pattern
Each service implements `@ControllerAdvice` with:
- `GlobalExceptionHandler` class
- Standardized error response format
- HTTP status code mapping

**Error Response Format**:
```json
{
  "timestamp": "ISO-8601 timestamp",
  "status": "HTTP status code",
  "error": "Error type",
  "message": "Error message",
  "path": "Request path"
}
```

### 8.2 Exception Types
- `ResourceNotFoundException`: 404 Not Found
- `BadRequestException`: 400 Bad Request
- `UnauthorizedException`: 401 Unauthorized
- `ForbiddenException`: 403 Forbidden
- `AlreadyExistsException`: 409 Conflict
- `InternalServerException`: 500 Internal Server Error

### 8.3 Transaction Rollback
- **Scenario**: Debit succeeds but credit fails
- **Mechanism**: Repayment transaction via `transaction-debit-repaid-topic`
- **Flow**: Credit failure → Create repay event → Debit original account → Update transaction status

## 9. Scheduled Tasks

### 9.1 Interest Calculation (Account Service)
- **Frequency**: Monthly on 1st day at midnight (Asia/Kolkata timezone)
- **Cron Expression**: `@Scheduled(cron = "0 0 0 1 * ?", zone = "Asia/Kolkata")`
- **Logic**: 
  - Calculate monthly interest for SAVINGS accounts only
  - Interest rates: 3.5% annually for balances < ₹1,00,000, 7% annually for balances ≥ ₹1,00,000
  - Formula: `balance * (interestRate / 1200)` - converts annual rate to monthly
  - Updates account balance in database
  - Publishes transaction event to `transaction-interest-topic` via Kafka
- **Service Class**: `InterestService`
- **Method**: `processMonthlyInterest()`
- **Dependencies**: `AccountRepo`, `AccountRules`, `AccountTransactionProducer`

### 9.2 Daily Limit Reset (Card Service)
- **Frequency**: Daily at midnight
- **Cron Expression**: `@Scheduled(cron = "0 0 0 * * ?")`
- **Logic**: 
  - Resets daily card spending limit for all ACTIVE cards
  - Fetches account type for each card via Feign client (`AccountInterface`)
  - Sets daily limit based on account type:
    - SAVINGS accounts: ₹50,000
    - CURRENT accounts: ₹200,000
  - Updates card record in database
- **Service Class**: `LimitResetService`
- **Method**: `resetDailyLimit()`
- **Dependencies**: `CardRepo`, `CardGenerals`, `AccountInterface` (Feign)

## 10. Configuration Management

### 10.1 Config Server Structure
```
Config Repository (Git)
├── application.yml (default)
├── bankauthservice.yml
├── bankaccountservice.yml
├── banktransactionservice.yml
├── bankpaymentservice.yml
├── bankcardservice.yml
└── bankmessagingservice.yml
```

### 10.2 Configuration Properties

#### 10.2.1 Common Properties
- `spring.application.name`: Service name
- `server.port`: Service port
- `eureka.client.service-url.defaultZone`: Eureka URL
- `spring.config.import`: Config server URL
- `spring.datasource.url`: Database URL
- `spring.kafka.bootstrap-servers`: Kafka brokers
- `spring.data.redis.host`: Redis host

#### 10.2.2 Service-Specific Properties
- `interServiceSecretKey`: Shared secret for inter-service auth
- `jwt.secret`: JWT signing secret
- `jwt.expiration`: Token expiration time
- `payment.callbackUrl.{vendorId}`: Webhook URLs
- `vendor.key.{vendorId}`: Vendor secret keys
- `spring.mail.*`: SMTP configuration (optional)

## 11. Deployment Configuration

### 11.1 Dockerfile Structure
Each service follows multi-stage build:
```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE <port>
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 11.2 Docker Compose Service Configuration
- **Networks**: All services on `banking-network`
- **Depends On**: Health check conditions
- **Environment Variables**: From `public.env` and `private.env`
- **Health Checks**: TCP port checks with retries

### 11.3 Service Startup Order
1. Infrastructure: PostgreSQL, Redis, Zookeeper, Kafka
2. Config Server
3. Service Registry
4. API Gateway
5. Microservices (parallel)

## 12. Testing Strategy

### 12.1 Testing Framework
- **JUnit 5 (Jupiter)**: Primary testing framework
- **Mockito**: Mocking framework for unit tests
- **Spring Boot Test**: Integration testing support
- **Spring Security Test**: Security context testing
- **Spring Kafka Test**: Kafka consumer/producer testing

### 12.2 Unit Tests
- **Framework**: JUnit 5 with Mockito
- **Annotations Used**:
  - `@ExtendWith(MockitoExtension.class)`: Enables Mockito integration
  - `@Mock`: Creates mock objects for dependencies
  - `@InjectMocks`: Injects mocks into the class under test
  - `@MockitoSettings(strictness = Strictness.LENIENT)`: Configures mock strictness
  - `@BeforeEach`: Setup method before each test
  - `@Test`: Marks test methods
- **Test Coverage**:
  - Service layer logic (AccountService, CardService, PaymentService, TransactionService, AuthService)
  - Utility classes (HmacUtil, CardGenerals, AccountRules)
  - Exception handlers
  - JWT service
  - Vendor validation
- **Mocking Strategy**: Mock external dependencies (repositories, Feign clients, Kafka producers)

### 12.3 Integration Tests
- Repository layer with test database
- Feign client mocking using Mockito
- Kafka consumer/producer testing with Spring Kafka Test
- Security context testing with Spring Security Test

### 12.4 API Tests
- Swagger UI for manual testing
- Postman collections
- Automated API tests (recommended)

### 12.5 Test Examples
The project includes comprehensive test suites:
- `AccountServiceTest.java`: Tests account creation, balance operations, interest calculation
- `CardServiceTest.java`: Tests card creation, verification, limit management
- `PaymentServiceTest.java`: Tests payment initiation, OTP verification, webhook handling
- `TransactionServiceTest.java`: Tests transaction processing, ledger updates
- `AuthServiceTest.java`: Tests authentication, OTP generation/verification
- `JWTServiceTest.java`: Tests JWT token generation and validation
- `VendorValidationTest.java`: Tests HMAC signature validation

## 13. Performance Considerations

### 13.1 Database Optimization
- Indexes on frequently queried columns
- Connection pooling (HikariCP)
- Batch operations for bulk inserts
- Pagination for list queries

### 13.2 Kafka Optimization
- Partition strategy for parallel processing
- Consumer group configuration
- Batch processing where applicable
- Retry with exponential backoff

### 13.3 Caching Strategy
- Cache frequently accessed data
- Appropriate TTL values
- Cache invalidation on updates
- Cache warming on service startup (optional)

## 14. Monitoring and Logging

### 14.1 Health Endpoints
- `/actuator/health`: Service health status
- `/actuator/info`: Service information
- Eureka dashboard: Service registration status

### 14.2 Logging
- **Framework**: SLF4J with Logback
- **Levels**: INFO, WARN, ERROR
- **Pattern**: Timestamp, level, logger, message
- **Recommendation**: Centralized logging (ELK Stack) for production

### 14.3 Metrics (Future Enhancement)
- Request count per endpoint
- Response times
- Error rates
- Kafka consumer lag
- Database connection pool usage
