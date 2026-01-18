# 🏦 Banking Microservices System

A production-style banking backend system built using Java, Spring Boot, and Docker, following real-world microservices architecture and deployment practices.

The system supports authentication, accounts, transactions, payments, cards, messaging, and notifications, orchestrated through Spring Cloud, Kafka, Redis, and PostgreSQL.

## 🧩 Architecture Overview

### Core Components

- **Config Server** – Centralized configuration management
- **Service Registry (Eureka)** – Service discovery
- **API Gateway** – Single entry point for all client requests
- **Auth Service** – User authentication & OTP handling
- **Account Service** – Account creation & balance management
- **Transaction Service** – Debit/Credit tracking
- **Payment Service** – Payment processing
- **Card Service** – Card & limits management
- **Messaging Service** – Email notifications (OTP & transaction alerts) via Gmail SMTP, receives events via Kafka

### Infrastructure

- **PostgreSQL** – Multiple databases, single instance
- **Redis** – Caching & rate-limiting
- **Kafka + Zookeeper** – Event-driven communication
- **Docker & Docker Compose** – Local + production-like setup

## ⚙️ Running the Project

The project supports three execution modes:

### 1️⃣ Local Development (Eclipse / IntelliJ)

Run services individually using your IDE. All infrastructure services (PostgreSQL, Redis, Kafka, Zookeeper) must be installed and running locally. Useful for debugging & step-by-step development.

**Prerequisites:**
- Java 17
- Maven
- PostgreSQL installed locally
- Redis installed locally
- Kafka and Zookeeper installed and running locally

**Infrastructure Setup:**

**1. PostgreSQL Setup:**
- Install PostgreSQL on your machine
- Create the following databases manually:
  - `bankauthdb`
  - `bankaccountdb`
  - `banktransactiondb`
  - `bankpaymentdb`
  - `bankcarddb`
- Use the same username/password as configured in `public.env` (default: `postgres` / `1234`)

**2. Redis Setup:**
- Install Redis locally
- Tutorial: [Install Redis on macOS/Windows](https://www.liquidweb.com/blog/install-redis-macos-windows/)
- Start Redis server (default port: 6379)

**3. Kafka & Zookeeper Setup:**
- Install Kafka and Zookeeper locally
- Tutorial: [Apache Kafka Installation](https://codingnconcepts.com/post/apache-kafka-installation/)
- Start Zookeeper first, then Kafka
- Ensure Kafka is running on port 9092

**4. Config Server Setup:**
- Config files are stored in a separate repository: [Banking-System-Simulator-Config](https://github.com/kskcoder/Banking-System-Simulator-Config)
- Ensure the config repository is accessible (public or configured with proper access)
- Update `CONFIG_GIT_URI_EXTERNAL` in `public.env` if using a different config repository

**5. Email Setup (Optional):**
- To enable email notifications, create `private.env` file locally
- Follow Gmail SMTP setup tutorial: [Gmail SMTP using App-Specific Passwords](https://support.happyfox.com/kb/article/1510-gmail-smtp-using-app-specific-passwords/)
- Copy the 16-character app password and paste it as `SPRING_MAIL_PASSWORD` in `private.env`
- Add `SPRING_MAIL_USERNAME` with your Gmail address

**Steps to Run:**
1. Ensure all infrastructure services are running (PostgreSQL, Redis, Zookeeper, Kafka)
2. Run services in your IDE in the following order:
   - `bankconfigservice` (port 8888)
   - `bankserviceregistry` (port 8761)
   - `bankapigateway` (port 8765)
   - Other microservices (auth, account, transaction, payment, card, messaging)

### 2️⃣ Docker – Local Build (Single Compose)

Entire system runs using one Docker Compose file. Services are built locally from source. No local installations required except Docker. Databases are auto-created and seeded on first run.

**Prerequisites:**
- Docker & Docker Compose

**Command:**
```bash
docker compose -f docker-local-compose.yml up -d
```

**Access Points:**
- API Gateway: `http://localhost:8765`
- Eureka Dashboard: `http://localhost:8761`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`

### 3️⃣ Docker Hub Images (Production-like)

All microservices are pre-built and published to Docker Hub. Compose pulls images directly (no local build required). Fastest way to get started.

**Required Files:**

You only need **2-3 files** to run the system:

1. **`public.env`** - Public configuration (required)
2. **`docker-hub-compose.yml`** - Docker Compose file (required)
3. **`private.env`** - Private secrets for email functionality (optional, only if you need email notifications)

**Download Required Files:**

Run these commands to download the required files:

```bash
curl -O https://raw.githubusercontent.com/kskcoder/Banking-System-Simulator/release/public.env
curl -O https://raw.githubusercontent.com/kskcoder/Banking-System-Simulator/release/docker-hub-compose.yml
```

Or using `wget`:
```bash
wget https://raw.githubusercontent.com/kskcoder/Banking-System-Simulator/release/public.env
wget https://raw.githubusercontent.com/kskcoder/Banking-System-Simulator/release/docker-hub-compose.yml
```

**Alternative:** Right-click these links and select "Save As": [public.env](https://raw.githubusercontent.com/kskcoder/Banking-System-Simulator/release/public.env) | [docker-hub-compose.yml](https://raw.githubusercontent.com/kskcoder/Banking-System-Simulator/release/docker-hub-compose.yml)

**Note:** For email functionality, create `private.env` locally (see [Email Setup](#-otp--email-setup) section). This file is not included in the repository for security reasons.

**Command:**
```bash
docker compose -f docker-hub-compose.yml up -d
```

**Note:** The `bash` in the code block above is just syntax highlighting. Run the command directly: `docker compose -f docker-hub-compose.yml up -d`

## 🗄️ Database Strategy

- **Single PostgreSQL instance** with multiple databases (one per service)
- Same username/password across services
- Schema & seed data managed using Flyway
- No database logic in Docker Compose

This mirrors real production deployments, where DB servers are external to services.

### Database Names

- `bankauthdb` – Auth Service
- `bankaccountdb` – Account Service
- `banktransactiondb` – Transaction Service
- `bankpaymentdb` – Payment Service
- `bankcarddb` – Card Service

## 🔁 Database Migration & Seeding

Each microservice contains Flyway migrations:

- `V1__init.sql` – Table creation
- `V2__seed_default_users.sql` – Default records (users, test accounts, etc.)

Flyway runs automatically on service startup. Data is seeded only once.

## 🔐 Environment & Secrets Management

### Public Config (`public.env`)

Used for:
- Database credentials
- Service URLs
- Kafka / Redis config

This file can exist locally but should not contain sensitive secrets.

**Example:**
```env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=1234
EUREKA_URL=http://bankserviceregistry:8761/eureka
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
SPRING_DATA_REDIS_HOST=redis
```

### Private Config (`private.env`)

Used for:
- Email provider credentials
- App passwords
- API keys

**⚠️ This file is NOT included in the project repository**  
**⚠️ You must create this file locally if you need email functionality**  
**✔ Loaded via Docker Compose or GitHub Actions secrets when provided**

## ✉️ OTP & Email Setup

### Console-Only OTP (Default)

OTP is generated and logged in console. Useful for local development and testing.

### Email OTP (Optional)

Uses external email provider. Enabled only when email credentials are provided in `private.env`.

**Setup Instructions:**

1. **Gmail App Passwords:**
   - Follow the tutorial: [Gmail SMTP using App-Specific Passwords](https://support.happyfox.com/kb/article/1510-gmail-smtp-using-app-specific-passwords/)
   - Enable 2-Step Verification in your Google account
   - Generate App Password: https://myaccount.google.com/apppasswords
   - Copy the 16-character app password code

2. **SMTP Configuration:**
   Add to `private.env`:
   ```env
   SPRING_MAIL_USERNAME=your-email@gmail.com
   SPRING_MAIL_PASSWORD=your-16-character-app-password
   ```
   **Note:** Paste the 16-character app password code (without spaces) as the value for `SPRING_MAIL_PASSWORD`

3. **Create Users for Email Notifications:**
   After configuring `private.env` with email credentials, you must create new users through the signup API endpoint. The pre-seeded test users (`User1`, `User2`, `TejasAdmin`) use example email addresses and will not receive actual email notifications. To receive email notifications (OTP codes, transaction alerts, etc.), create users with your own valid email addresses via the authentication service signup endpoint.

## 🚀 CI/CD

### GitHub Actions

- Builds images only for changed services
- Pushes images to Docker Hub
- Zero manual image builds
- Automatic version bumping

**Workflow Triggers:**
- Push to `release` branch
- Manual workflow dispatch (with optional force build all)

**Features:**
- Change detection per service
- Multi-platform builds (linux/amd64, linux/arm64)
- Version management via VERSION files
- Docker Hub authentication with retry logic

## 📦 Tech Stack

**Core Technologies:**
- **Java 17**
- **Spring Boot 3.3.4**
- **Spring Cloud** (Eureka, Config, Gateway)
- **Spring Security** (JWT Authentication)
- **Spring Data JPA** (PostgreSQL)
- **PostgreSQL** (Database)
- **Kafka** (Event-driven messaging)
- **Redis** (Caching & rate-limiting)
- **Flyway** (Database migrations)
- **Swagger/OpenAPI 3** (API documentation)
- **Docker & Docker Compose** (Containerization)
- **GitHub Actions** (CI/CD)

See [Service Technologies](#-service-technologies) section for detailed tech stack per service.

## 🎯 Design Goals

- Production-realistic setup
- Clear separation of concerns
- No hard-coded secrets
- Event-driven where applicable
- Easy local + cloud portability

## 📋 Service Ports

| Service | Port |
|---------|------|
| Config Server | 8888 |
| Service Registry (Eureka) | 8761 |
| API Gateway | 8765 |
| Auth Service | 8080 |
| Account Service | 8085 |
| Transaction Service | 8090 |
| Card Service | 8095 |
| Payment Service | 8100 |
| Messaging Service | 8105 |

## 🛠️ Service Technologies

Each microservice is built with the following technologies:

| Service | Technologies |
|---------|-------------|
| **Config Server** | Spring Cloud Config, Git-based configuration ([Config Repository](https://github.com/kskcoder/Banking-System-Simulator-Config)) |
| **Service Registry** | Spring Cloud Netflix Eureka Server |
| **API Gateway** | Spring Cloud Gateway, Load Balancing |
| **Auth Service** | Spring Security, JWT (jjwt), PostgreSQL, Redis (caching), Kafka (OTP messaging) |
| **Account Service** | Spring Data JPA, PostgreSQL, Redis (caching), Kafka (events), Feign Client |
| **Transaction Service** | Spring Data JPA, PostgreSQL, Kafka (events), Feign Client |
| **Payment Service** | Spring Data JPA, PostgreSQL, Kafka (events), Feign Client |
| **Card Service** | Spring Data JPA, PostgreSQL, Redis (caching), Kafka (events), Feign Client |
| **Messaging Service** | Spring Kafka (event consumption), Spring Mail (Gmail SMTP), HTML email templates |

**Common Technologies Across Services:**
- Spring Boot 3.3.4
- Java 17
- Spring Cloud (Eureka, Config, Gateway)
- PostgreSQL (via Spring Data JPA)
- Swagger/OpenAPI 3
- Docker

## 📸 Demo Pictures

> **📌 Important:** All demo screenshots shown below are captured from **Swagger UI**. This is a backend-only microservices system - there is no frontend web application. The APIs can also be tested using Postman or any REST client, but all demo screenshots are from Swagger UI. To access Swagger UI, navigate to `http://localhost:8765/webjars/swagger-ui/index.html` (API Gateway).

### 🧪 Test Data

Use the following test credentials and data to explore the system:

**Test Users:**
- **Username:** `User1` | **Password:** `User1` | **Role:** USER | **Email:** user1@example.com
- **Username:** `User2` | **Password:** `User2` | **Role:** USER | **Email:** user2@example.com
- **Username:** `TejasAdmin` | **Password:** `TejasAdmin` | **Role:** ADMIN | **Email:** tejasadmin@example.com

**Test Accounts:**
- **User1** → Account: `AC11768471543584479` | Type: SAVINGS | Balance: ₹15,000
- **User2** → Account: `AC21768471543584044` | Type: SAVINGS | Balance: ₹22,000
- **TejasAdmin** → Account: `AC31768471543584173` | Type: CURRENT | Balance: ₹25,810

**Test Cards:**
- **User1** → Card: `1111167093812745` | CVV: `592` | Expiry: `01/2031` | Limit: ₹50,000 | Status: INACTIVE
- **User2** → Card: `1111420846226035` | CVV: `336` | Expiry: `01/2031` | Limit: ₹50,000 | Status: INACTIVE
- **TejasAdmin** → Card: `1111166139647775` | CVV: `620` | Expiry: `01/2031` | Limit: ₹200,000 | Status: INACTIVE

**Note:** 
- Username and password are the same for all test users. Cards will be activated after first use.
- **Important:** When using card numbers in API requests (signature calculation, payment body, etc.), input them **without spaces** (e.g., use `1111167093812745` instead of `1111 1670 9381 2745`).

---

### Authentication & JWT Generation

**Auth Service - JWT Token Generation**

![Auth Service JWT Generation](https://github.com/user-attachments/assets/b688d053-84e6-48b3-b52e-3afc03979804)

*JWT token generated after successful user authentication*

---

### OTP Generation & Verification

**OTP Generation**

![OTP Generation](otp-generation.png)

*OTP code generated and sent via console/email*

**OTP Verification**

![OTP Verification](otp-verification.png)

*Successful OTP verification process*

---

### Transaction Operations

**Debit Transaction**

![Debit Transaction](debit-transaction.png)

*Debit transaction processing and balance update*

**Credit Transaction**

![Credit Transaction](credit-transaction.png)

*Credit transaction processing and balance update*

**Transaction History**

![Transaction History](transaction-history.png)

*View of transaction history and account statements*

---

### Payment Processing

**Payment Initiation**

![Payment Processing](payment-processing.png)

*Payment request processing flow*

**Payment Confirmation**

![Payment Confirmation](payment-confirmation.png)

*Payment success confirmation and notification*

---

### Card Management

**Card Creation**

![Card Creation](card-creation.png)

*New card creation and limit assignment*

**Card Limits**

![Card Limits](card-limits.png)

*Card limit management and updates*

---

### Messaging & Notifications

**Transaction Notifications**

![Transaction Notifications](transaction-notifications.png)

*Kafka-based async transaction notifications*

**OTP Email**

![OTP Email](otp-email.png)

*OTP code delivered via email*

---

### Service Discovery & Monitoring

**Eureka Dashboard**

![Eureka Dashboard](eureka-dashboard.png)

*Service registry showing all registered microservices*

**API Gateway Routes**

![API Gateway Routes](api-gateway-routes.png)

*API Gateway routing configuration and endpoints*

---

### Account Management

**Account Creation**

![Account Creation](account-creation.png)

*New bank account creation process*

**Account Balance**

![Account Balance](account-balance.png)

*Account balance inquiry and details*

## 🔧 API Testing & Documentation

### Swagger UI Access

Access Swagger UI through the API Gateway for interactive API testing:

**Swagger UI URL:** `http://localhost:8765/webjars/swagger-ui/index.html`

All microservices are accessible through this single Swagger UI interface, which aggregates APIs from all services.

### API Gateway Endpoint

All services can be accessed through the API Gateway:
- **Base URL:** `http://localhost:8765`
- **Swagger UI:** `http://localhost:8765/webjars/swagger-ui/index.html`
- Routes are configured to forward requests to appropriate microservices

### Alternative: Postman or REST Clients

While all demo screenshots are from Swagger UI, you can also test the APIs using Postman or any REST client:

1. Import the API endpoints from Swagger JSON (available at `/v3/api-docs` for each service)
2. Use the API Gateway URL: `http://localhost:8765`
3. Include JWT tokens in the Authorization header for protected endpoints

**Note:** The demo pictures above show actual API responses from Swagger UI. This backend system does not include a frontend web application.

### 💳 Payment Service - Signature Calculation

The Payment Service uses HMAC-SHA256 signatures for request authentication. For **testing/demo purposes**, you can use the helper endpoint to calculate signatures.

#### Using the Demo Signature Endpoint

**Endpoint:** `POST /payments/demo/calculate-signature`

**Headers:**
- `X-Vendor-Secret`: `gatewayTPayVendorKey` (for demo)
- `X-Timestamp`: Any timestamp value (e.g., `1234567890`)

**Body:** Your payment request JSON (must be **completely copy-pasted as-is**)

**Response:** Returns the calculated signature (Base64 HMAC-SHA256)

#### Step-by-Step Workflow

**For Payment Initiation:**

1. Prepare your payment request body (e.g., `{"fromAccountNumber":"AC11768471543584479","toAccountNumber":"AC21768471543584044","amount":100.0}`)
2. **Copy the entire body exactly as-is** (including all spaces, formatting, etc.)
3. Call `POST /payments/demo/calculate-signature` with:
   - Same body (copy-pasted exactly)
   - Header: `X-Vendor-Secret: gatewayTPayVendorKey`
   - Header: `X-Timestamp: 1234567890` (or any timestamp)
4. Copy the returned signature value
5. Use the signature in the `X-Signature` header when calling `POST /payments/initiate`:
   - Same body (copy-pasted exactly)
   - Header: `X-Signature: <signature-from-step-4>`
   - Header: `X-Timestamp: <same-timestamp-from-step-3>`
   - Header: `X-Vendor-Id: TPay`
   - Header: `X-Vendor-Secret: gatewayTPayVendorKey`
   - Header: `X-External: 1`

**For OTP Submission:**

The same process applies for `POST /payments/submitotp`:

1. Prepare your OTP request body (e.g., `{"paymentId":1,"otp":"123456"}`)
   - **Important:** Send OTP as a **string** (e.g., `"otp":"123456"`), not as an integer
2. **Copy the entire body exactly as-is**
3. Call `POST /payments/demo/calculate-signature` with the same body and headers
4. Use the returned signature in `POST /payments/submitotp` with the same body and headers

**Important Notes:**

- ⚠️ The request body sent to `/payments/demo/calculate-signature` must be **completely copy-pasted as-is** to the actual payment endpoint. Any changes (spaces, formatting, etc.) will result in signature mismatch.
- ⚠️ **Card numbers must be input without spaces** in request bodies and signature calculations (e.g., use `1111167093812745` instead of `1111 1670 9381 2745`).
- ⚠️ OTP values must be sent as **strings** (e.g., `"otp":"123456"`), not integers (e.g., `"otp":123456`).
- ⚠️ This endpoint is for **demo/testing only**. In production, external vendors calculate signatures themselves using their secret keys and the same algorithm (normalized JSON body + timestamp, then HMAC-SHA256 + Base64).

## 🧠 Notes

This project intentionally avoids:
- In-container databases with preloaded data
- Hardcoded credentials
- Service-to-service tight coupling

It focuses on how systems are actually built and deployed in real teams.

## 📝 Project Structure

```
Banking-System-Simulator/
├── bankconfigservice/      # Config Server
├── bankserviceregistry/    # Eureka Service Registry
├── bankapigateway/         # API Gateway
├── bankauthservice/        # Authentication Service
├── bankaccountservice/     # Account Management Service
├── banktransactionservice/ # Transaction Service
├── bankpaymentservice/     # Payment Service
├── bankcardservice/        # Card Service
├── bankmessagingservice/   # Messaging Service
├── bankingcommon/          # Shared common library
├── docker-local-compose.yml    # Local Docker Compose
├── docker-hub-compose.yml      # Production Docker Compose
├── public.env                  # Public configuration (included in repo)
└── private.env                 # Private secrets (NOT in repo - create locally if needed)
```

## 🔍 Monitoring & Health Checks

All services include health checks and are monitored via:
- Eureka Dashboard: `http://localhost:8761`
- Service health endpoints: `http://localhost:<port>/actuator/health`

## 📚 Additional Resources

- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Flyway Documentation](https://flywaydb.org/documentation/)
