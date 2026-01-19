# Banking System Simulator - High-Level Design (HLD)

## 1. System Overview

### 1.1 Purpose
A production-style banking backend system built using microservices architecture that supports authentication, account management, transactions, payments, card management, and messaging services.

### 1.2 Key Features
- User authentication and authorization with JWT
- Account creation and balance management
- Transaction processing (debit/credit)
- Payment processing with external vendor integration
- Card management with spending limits
- Email notifications (OTP and transaction alerts)
- Event-driven architecture using Kafka
- Centralized configuration management
- Service discovery and API gateway

### 1.3 Non-Functional Requirements
- **Scalability**: Microservices can scale independently
- **Availability**: Service discovery and health checks ensure high availability
- **Security**: JWT-based authentication, inter-service authentication, HMAC signatures for external vendors
- **Performance**: Redis caching, asynchronous event processing
- **Maintainability**: Centralized configuration, clear service boundaries
- **Reliability**: Retry mechanisms, transaction rollback capabilities

## 2. Architecture Patterns

### 2.1 Microservices Architecture
The system follows a microservices pattern where each service:
- Has its own database (database per service pattern)
- Can be deployed independently
- Communicates via REST APIs and event-driven messaging
- Registers with service discovery (Eureka)

### 2.2 Event-Driven Architecture
- Asynchronous communication via Apache Kafka
- Services publish events for other services to consume
- Decouples services and improves scalability

### 2.3 API Gateway Pattern
- Single entry point for all client requests
- Handles routing, authentication, and request/response transformation
- Aggregates Swagger documentation from all services

### 2.4 Service Discovery Pattern
- Eureka server for service registration and discovery
- Services register themselves on startup
- Dynamic service location without hardcoded URLs

### 2.5 Centralized Configuration
- Spring Cloud Config Server
- Configuration stored in Git repository
- Runtime configuration updates without service restart

## 3. System Architecture

### 3.1 High-Level Architecture Diagram

```
┌────────────────────────────┐          ┌────────────────────────────┐
│      Client Layer          │          │   External Vendor          │
│  (Web/Mobile Applications) │          │   (TPay, Payment Gateways) │◂─────┐
│                            │          │   - HMAC-SHA256 Signature  │      │
│                            │          │   - Receives Webhooks      │      │
└────────────┬───────────────┘          └─────────────┬──────────────┘      │
             │                                        │                     │
             ▼                                        ▼                     │
┌─────────────────────────────────────────────────────────────────┐         │
│                      API Gateway (8765)                         │         │
│  - Request Routing                                              │         │
│  - JWT Authentication                                           │         │
│  - Vendor Signature Validation                                  │         │
│  - CORS Handling                                                │         │
└────────────┬────────────────────────────────────────────────────┘         │
             │                                                              │
             ├─────────────────────────────────────────────────────┐        │
             │                                                     │        │
             ▼                                                     ▼        │
┌────────────────────────────┐          ┌────────────────────────────┐      │
│   Service Registry         │          │   Config Server            │      │
│   (Eureka - 8761)          │          │   (8888)                   │      │
└────────────────────────────┘          └────────────────────────────┘      │
             │                                                              │
             ├─────────────────────────────────────────────────────┐        │
             │                                                     │        │
             ▼                                                     ▼        │
┌────────────────────────────┐          ┌────────────────────────────┐      │
│   Auth Service (8080)      │          │   Account Service (8085)   │      │
│   - User Authentication    │          │   - Account Management     │      │    
│   - JWT Generation         │          │   - Balance Operations     │  Webhook Callbacks                  
│   - OTP Management         │          │   - Account Creation       │  (Payment Status)  
└────────────┬───────────────┘          └──────────────┬─────────────┘      │
             │                                         │                    │
             ▼                                         ▼                    │
┌────────────────────────────┐          ┌────────────────────────────┐      │
│   Transaction Service      │          │   Payment Service (8100)   │      │
│   (8090)                   │          │   - Payment Initiation     │──────┘
│   - Debit/Credit Tracking  │          │   - OTP Verification       │
│   - Transaction History    │          │   - Webhook Callbacks      │
└────────────┬───────────────┘          └──────────────┬─────────────┘
             │                                         │
             ▼                                         ▼
┌────────────────────────────┐          ┌────────────────────────────┐
│   Card Service (8095)      │          │   Messaging Service        │
│   - Card Management        │          │   (8105)                   │
│   - Limit Management       │          │   - Email Notifications    │
│   - Card Activation        │          │   - OTP Emails             │
└────────────────────────────┘          └────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │  PostgreSQL  │  │    Redis     │  │    Kafka     │           │
│  │  (Databases) │  │   (Cache)    │  │  (Messaging) │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
└─────────────────────────────────────────────────────────────────┘                
                                                       

Note: External vendors connect only through API Gateway. They cannot 
directly access any internal microservices. The API Gateway validates 
HMAC-SHA256 signatures before routing requests to Payment Service.
Payment Service sends webhook callbacks directly to External Vendor.
```
<｜tool▁calls▁begin｜><｜tool▁call▁begin｜>
read_file

### 3.2 Service Communication Flow

#### Synchronous Communication (REST/Feign)
- API Gateway → Microservices (via Eureka)
- Microservices → Microservices (via Feign Clients)
- Uses JWT tokens or inter-service secrets for authentication

#### Asynchronous Communication (Kafka)
- Transaction events: `account-debit-topic`, `account-credit-topic`, `transaction-debit-topic`, `transaction-credit-topic`
- Payment events: `payment-message-topic`
- Messaging events: `messaging-send-topic`
- OTP events: Published to messaging service

## 4. Component Details

### 4.1 Infrastructure Components

#### 4.1.1 PostgreSQL Database
- **Pattern**: Database per service
- **Databases**:
  - `bankauthdb` - User credentials, OTP entries
  - `bankaccountdb` - Account information
  - `banktransactiondb` - Transaction records, ledger entries
  - `bankpaymentdb` - Payment records
  - `bankcarddb` - Card information
- **Migration**: Flyway for schema versioning

#### 4.1.2 Redis
- **Purpose**: Caching and rate limiting
- **Usage**:
  - Account balance caching
  - Card information caching
  - Rate limiting for API endpoints
  - OTP storage (optional)

#### 4.1.3 Apache Kafka + Zookeeper
- **Purpose**: Event-driven messaging
- **Topics**:
  - `account-debit-topic` - Debit requests from transaction service
  - `account-credit-topic` - Credit requests from transaction service
  - `transaction-debit-topic` - Debit responses from account service
  - `transaction-credit-topic` - Credit responses from account service
  - `transaction-debit-repaid-topic` - Repayment transactions
  - `transaction-interest-topic` - Interest credit transactions
  - `payment-message-topic` - Payment status updates
  - `messaging-send-topic` - Email notification requests

### 4.2 Core Services

#### 4.2.1 Config Server (Port 8888)
- **Technology**: Spring Cloud Config
- **Configuration Source**: Git repository
- **Purpose**: Centralized configuration management
- **Features**: Dynamic configuration updates, environment-specific configs

#### 4.2.2 Service Registry - Eureka (Port 8761)
- **Technology**: Spring Cloud Netflix Eureka
- **Purpose**: Service discovery and health monitoring
- **Features**: Service registration, health checks, load balancing support

#### 4.2.3 API Gateway (Port 8765)
- **Technology**: Spring Cloud Gateway
- **Features**:
  - Request routing to microservices
  - JWT token validation
  - Vendor signature validation (HMAC-SHA256)
  - CORS configuration
  - Swagger UI aggregation
- **Security**: JWT filter, vendor signature filter

#### 4.2.4 Auth Service (Port 8080)
- **Responsibilities**:
  - User registration and authentication
  - JWT token generation and validation
  - OTP generation and verification
  - Password management
- **Database**: `bankauthdb`
- **External Dependencies**: Redis (caching), Kafka (OTP messaging)

#### 4.2.5 Account Service (Port 8085)
- **Responsibilities**:
  - Account creation and management
  - Balance operations (debit/credit)
  - Account information retrieval
  - Interest calculation
- **Scheduled Tasks**:
  - **Monthly Interest Calculation**: Runs on 1st day of every month at midnight (Asia/Kolkata timezone)
    - Calculates monthly interest for SAVINGS accounts
    - Interest rates: 3.5% (balance < ₹1,00,000) or 7% (balance ≥ ₹1,00,000) annually
    - Updates account balance and publishes interest transaction event
- **Database**: `bankaccountdb`
- **External Dependencies**: Redis (caching), Kafka (event publishing), Feign (Auth Service)

#### 4.2.6 Transaction Service (Port 8090)
- **Responsibilities**:
  - Transaction initiation and processing
  - Transaction history management
  - Ledger maintenance
  - Transaction status tracking
- **Database**: `banktransactiondb`
- **External Dependencies**: Kafka (event publishing/consuming), Feign (Account Service)

#### 4.2.7 Payment Service (Port 8100)
- **Responsibilities**:
  - Payment initiation
  - Payment OTP verification
  - Payment status management
  - Webhook callback handling
  - External vendor integration
- **Database**: `bankpaymentdb`
- **External Dependencies**: Kafka (event consumption), Feign (Account, Auth, Card, Transaction Services)

#### 4.2.8 Card Service (Port 8095)
- **Responsibilities**:
  - Card creation and management
  - Card activation/deactivation
  - Spending limit management
  - Card information retrieval
- **Scheduled Tasks**:
  - **Daily Limit Reset**: Runs daily at midnight
    - Resets daily card spending limit for all ACTIVE cards
    - Daily limits: ₹50,000 for SAVINGS accounts, ₹200,000 for CURRENT accounts
    - Fetches account type via Feign client and sets appropriate limit
- **Database**: `bankcarddb`
- **External Dependencies**: Redis (caching), Kafka (event publishing), Feign (Account Service)

#### 4.2.9 Messaging Service (Port 8105)
- **Responsibilities**:
  - Email notification sending
  - OTP email delivery
  - Transaction alert emails
- **External Dependencies**: Kafka (event consumption), SMTP (Gmail)

## 5. Data Flow

### 5.1 User Authentication Flow
```
Client → API Gateway → Auth Service → PostgreSQL
                              ↓
                         JWT Token Generated
                              ↓
                         Client receives JWT
```

### 5.2 Transaction Flow (Debit)
```
Client → API Gateway → Transaction Service
                              ↓
                    Publish to account-debit-topic
                              ↓
                    Account Service consumes event
                              ↓
                    Validate balance & debit account
                              ↓
                    Publish to transaction-debit-topic
                              ↓
                    Transaction Service updates ledger
                              ↓
                    Publish to messaging-send-topic
                              ↓
                    Messaging Service sends email
```

### 5.3 Payment Flow (External Vendor)
```
External Vendor → API Gateway (HMAC Signature Validation)
                              ↓
                    Payment Service (via API Gateway routing)
                              ↓
                    Validate Card (Card Service via Feign)
                              ↓
                    Create Payment Record
                              ↓
                    Publish OTP request to messaging-send-topic
                              ↓
                    Messaging Service sends OTP email
                              ↓
                    External Vendor submits OTP via API Gateway
                              ↓
                    Payment Service verifies OTP
                              ↓
                    Initiate Transaction (Transaction Service via Feign)
                              ↓
                    Publish payment status to payment-message-topic
                              ↓
                    Payment Service sends webhook callback → External Vendor
```

**Note**: External vendors can only access Payment Service through API Gateway. They cannot directly access any other internal services. All communication is routed through the API Gateway which validates HMAC signatures before forwarding requests.

## 6. Security Architecture

### 6.1 Authentication Mechanisms

#### 6.1.1 User Authentication
- **Method**: JWT (JSON Web Tokens)
- **Flow**: Username/password → Auth Service → JWT token
- **Token Contents**: User ID, role, expiration
- **Validation**: API Gateway validates JWT before routing

#### 6.1.2 Inter-Service Authentication
- **Method**: Shared secret keys (`X-Internal-Auth` header)
- **Usage**: Feign client requests between services
- **Headers**: `X-Internal-Auth`, `X-User-Id`, `X-User-Role`

#### 6.1.3 External Vendor Authentication
- **Method**: HMAC-SHA256 signature
- **Headers Required**: `X-Vendor-Id`, `X-Vendor-Secret`, `X-Timestamp`, `X-Signature`
- **Validation**: API Gateway validates signature before routing

### 6.2 Authorization
- **Role-Based Access Control (RBAC)**:
  - `USER` - Standard user operations
  - `ADMIN` - Administrative operations
  - `INTERNAL_SERVICE` - Service-to-service communication

### 6.3 Data Security
- Passwords stored as hashed values
- OTP stored as hashed values with expiration
- Sensitive data encrypted in transit (HTTPS recommended for production)
- Database credentials managed via environment variables

## 7. Deployment Architecture

### 7.1 Containerization
- **Technology**: Docker
- **Orchestration**: Docker Compose
- **Images**: Each service has its own Dockerfile
- **Multi-stage builds**: Optimized image sizes

### 7.2 Deployment Modes

#### 7.2.1 Local Development
- Services run individually in IDE
- Infrastructure services run locally (PostgreSQL, Redis, Kafka)
- Configuration from Config Server

#### 7.2.2 Docker Local Build
- Single Docker Compose file
- Services built from source
- Databases auto-created and seeded

#### 7.2.3 Docker Hub (Production-like)
- Pre-built images from Docker Hub
- Fast deployment
- CI/CD via GitHub Actions

### 7.3 Service Dependencies
```
Config Server (no dependencies)
    ↓
Service Registry (depends on Config Server)
    ↓
API Gateway (depends on Config Server, Service Registry)
    ↓
All Microservices (depend on Config Server, Service Registry, Infrastructure)
```

## 8. Scalability Considerations

### 8.1 Horizontal Scaling
- Each microservice can scale independently
- Stateless services enable easy scaling
- Load balancing via Eureka
- Kafka partitions enable parallel processing

### 8.2 Caching Strategy
- Redis caching for frequently accessed data
- Account balances cached
- Card information cached
- Cache invalidation on updates

### 8.3 Database Scaling
- Database per service allows independent scaling
- Read replicas can be added for read-heavy services
- Connection pooling configured per service

## 9. Monitoring and Observability

### 9.1 Health Checks
- Spring Boot Actuator endpoints (`/actuator/health`)
- Eureka health monitoring
- Docker health checks

### 9.2 Service Discovery Dashboard
- Eureka Dashboard: `http://localhost:8761`
- View registered services
- Monitor service health status

### 9.3 API Documentation
- Swagger/OpenAPI 3 for all services
- Aggregated Swagger UI via API Gateway
- Access: `http://localhost:8765/webjars/swagger-ui/index.html`

## 10. Error Handling and Resilience

### 10.1 Retry Mechanisms
- Kafka message retry with exponential backoff
- Feign client retry configuration
- Transaction rollback on failures

### 10.2 Circuit Breaker Pattern
- Feign clients support circuit breakers
- Prevents cascading failures
- Fallback mechanisms for critical operations

### 10.3 Exception Handling
- Global exception handlers per service
- Standardized error responses
- Proper HTTP status codes

## 11. Technology Stack Summary

### 11.1 Core Technologies
- **Language**: Java 17
- **Framework**: Spring Boot 3.3.4
- **Cloud**: Spring Cloud (Eureka, Config, Gateway)
- **Security**: Spring Security, JWT (jjwt)
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Messaging**: Apache Kafka 7.5.0, Zookeeper
- **Migration**: Flyway
- **API Docs**: Swagger/OpenAPI 3
- **Containerization**: Docker, Docker Compose
- **CI/CD**: GitHub Actions
- **Testing**: JUnit 5 (Jupiter), Mockito

### 11.2 Service-Specific Technologies
- **Config Server**: Spring Cloud Config, Git
- **Service Registry**: Spring Cloud Netflix Eureka
- **API Gateway**: Spring Cloud Gateway, Reactive Web
- **Auth Service**: Spring Security, JWT, BCrypt
- **Account/Transaction/Payment/Card Services**: Spring Data JPA, Feign Client
- **Messaging Service**: Spring Mail, Kafka Consumer

## 12. Future Enhancements

### 12.1 Potential Improvements
- Distributed tracing (Zipkin/Jaeger)
- Centralized logging (ELK Stack)
- API rate limiting at gateway level
- Multi-factor authentication
- Real-time notifications (WebSocket)
- Advanced analytics and reporting
- Multi-currency support
- Fraud detection system
- Audit logging service

### 12.2 Production Readiness
- SSL/TLS certificates
- Secrets management (Vault)
- Monitoring and alerting (Prometheus, Grafana)
- Backup and disaster recovery
- Performance testing and optimization
- Security scanning and penetration testing
