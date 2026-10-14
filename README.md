# BankCore: Banking Transaction & Account Ledger System

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg?style=flat-square&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot 3.3.4](https://img.shields.io/badge/Spring_Boot-3.3.4-brightgreen.svg?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-Hibernate_6-blue.svg?style=flat-square)](https://spring.io/projects/spring-data-jpa)
[![MySQL 8](https://img.shields.io/badge/MySQL-8.0-blue.svg?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Swagger OpenAPI 3](https://img.shields.io/badge/Swagger-OpenAPI_3.0-green.svg?style=flat-square&logo=swagger)](http://localhost:8080/swagger-ui/index.html)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)

An enterprise-grade backend banking engine built with **Java 17** and **Spring Boot 3**. BankCore is engineered to handle core financial operations with strict transaction integrity: **account opening**, **ACID fund transfers**, **pessimistic locking concurrency protection**, **immutable audit trails**, and **reconciled monthly statement generation**.

---

## 📌 Features & Resume Alignment

- **ACID-Compliant Fund Transfers**: Wrapped every transfer in Spring's `@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)`. If any failure occurs midway (e.g. insufficient funds, inactive account, database failure), the entire transaction rolls back cleanly — **no partial debits ever reach the database**.
- **Pessimistic Locking Concurrency**: Utilizes `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`) on account balance records. Simultaneous transfer requests queue up safely at the row level rather than causing lost updates or race conditions.
- **Deadlock-Free Lock Ordering**: Eliminates cyclic lock waits during bidirectional concurrent transfers by acquiring row locks in deterministic ascending order of Account Primary Keys (`min(id)` first, then `max(id)`).
- **Full Operational Audit Trail**: Every debit, credit, opening deposit, and who triggered it (`initiatedBy` / `triggeredBy`) is recorded in an immutable `audit_logs` ledger for compliance and testing auditability.
- **Monthly Statement Generation**: Calculates period opening balances, aggregates debits and credits, itemizes transactions with running balances, and reconciles closing balances mathematically.
- **RESTful API & Swagger Documentation**: Full interactive OpenAPI 3 documentation available out of the box at `/swagger-ui/index.html`.

---

## 🏗️ Architecture & Data Flow

```
[ Client / Postman / Web ]
           │
           ▼
[ AccountController / TransactionController / StatementController ]
           │  (Jakarta Bean Validation @Valid)
           ▼
[ Service Layer: TransactionServiceImpl (@Transactional) ]
     ├── 1. Sort Account IDs (Deterministic Locking: min(id) -> max(id))
     ├── 2. SELECT * FROM accounts WHERE id = ? FOR UPDATE
     ├── 3. Validate Account Status & Sender Balance
     ├── 4. Update Balances (sender.debit, receiver.credit)
     ├── 5. Insert Transaction Ledger Row (Balance Snapshots)
     └── 6. Insert Immutable Audit Log (Actor & Details)
           │
           ▼
[ Database: MySQL 8 (InnoDB) / H2 In-Memory ]
```

---

## 🛠️ Tech Stack & Tooling

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Java 17 | Core language with records, pattern matching, and modern JVM optimizations |
| **Framework** | Spring Boot 3.3.4 | Modern RESTful microservice foundation |
| **Data Access** | Spring Data JPA / Hibernate 6 | ORM, JPQL queries, and row-level pessimistic locking |
| **Primary Database** | MySQL 8.0 (InnoDB) | Production relational storage engine supporting row locks |
| **In-Memory Database**| H2 Database | Zero-setup local development and automated testing profile |
| **Validation** | Jakarta Bean Validation | `@NotNull`, `@DecimalMin`, `@Email`, `@NotBlank` |
| **API Docs** | Springdoc OpenAPI 2.6.0 | Interactive Swagger UI v3 documentation |
| **Testing** | JUnit 5 & Mockito 5 | Unit tests, rollback verification, and multi-threaded stress tests |
| **Containerization**| Docker & Docker Compose | Multi-stage image build and multi-container orchestration |

---

## 🚀 Getting Started

### Prerequisites
- **Java 17+** installed (`java -version`)
- **Maven** (or use the provided Maven configurations)
- **Docker Desktop** (optional, for running with MySQL in containers)

### 1. Run Locally (Zero Config - Default In-Memory H2 Profile)
Clone the repository and run:
```bash
mvn spring-boot:run
```
The application will start on **`http://localhost:8080`**.
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **H2 Web Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:bankcoredb`, User: `sa`, Password: *(empty)*)

### 2. Run with MySQL Profile
Ensure MySQL is running on `localhost:3306`, then start with the `mysql` profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

### 3. Run with Docker Compose (Full Stack: App + MySQL Container)
```bash
docker-compose up --build
```

---

## 📡 REST API Reference

### 1. Accounts

#### Create Account
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
- **GET** `/api/v1/accounts?page=0&size=10`

---

### 2. Transactions

#### Fund Transfer (ACID & Pessimistic Lock Protected)
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
**Response (200 OK):**
```json
{
  "success": true,
  "message": "Fund transfer completed successfully",
  "data": {
    "transactionReference": "TXN-8A7B6C5D4E3F2A1B",
    "sourceAccountNumber": "AC1000000001",
    "destinationAccountNumber": "AC1000000002",
    "amount": 1500.00,
    "transactionType": "TRANSFER",
    "status": "SUCCESS",
    "description": "Monthly rent payment",
    "initiatedBy": "suraj_web",
    "sourceBalanceAfter": 3500.00,
    "destinationBalanceAfter": 6500.00,
    "timestamp": "2026-10-14T20:30:00"
  }
}
```

#### Cash Deposit
- **POST** `/api/v1/transactions/deposit`
```json
{
  "accountNumber": "AC1000000001",
  "amount": 2000.00,
  "description": "Branch deposit",
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

---

### 3. Monthly Statements

#### Generate Statement
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
        "transactionReference": "TXN-8A7B6C5D4E3F2A1B",
        "timestamp": "2026-10-14T10:15:00",
        "description": "Monthly rent payment",
        "type": "DEBIT",
        "amount": 1500.00,
        "runningBalance": 3500.00
      },
      {
        "transactionReference": "TXN-9F8E7D6C5B4A3A2B",
        "timestamp": "2026-10-14T12:00:00",
        "description": "Branch deposit",
        "type": "CREDIT",
        "amount": 2000.00,
        "runningBalance": 5500.00
      }
    ],
    "generatedAt": "2026-10-14T20:35:00"
  }
}
```

---

### 4. Audit Trail

#### Query Audit Logs
- **GET** `/api/v1/audit-logs` — Retrieve all system audit entries
- **GET** `/api/v1/audit-logs/actor/{actor}` — Filter logs by who triggered the action
- **GET** `/api/v1/audit-logs/entity/{entityName}/{entityId}` — Filter logs by entity

---

## 🧪 Testing Suite

Run all tests via Maven:
```bash
mvn test
```

### Test Coverage Highlights:
- **`AccountServiceTest`**: Tests account creation, unique account number generation, initial deposit ledgering, and rejection of duplicate emails.
- **`TransactionServiceTest`**:
  - Tests successful fund transfers and balance deductions.
  - Validates `@Transactional` rollback: proves that if sender has insufficient balance, an `InsufficientBalanceException` is thrown and **no partial debit is saved**.
  - Verifies rejection of inactive/suspended accounts and self-transfers.
- **`StatementServiceTest`**: Validates reconciliation equation: `Opening Balance + Total Credits - Total Debits == Closing Balance`.
- **`BankCoreConcurrencyTest`**: Multi-threaded stress test using `CountDownLatch` and `ExecutorService` simulating 10 concurrent transfer requests to verify zero balance corruption and prove pessimistic locking effectiveness.

---

## 💡 Interview Defense Cheatsheet (Quick Talking Points)

1. **Why Pessimistic Locking over Optimistic Locking?**
   > *"Optimistic locking (`@Version`) would fail with `OptimisticLockingFailureException` during high-frequency balance updates, forcing retry storms. In banking, pessimistic write locking (`SELECT ... FOR UPDATE`) queues requests safely at the database row level, guaranteeing serialized balance updates."*

2. **How did you prevent Deadlocks?**
   > *"When transferring between two accounts, I implemented deterministic lock ordering: always locking the account with the smaller numerical ID first, then the larger ID. This prevents cyclic lock dependency when Account A and Account B transfer to each other simultaneously."*

3. **How does `@Transactional` guarantee no partial debits?**
   > *"Spring's `@Transactional(rollbackFor = Exception.class)` ensures that debiting sender, crediting receiver, creating the ledger record, and logging the audit event belong to a single atomic database unit of work. Any runtime error immediately triggers a `ROLLBACK`."*

4. **Why `BigDecimal`?**
   > *"Floating-point types (`float`, `double`) suffer from binary representation rounding errors (e.g. `0.1 + 0.2 = 0.30000000000000004`). `BigDecimal` provides arbitrary-precision decimal arithmetic essential for financial accuracy."*

---

## 👤 Author
**Suraj Daryappa Karande**  
- **Email**: [surajdkarande6396@gmail.com](mailto:surajdkarande6396@gmail.com)  
- **LinkedIn**: [linkedin.com/in/surajk0001](https://www.linkedin.com/in/surajk0001)  
- **GitHub**: [github.com/SurajKarande01](https://github.com/SurajKarande01)  
- **Portfolio**: [suraj-karande-portfolio.vercel.app](https://suraj-karande-portfolio.vercel.app/)
