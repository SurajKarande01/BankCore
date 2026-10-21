# BankCore - Banking Transaction & Account Ledger System

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg?style=flat-square&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot 3.3.4](https://img.shields.io/badge/Spring_Boot-3.3.4-brightgreen.svg?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-Hibernate_6-blue.svg?style=flat-square)](https://spring.io/projects/spring-data-jpa)
[![MySQL 8](https://img.shields.io/badge/MySQL-8.0-blue.svg?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Swagger OpenAPI 3](https://img.shields.io/badge/Swagger-OpenAPI_3.0-green.svg?style=flat-square&logo=swagger)](http://localhost:8080/swagger-ui/index.html)

BankCore is a backend banking engine built with **Java 17** and **Spring Boot 3**. I built this system to handle core banking operations with strict transaction integrity and financial correctness: opening bank accounts, executing ACID-compliant fund transfers, serializing concurrent transactions using pessimistic locking, recording immutable audit trails, and generating reconciled monthly account statements.

---

## 💡 Key Highlights & Engineering Features

- **Atomic Fund Transfers**: Wrapped transfer workflows inside Spring's `@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)`. If an error happens midway (such as an insufficient balance or inactive account), Spring rolls back the entire transaction so partial debits never touch the database.
- **Pessimistic Locking Concurrency**: Added `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`) to balance updates. When multiple threads attempt to transfer or withdraw from the same account at the same time, database rows lock and requests queue up safely, preventing lost updates.
- **Deadlock Prevention via Lock Ordering**: To prevent database deadlocks when two accounts transfer to each other simultaneously, locks are always acquired in ascending order of Account ID (`min(id)` first, then `max(id)`).
- **Immutable Audit Trail**: Every balance update (deposit, withdrawal, transfer) writes an entry to the `audit_logs` table tracking the actor, operation, timestamp, IP address, and status.
- **Monthly Statement Engine**: Generates month-by-month statements calculating the opening balance from historical ledger entries, summing debits and credits, itemizing each transaction with its running balance, and reconciling the closing balance.
- **Financial Precision**: All monetary values are handled using `BigDecimal` to avoid binary floating-point rounding issues (`float`/`double`).

---

## 🏗️ Architecture & Data Flow

```text
[ Client / Postman / Swagger UI ]
               │
               ▼
   [ Spring MVC REST Controllers ]
   (Jakarta Bean Validation @Valid)
               │
               ▼
[ TransactionServiceImpl (@Transactional) ]
   ├── 1. Sort Account IDs (Lock Ordering: min(id) -> max(id))
   ├── 2. SELECT * FROM accounts WHERE id = ? FOR UPDATE
   ├── 3. Validate Account Status & Sufficient Balance
   ├── 4. Update Balances (sender.debit, receiver.credit)
   ├── 5. Insert Transaction Ledger Record (Balance Snapshots)
   └── 6. Insert Immutable Audit Log (Actor & Details)
               │
               ▼
  [ Database: H2 In-Memory (Dev/Test) | MySQL 8 (Prod) ]
```

---

## 🛠️ Tech Stack

- **Java Version**: Java 17 LTS
- **Framework**: Spring Boot 3.3.4
- **Persistence & ORM**: Spring Data JPA, Hibernate 6.5
- **Databases**:
  - H2 Database (in-memory, default profile for rapid local development and automated testing)
  - MySQL 8.0 (InnoDB storage engine for row-level locking)
- **Validation**: Jakarta Bean Validation (`@NotNull`, `@DecimalMin`, `@Email`, `@NotBlank`, `@Size`)
- **API Documentation**: Springdoc OpenAPI 2.6.0 (Swagger UI)
- **Testing**: JUnit 5, Mockito, AssertJ
- **Utilities**: Project Lombok, Jackson (JavaTimeModule)

---

## 🚀 Running the Project Locally

### Prerequisites
- **Java 17+** installed (`java -version`)
- **Maven 3.9+** installed (`mvn -version`)

### 1. Default Profile (In-Memory H2 - Zero Setup)
Clone the repository and run:
```bash
mvn spring-boot:run
```
The server will start at `http://localhost:8080`.
- **Interactive Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **H2 Web Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - JDBC URL: `jdbc:h2:mem:bankcoredb`
  - Username: `sa`
  - Password: *(leave blank)*

### 2. MySQL Profile (Production Database)
Ensure MySQL is running on `localhost:3306` with database `bankcoredb`, then launch:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

---

## 📡 REST API Endpoints

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
  "createdBy": "suraj_admin"
}
```

#### Get Account by Number
- **GET** `/api/v1/accounts/{accountNumber}`

#### List Accounts (Paginated)
- **GET** `/api/v1/accounts?page=0&size=10&sortBy=createdAt&direction=desc`

---

### 2. Banking Transactions (`/api/v1/transactions`)

#### Fund Transfer (Pessimistic Locking & Audit Logged)
- **POST** `/api/v1/transactions/transfer`
```json
{
  "sourceAccountNumber": "AC1000000001",
  "destinationAccountNumber": "AC1000000002",
  "amount": 1500.00,
  "description": "Monthly rent payment",
  "initiatedBy": "suraj_web"
}
```
**Sample Response (200 OK):**
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
    "initiatedBy": "suraj_web",
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
  "description": "ATM cash withdrawal",
  "initiatedBy": "atm_pune_01"
}
```

#### Get Transaction by Reference
- **GET** `/api/v1/transactions/{reference}`

#### Get Recent Account Transactions
- **GET** `/api/v1/transactions/account/{accountNumber}`

---

### 3. Monthly Statements (`/api/v1/statements`)

#### Generate Monthly Statement
- **GET** `/api/v1/statements/{accountNumber}?year=2026&month=10`

**Sample Response (200 OK):**
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

- **GET** `/api/v1/audit-logs` — All system audit entries (newest first)
- **GET** `/api/v1/audit-logs/actor/{actor}` — Audit records filtered by username/agent
- **GET** `/api/v1/audit-logs/entity/{entityName}/{entityId}` — Audit records filtered by entity

---

## 🧪 Automated Testing & Concurrency Validation

Run the full automated test suite with Maven:
```bash
mvn clean test
```

### Coverage Highlights:
- **`BankCoreConcurrencyTest`**: Multi-threaded stress test using `ExecutorService` and `CountDownLatch` firing 10 concurrent transfer requests across worker threads simultaneously. Validates that pessimistic locking eliminates lost updates and that final balances match exact arithmetic predictions.
- **`TransactionServiceTest`**: Validates atomic debit/credit operations, overdraft prevention (`InsufficientBalanceException`), rejection of inactive/suspended accounts, and self-transfer guards.
- **`AccountServiceTest`**: Validates account registration, duplicate email rejection, and opening deposit ledger entries.
- **`StatementServiceTest`**: Validates mathematical reconciliation: `Opening Balance + Credits - Debits == Closing Balance`.

---

## 💬 Technical Q&A / Design Decisions

1. **Why use Pessimistic Locking over Optimistic Locking?**
   > In banking systems with high transaction frequency on the same account, optimistic locking (`@Version`) would trigger repeated `OptimisticLockingFailureException` rollbacks and require complex retry mechanisms. Pessimistic write locking (`SELECT ... FOR UPDATE`) serializes access at the database row level, ensuring safe, queued balance updates without retry storms.

2. **How is deadlock prevented during concurrent transfers?**
   > If Account A sends funds to Account B while Account B sends funds to Account A at the same time, opposing lock order would cause a database deadlock. I solved this by sorting the account IDs and always locking the smaller ID first (`min(id)` then `max(id)`). Both threads acquire locks in the exact same order.

3. **How does `@Transactional` prevent partial debits?**
   > Spring's `@Transactional(rollbackFor = Exception.class)` ensures that debiting the sender, crediting the receiver, recording the transaction record, and saving the audit log are executed as a single atomic unit of work. Any unhandled exception triggers an automatic database rollback.

4. **Why `BigDecimal` instead of `double` or `float`?**
   > Binary floating-point types (`float` and `double`) cannot accurately represent fractions like `0.1` or `0.2` due to binary IEEE 754 precision limits (e.g. `0.1 + 0.2 = 0.30000000000000004`). In financial systems, `BigDecimal` provides exact arbitrary-precision arithmetic.

---

## 👤 Author
**Suraj Daryappa Karande**  
- **Email**: [surajdkarande6396@gmail.com](mailto:surajdkarande6396@gmail.com)  
- **LinkedIn**: [linkedin.com/in/surajk0001](https://www.linkedin.com/in/surajk0001)  
- **GitHub**: [github.com/SurajKarande01](https://github.com/SurajKarande01)  
- **Portfolio**: [suraj-karande-portfolio.vercel.app](https://suraj-karande-portfolio.vercel.app/)
