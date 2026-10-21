# BankCore - Banking Transaction & Account Ledger System

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg?style=flat-square&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot 3.3.4](https://img.shields.io/badge/Spring_Boot-3.3.4-brightgreen.svg?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-Hibernate_6-blue.svg?style=flat-square)](https://spring.io/projects/spring-data-jpa)
[![MySQL 8](https://img.shields.io/badge/MySQL-8.0-blue.svg?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Swagger OpenAPI 3](https://img.shields.io/badge/Swagger-OpenAPI_3.0-green.svg?style=flat-square&logo=swagger)](http://localhost:8080/swagger-ui/index.html)

BankCore is a backend banking engine I built using **Java 17** and **Spring Boot 3**. The main motivation behind this project was to understand how real-world financial platforms handle core ledger operations reliably—specifically dealing with race conditions, database deadlocks, partial transaction failures, and audit compliance.

Instead of treating account balances as simple database rows that get updated with basic math, this system enforces strict ACID transactional boundaries, uses row-level pessimistic locking for balance updates, maintains an append-only ledger for every credit/debit, and calculates mathematically reconciled monthly statements.

---

## 💡 What Makes This Project Interesting

### 1. Concurrency Handling with Pessimistic Locking
In high-volume banking systems, two transfers or withdrawals hitting the same account at the same millisecond can easily cause lost updates if handled naively. 
- Rather than relying on optimistic locking (`@Version`), which causes frequent exceptions and forces clients to retry transactions, I used `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
- Under the hood, Hibernate issues `SELECT ... FOR UPDATE` on MySQL/H2. This holds a database row lock until the transaction commits, naturally queuing concurrent requests at the database layer.

### 2. Deadlock Prevention via Deterministic Lock Ordering
When Account A transfers money to Account B while Account B transfers to Account A simultaneously, the two threads can lock one account each and wait forever for the other, causing a classic database deadlock.
- I prevented this by always sorting the account primary keys before acquiring locks: `min(id)` is locked first, followed by `max(id)`.
- Because all worker threads acquire locks in the exact same order regardless of transfer direction, cyclic wait conditions are mathematically impossible.

### 3. Zero Partial Debits via Spring Transactions
Money transfers touch two accounts, generate a transaction record, and write an audit entry.
- The entire operation is annotated with `@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)`.
- If an exception occurs at any point (e.g., sender has insufficient funds, recipient account is suspended, or a database write fails), Spring rolls back every change cleanly. Money cannot vanish or duplicate.

### 4. Financial Precision with `BigDecimal`
To prevent binary floating-point inaccuracies typical with `double` or `float` (like `0.1 + 0.2 = 0.30000000000000004`), every monetary value in entities, DTOs, and calculation logic uses `BigDecimal` rounded to 2 decimal places with `RoundingMode.HALF_UP`.

### 5. Defensive Programming & Crash Resilience
- **Bounded Account Number Generation**: Random account number collisions are handled with a loop bounded to 15 attempts before safely falling back to a timestamp-seeded generator. This prevents infinite loops under heavy data volumes.
- **Defensive Pagination**: The account listing endpoint automatically clamps page numbers (`page >= 0`) and page sizes (between 1 and 100), protecting the database from huge memory allocations.
- **Null Safety in DTOs**: Entity-to-DTO mappers check for null references defensively to avoid `NullPointerException` at runtime.
- **Thread Pool Cleanup**: In multi-threaded concurrency tests, thread pool shutdown is enclosed in `try-finally` blocks to guarantee no hanging executor threads.

---

## 🏗️ System Architecture & Transfer Lifecycle

```text
[ Client / Web App / Postman / Swagger UI ]
                    │
                    ▼
        [ Account & Transaction REST APIs ]
          (Jakarta Validation: @Valid)
                    │
                    ▼
        [ TransactionServiceImpl ]
       (@Transactional READ_COMMITTED)
                    │
   ┌────────────────┴────────────────┐
   ▼                                 ▼
1. Lock Ordering            2. Validation & State
   min(id) -> max(id)          Check Account Status
   SELECT FOR UPDATE           Verify Balance >= Amount
   ┌─────────────────────────────────┘
   ▼
3. Ledger & Balances
   sender.debit(amount)  ->  receiver.credit(amount)
   Insert into 'transactions' table (captures balance snapshots)
   Insert into 'audit_logs' table (actor, timestamp, IP, status)
                    │
                    ▼
     [ Database Storage: H2 (Dev) | MySQL 8 (Prod) ]
```

---

## 🛠️ Tech Stack & Dependencies

- **Java Version**: Java 17 LTS (configured with modern records, switch expressions, and `--release 17`)
- **Backend Framework**: Spring Boot 3.3.4
- **Persistence / ORM**: Spring Data JPA & Hibernate 6.5
- **Databases**:
  - **H2 Database**: Zero-config in-memory database used by default for local development and unit tests.
  - **MySQL 8.0**: InnoDB engine profile with row-level locking for production setups.
- **API Documentation**: Springdoc OpenAPI 2.6.0 (Swagger UI)
- **Testing**: JUnit 5, Mockito, AssertJ, and multi-threaded stress tests using `ExecutorService` and `CountDownLatch`
- **Build Tool**: Apache Maven 3.9+

---

## 📂 Project Structure

```text
src/main/java/com/bankcore/
├── BankCoreApplication.java         # Spring Boot entry point
├── config/                          # AppConfig (ModelMapper) & OpenApiConfig (Swagger)
├── controller/                      # REST endpoints (Accounts, Transactions, Statements, Audits)
├── dto/
│   ├── request/                     # Validation payloads (CreateAccount, Transfer, Deposit, Withdraw)
│   └── response/                    # Standardized API response wrappers and DTOs
├── entity/                          # JPA Entities (Account, TransactionRecord, AuditLog)
│   └── enums/                       # AccountType, AccountStatus, TransactionType, TransactionStatus
├── exception/                       # Custom banking exceptions and GlobalExceptionHandler
├── repository/                      # Spring Data JPA repositories with custom JPQL & pessimistic locks
└── service/                         # Service interfaces and implementations
    └── impl/                        # AccountServiceImpl, TransactionServiceImpl, StatementServiceImpl, AuditServiceImpl
```

---

## 🚀 Running the Project Locally

### Prerequisites
- **Java 17+** installed (`java -version`)
- **Maven 3.9+** installed (`mvn -version`)

### 1. Default Profile (Zero Setup - In-Memory H2)
Clone the repository and launch the application directly:
```bash
mvn spring-boot:run
```
The server starts up at `http://localhost:8080`.
- **Interactive Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **H2 Web Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - JDBC URL: `jdbc:h2:mem:bankcoredb`
  - Username: `sa`
  - Password: *(leave blank)*

### 2. MySQL Profile (Production Database)
If you want to run against a real MySQL 8 instance:
1. Create a MySQL database:
   ```sql
   CREATE DATABASE bankcoredb;
   ```
2. Verify credentials in `src/main/resources/application-mysql.properties`.
3. Launch with the `mysql` profile:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=mysql
   ```

---

## 📡 REST API Reference

### 1. Account Management (`/api/v1/accounts`)

#### Create a New Account
- **POST** `/api/v1/accounts`
```json
{
  "accountHolderName": "Suraj Karande",
  "email": "suraj@bankcore.com",
  "phoneNumber": "+919876543210",
  "accountType": "SAVINGS",
  "initialDeposit": 5000.00,
  "createdBy": "admin_user"
}
```

#### Fetch Account by Number
- **GET** `/api/v1/accounts/{accountNumber}`

#### List Accounts (Paginated & Clamped)
- **GET** `/api/v1/accounts?page=0&size=10&sortBy=createdAt&direction=desc`

---

### 2. Transactions & Fund Transfers (`/api/v1/transactions`)

#### Transfer Funds (ACID & Pessimistic Lock Protected)
- **POST** `/api/v1/transactions/transfer`
```json
{
  "sourceAccountNumber": "AC1000000001",
  "destinationAccountNumber": "AC1000000002",
  "amount": 1500.00,
  "description": "Monthly rent payment",
  "initiatedBy": "web_portal"
}
```
**Response (200 OK):**
```json
{
  "success": true,
  "message": "Fund transfer completed successfully",
  "data": {
    "transactionReference": "TXN-7F8E9D0C1B2A3F4E",
    "sourceAccountNumber": "AC1000000001",
    "destinationAccountNumber": "AC1000000002",
    "amount": 1500.00,
    "transactionType": "TRANSFER",
    "status": "SUCCESS",
    "description": "Monthly rent payment",
    "initiatedBy": "web_portal",
    "sourceBalanceAfter": 3500.00,
    "destinationBalanceAfter": 6500.00,
    "timestamp": "2026-10-21T20:30:00"
  }
}
```

#### Cash Deposit
- **POST** `/api/v1/transactions/deposit`
```json
{
  "accountNumber": "AC1000000001",
  "amount": 2000.00,
  "description": "Branch cash deposit",
  "initiatedBy": "teller_01"
}
```

#### Cash Withdrawal
- **POST** `/api/v1/transactions/withdraw`
```json
{
  "accountNumber": "AC1000000001",
  "amount": 500.00,
  "description": "ATM withdrawal",
  "initiatedBy": "atm_pune_01"
}
```

#### Get Transaction History for Account
- **GET** `/api/v1/transactions/account/{accountNumber}`

---

### 3. Monthly Statements (`/api/v1/statements`)

#### Generate Reconciled Monthly Statement
- **GET** `/api/v1/statements/{accountNumber}?year=2026&month=10`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Monthly statement generated",
  "data": {
    "accountNumber": "AC1000000001",
    "accountHolderName": "Suraj Karande",
    "year": 2026,
    "month": 10,
    "openingBalance": 5000.00,
    "totalCredits": 2000.00,
    "totalDebits": 1500.00,
    "closingBalance": 5500.00,
    "transactionCount": 2,
    "transactions": [
      {
        "transactionReference": "TXN-7F8E9D0C1B2A3F4E",
        "timestamp": "2026-10-14T10:15:00",
        "description": "Monthly rent payment",
        "type": "DEBIT",
        "amount": 1500.00,
        "runningBalance": 3500.00
      },
      {
        "transactionReference": "TXN-8A9B0C1D2E3F4A5B",
        "timestamp": "2026-10-14T12:00:00",
        "description": "Branch cash deposit",
        "type": "CREDIT",
        "amount": 2000.00,
        "runningBalance": 5500.00
      }
    ],
    "generatedAt": "2026-10-21T20:35:00"
  }
}
```

---

### 4. Audit Trail (`/api/v1/audit-logs`)

- **GET** `/api/v1/audit-logs` — Retrieve all system audit records
- **GET** `/api/v1/audit-logs/actor/{actor}` — Filter logs by specific user/system agent
- **GET** `/api/v1/audit-logs/entity/{entityName}/{entityId}` — Filter logs by entity type and ID

---

## 🧪 Testing Suite & Concurrency Validation

Run the test suite using Maven:
```bash
mvn clean test
```

### Key Test Cases:
1. **`BankCoreConcurrencyTest`**: 
   - Uses `ExecutorService` and `CountDownLatch` to fire 10 concurrent transfer requests across multiple worker threads simultaneously.
   - Proves that pessimistic row-level locking avoids race conditions and that the accounts' final balances match the exact mathematical sum without any balance drift.
2. **`TransactionServiceTest`**:
   - Tests successful transfers and balance adjustments.
   - Verifies transactional rollback: confirms that when a sender has insufficient balance, an `InsufficientBalanceException` is thrown and no partial debit is saved.
   - Verifies rejection of inactive or suspended accounts and self-transfers.
3. **`AccountServiceTest`**:
   - Tests account opening, initial deposit ledgering, and rejection of duplicate emails.
4. **`StatementServiceTest`**:
   - Confirms the monthly statement reconciliation equation: `Opening Balance + Total Credits - Total Debits == Closing Balance`.

---

## 💬 Interview Q&A / Design Notes

**Q: Why choose Pessimistic Locking over Optimistic Locking?**  
> In account ledger systems, balance rows can receive multiple rapid updates in a small window. With optimistic locking (`@Version`), any concurrent write triggers an `OptimisticLockingFailureException`, which forces the client or application layer to handle retries. Pessimistic write locking (`SELECT ... FOR UPDATE`) queues requests safely at the database row level, guaranteeing serialized updates without retry storms.

**Q: How do you prevent deadlocks during concurrent transfers?**  
> If User A sends money to User B while User B sends money to User A at the same time, conflicting lock order causes a database deadlock. I resolved this by sorting account IDs and always acquiring locks in ascending order (`min(id)` first, then `max(id)`). Both threads request locks in the exact same sequence.

**Q: How does `@Transactional` prevent partial debits?**  
> With `@Transactional(rollbackFor = Exception.class)`, debiting the sender, crediting the receiver, creating the transaction ledger entry, and writing the audit log all share one atomic transaction. If an error occurs halfway through, the database rolls back to its exact initial state.

**Q: Why use `BigDecimal` instead of `double` or `float`?**  
> IEEE 754 floating-point numbers represent values in binary fractions, which leads to rounding inaccuracies (like `0.1 + 0.2 = 0.30000000000000004`). In financial software, every cent must be exact, so `BigDecimal` with explicit scale and rounding mode is mandatory.

---

## 👤 Author
**Suraj Daryappa Karande**  
- **Email**: [surajdkarande6396@gmail.com](mailto:surajdkarande6396@gmail.com)  
- **LinkedIn**: [linkedin.com/in/surajk0001](https://www.linkedin.com/in/surajk0001)  
- **GitHub**: [github.com/SurajKarande01](https://github.com/SurajKarande01)  
- **Portfolio**: [suraj-karande-portfolio.vercel.app](https://suraj-karande-portfolio.vercel.app/)
