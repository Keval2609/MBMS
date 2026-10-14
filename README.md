# 🏦 MBMS: Multi-Branch Bank Management System

![Java](https://img.shields.io/badge/Java-17-blue)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange)
![JDBC](https://img.shields.io/badge/JDBC-Native-lightgrey)
![HikariCP](https://img.shields.io/badge/HikariCP-5.1.0-brightgreen)
![Flyway](https://img.shields.io/badge/Flyway-10.8.1-red)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20Layered%20%2F%20Service--DAO-success)
![JUnit5](https://img.shields.io/badge/Tests-59%20Passing-success)

A high-performance, console-driven multi-branch banking engine built from the ground up to demonstrate **production-grade database concurrency**, **row-level locking**, **deadlock-free programmatic transactions**, and **pure JDBC architecture**.

Rather than relying on heavy ORM frameworks (like Spring Boot or Hibernate) that conceal database mechanics, MBMS explicitly manages connection pools, transaction boundaries, lock acquisition order, and ledger integrity natively.

---

## 🎬 Live Terminal Demo

*(Click to play the interactive terminal recording)*

[![asciicast](https://asciinema.org/a/649HQcvFy1q6JWfy.svg)](https://asciinema.org/a/649HQcvFy1q6JWfy)

---

## ⚡ Core Engineering Highlights

### 1. Deterministic Deadlock Prevention & Row-Level Locking
In high-throughput banking systems, simultaneous bidirectional transfers between the same pair of accounts create cyclic dependency deadlocks. MBMS solves this at the database engine level:
* **Row-Level Locking:** Executes `SELECT ... FOR UPDATE` on account rows to lock balances exclusively during transfers.
* **Deterministic Lock Ordering:** If Thread A transfers funds from Account 1 &rarr; 2 while Thread B transfers from Account 2 &rarr; 1, `AccountService` algorithmically sorts the account IDs and acquires database locks in strictly ascending numerical order (`min(id1, id2)` then `max(id1, id2)`).
* **Automated Concurrency Proof:** Validated via automated multi-threaded stress tests (`ConcurrencyTransferTest`) simulating simultaneous bidirectional transfers across parallel threads with zero deadlocks and zero balance drift.

### 2. Programmatic Transaction Boundaries (`TransactionManager`)
* **Connection-Aware DAOs:** DAOs implement connection propagation (`withConnection(conn)`), allowing multiple repositories (`AccountDAO`, `TransactionDAO`) to participate in the exact same physical database connection.
* **Functional Transaction Scope:** The `TransactionManager` encapsulates manual JDBC commit/rollback boundaries (`conn.setAutoCommit(false)`, `conn.commit()`, `conn.rollback()`), ensuring ACID transactions with automatic cleanup and resource leak prevention.

### 3. Immutable Double-Entry Ledger
* Financial transactions are strictly recorded as immutable double-entry ledger entries (`Transaction` table).
* Captures real-time balance snapshots (running balance before and after each debit/credit operation), preserving historical audit trails with cryptographic-grade accuracy.

### 4. Domain Factory Pattern (`AccountFactory`)
* Encapsulates account creation business rules across multiple product types (`SAVINGS`, `CURRENT` / `CHECKING`, `BUSINESS`).
* Enforces minimum opening balances, default interest rates, overdraft limits, and initial status transitions before persistence.

### 5. Domain-Specific Exception Hierarchy
* Comprehensive exception hierarchy rooted at `BankException`:
  * `AccountNotFoundException`
  * `CustomerNotFoundException`
  * `InsufficientBalanceException`
  * `InactiveAccountException`
  * `UnauthorizedException`
  * `DataAccessException`
* Cleanly translates technical database failures into explicit domain semantics.

### 6. Role-Based Access Control (RBAC) & Security
* Enforces role policies (`ADMIN` vs. `TELLER`) at the Service layer using an immutable `SessionContext` record.
* Employee credentials are authenticated using salted **BCrypt** hashes, preventing cleartext exposure.

---

## 🛠️ Architecture & Tech Stack

```
MBMS/
├── src/main/java/com/example/
│   ├── Main.java                     # Interactive CLI presentation & menu router
│   ├── DatabaseManager.java          # HikariCP connection pool & Flyway engine
│   ├── dao/                          # Pure JDBC Data Access Objects
│   │   ├── AccountDAO.java / AccountDAOImpl.java
│   │   ├── CustomerDAO.java / CustomerDAOImpl.java
│   │   ├── EmployeeDAO.java / EmployeeDAOImpl.java
│   │   ├── TransactionDAO.java / TransactionDAOImpl.java
│   │   └── TransactionManager.java   # Programmatic transaction boundary coordinator
│   ├── domain/                       # Core domain entities, enums & session records
│   │   ├── Account.java, AccountType.java, AccountStatus.java
│   │   ├── Customer.java
│   │   ├── Employee.java, Role.java
│   │   ├── Transaction.java, TransactionType.java, TransactionStatus.java
│   │   └── SessionContext.java
│   ├── exception/                    # Typed banking exception hierarchy
│   │   ├── BankException.java (Base)
│   │   ├── AccountNotFoundException.java, CustomerNotFoundException.java
│   │   ├── InsufficientBalanceException.java, InactiveAccountException.java
│   │   ├── UnauthorizedException.java, DataAccessException.java
│   ├── factory/                      # Domain creation factories
│   │   └── AccountFactory.java
│   └── service/                      # Business rule layer (RBAC & transaction-scoped)
│       ├── AccountService.java
│       ├── CustomerService.java
│       ├── AuthService.java
│       └── AdminService.java
├── src/main/resources/
│   ├── application.properties        # Database & HikariCP pool configurations
│   └── db/migration/                 # Flyway SQL migrations (V1, V2, V3)
└── src/test/java/com/example/        # 59 automated unit, integration, and stress tests
```

* **Language:** Java 17
* **Database:** MySQL 8.0 (Containerized via Docker)
* **Connection Pooling:** HikariCP 5.1.0 (with prepared statement cache tuning)
* **Schema Migration:** Flyway 10.8.1
* **Password Hashing:** jBCrypt 0.4
* **Testing:** JUnit 5 (59 unit, integration, and concurrency tests)

---

## 💻 Interactive Features & Menu Workflow

Upon authenticating, the system provides role-based CLI workflows:

1. **Transfer Funds:** Inter-account funds transfer with deadlock prevention, atomic balance updates, and transaction reference generation.
2. **Deposit Funds:** Credit funds to any active account with immediate ledger auditing.
3. **Withdraw Funds:** Debit funds with real-time balance checks and overdraft enforcement.
4. **View Transaction History:** Formatted chronological audit log showing transaction references, type, counterparty details, flow direction (`+` / `-`), and status.
5. **Account & Customer Management:**
   * Register new customers (name, email, phone, government ID).
   * Open new accounts via `AccountFactory` (Savings, Current/Checking) linked to customers and branches.
   * View all registered customers.
6. **System Configuration (Admin Only):**
   * Register and provision branch employees with secure BCrypt-hashed credentials.

---

## 🚀 Getting Started

### Prerequisites
* [Docker & Docker Compose](https://www.docker.com/)
* [Java 17+](https://adoptium.net/)
* [Apache Maven 3.8+](https://maven.apache.org/)

### Quick Start Guide

**1. Start the MySQL Container**
```bash
docker compose up -d
```
> MySQL will start on port `3307` with the preconfigured credentials and database volume.

**2. Run the Application**
Flyway will automatically detect the database state, apply all pending schema migrations, and start the console interface:
```bash
mvn clean compile exec:java "-Dexec.mainClass=com.example.Main"
```

**3. Default Credentials**
| Username | Password | Role | Permissions |
| :--- | :--- | :--- | :--- |
| `admin` | `admin` | `ADMIN` | All operations + Staff management & system config |

---

## 🧪 Testing & Verification

MBMS includes a comprehensive test suite covering DAO interactions, transaction rollbacks, service validation, and multi-threaded concurrency:

### Run All Tests via Maven
```bash
mvn test
```

### Run All Tests via Custom Runner
```bash
mvn test-compile exec:java "-Dexec.mainClass=com.example.TestRunner" "-Dexec.classpathScope=test"
```

### Test Coverage Highlights
* **Unit Tests:** Business logic, status validations, and `AccountFactory` variations.
* **Integration Tests:** H2/MySQL DAO operations and Flyway migration verification.
* **Concurrency Stress Test (`ConcurrencyTransferTest`):** Executes 100+ parallel, simultaneous bidirectional transfers between accounts to guarantee zero deadlocks and total balance preservation.

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).