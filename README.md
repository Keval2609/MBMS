# 🏦 MBMS: Multi-Branch Bank Management System

![Java](https://img.shields.io/badge/Java-17-blue)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange)
![JDBC](https://img.shields.io/badge/JDBC-Native-lightgrey)
![Architecture](https://img.shields.io/badge/Architecture-Service%2FDAO-success)

A high-performance, console-driven banking engine built from scratch to demonstrate **advanced database concurrency**, **row-level locking**, and **pure JDBC transaction management**. 

Instead of relying on frameworks like Spring Boot or Hibernate to hide the complexity of database interactions, this system explicitly manages its own connection pools, transaction boundaries, and deadlock-prevention algorithms natively.

## 🎬 Live Terminal Demo
*(Click to play the interactive terminal recording)*

[![asciicast](https://asciinema.org/a/649HQcvFy1q6JWfy.svg)](https://asciinema.org/a/649HQcvFy1q6JWfy)

---

## ⚡ Core Engineering Highlights

### 1. Concurrency & Deadlock Prevention (The Stress Test)
In a distributed banking system, simultaneous transfers between the same accounts can cause database deadlocks. MBMS solves this natively:
* **Row-Level Locking:** Utilizes `SELECT ... FOR UPDATE` to exclusively lock account rows during a transfer.
* **Global Lock Ordering:** To prevent deadlocks when Thread A transfers $1 from Account 1->2 while Thread B transfers $1 from Account 2->1, the `AccountService` algorithmically sorts the Account IDs and always requests database locks in ascending order.
* **Proof of Execution:** The application includes a built-in multi-threaded stress test that fires 100 simultaneous, bidirectional transfers at the exact same millisecond to mathematically prove the deadlock-prevention logic holds under heavy load.

### 2. Immutable Ledger & Ledger Snapshots
* Uses manual JDBC commit/rollback boundaries (`conn.setAutoCommit(false)`) to ensure Atomicity. 
* Transactions are strictly double-entry. The audit log captures post-transaction ledger snapshots (saving both the sender's and receiver's running balances) at the exact millisecond of the transfer, guaranteeing historical balance integrity.

### 3. Role-Based Access Control (RBAC) & Security
* Implements a custom `SessionContext` to enforce Role-Based Access Control (Admin vs. Teller) at the Service layer, ensuring UI manipulation cannot bypass security.
* All employee passwords are mathematically hashed using `BCrypt` before ever reaching the database.

---

## 🛠️ Architecture & Tech Stack

* **Language:** Java 17
* **Database:** MySQL 8.0 (Dockerized)
* **Connection Pooling:** HikariCP
* **Migrations:** Flyway (Automated schema building)
* **Security:** jBCrypt
* **Design Pattern:** Clean layered architecture separating CLI presentation, Service business logic, and DAO database operations.

---

## 🚀 Getting Started

### Prerequisites
* Docker & Docker Compose
* Java 17+ and Maven

### Installation & Run Instructions

**1. Spin up the Database**
Ensure Docker is running, then boot up the MySQL container:
```bash
docker compose up -d
```

**2. Run the Application**
Maven will automatically run the Flyway migrations, build the source code, and launch the interactive CLI:
```bash
mvn clean compile exec:java "-Dexec.mainClass=com.example.Main"
```

**3. Demo Credentials**
* **Username:** `admin`
* **Password:** `admin`