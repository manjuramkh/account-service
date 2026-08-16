# Account Service

The Account Service is a core microservice in the Digital Banking platform responsible for managing customer bank accounts and account-level operations.

It provides APIs for account creation, retrieval, activation, freezing, closing, balance inquiry, account status, account limits, and account-related validations.

The service acts as the system of record for account master data and works with other banking services such as Customer, Transfer, Payment, Card, Ledger, Transaction, Notification, Fraud/Risk, and Audit services.

### Responsibilities

- Create account
- Close account
- Activate account
- Freeze account
- Retrieve account
- Account status
- Account type
- Account number
- Balance
- Account limits


## Development

### APIs

- POST /api/v1/accounts
- POST /api/v1/accounts/{accountId}/close
- PATCH /api/v1/accounts/{accountId}/status
- GET /api/v1/accounts/{accountId}
- GET /api/v1/accounts/by-number/{accountNumber}
- GET /api/v1/customers/{customerId}/accounts
- GET /api/v1/accounts/{accountId}/balance
- GET /api/v1/accounts/{accountId}/limits


### Database Architecture
Accounts:
| Column          | Description                                      |
|-----------------|--------------------------------------------------|
| `accountId`     | Unique internal identifier for the account      |
| `accountNumber` | Customer-facing bank account number              |
| `customerId`    | Identifier of the customer who owns the account |
| `accountType`   | Type of account, e.g. SAVINGS or CURRENT        |
| `currency`      | Account currency, e.g. INR or USD                |
| `availableBalance` | Balance currently available for transactions |
| `ledgerBalance` | Actual account balance maintained in ledger      |
| `status`        | Account status, e.g. ACTIVE, FROZEN, CLOSED     |
| `createdAt`     | Date and time when the account was created       |

account_status_history:

| Column | Description |
|---|---|
| `old_status` | Previous status of the account |
| `new_status` | New status of the account |
| `reason` | Reason for the account status change |
  

### Key Design Principles

The Account Service follows these principles:

- **Single Responsibility**
Own account-related business capabilities.
- **Database per Service**
Account database belongs to Account Service.
- **API-first communication**
Other services use APIs/events instead of direct database access.
- **Idempotent state-changing operations**
Prevent duplicate effects from retries.
- **Strong validation**
Validate account state and ownership before operations.
- **Auditability**
Preserve important account lifecycle changes.
- **Event-driven integration**
Publish account lifecycle events where appropriate.
- **Observability**
Support metrics, logs, tracing, and correlation IDs.
- **Security by default**
Enforce authentication and authorization at every sensitive operation.
- **Financial ownership boundaries**
Keep account master data separate from transfer workflow and ledger accounting.

## Technology Stack

A production-oriented implementation could use:

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL
- Hibernate
- Resilience4j
- Spring Boot Actuator
- ELK Stack
- SonarQube
- Docker
