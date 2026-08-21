# 💰 Personal Financial Management Platform (REST API)

[![Cloud CI/CD Pipeline](https://github.com/TheDarkFyre/personal-finance-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/TheDarkFyre/personal-finance-platform/actions)
[![Release](https://img.shields.io/github/v/release/TheDarkFyre/personal-finance-platform?color=emerald&label=Latest%20Release)](https://github.com/TheDarkFyre/personal-finance-platform/releases/latest)
[![Docker](https://img.shields.io/badge/Docker-ghcr.io-blue.svg)](https://github.com/TheDarkFyre/personal-finance-platform/pkgs/container/personal-finance-platform)
[![Java 21/23](https://img.shields.io/badge/Java-21%20%2F%2023-orange.svg)](https://openjdk.org/)
[![Spring Boot 3.3.5](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

A modern, cloud-native **Personal Financial Management Platform REST API** built with **Java 21/23**, **Spring Boot 3.3**, **PostgreSQL**, **Spring Data JPA**, **Docker**, **SpringDoc OpenAPI 3 (Swagger UI)**, **Spring Boot Actuator**, and **GitHub Actions CI/CD**.

---

## ⚡ Quick Start & Installation

You can run the application in **three different ways**:

### Option 1: Run with Docker Compose (Recommended — Zero Setup)

Run the entire platform (Spring Boot API + PostgreSQL Database) with one command. No Java or Maven required:

```bash
# Download and start the pre-built container from GitHub Container Registry
docker compose up -d
```

To stop:
```bash
docker compose down
```

---

### Option 2: Run the Standalone Executable JAR (1-Click Desktop Run)

Download [`personal-finance-api-1.0.0.jar`](https://github.com/TheDarkFyre/personal-finance-platform/releases/latest) from the [Releases section](https://github.com/TheDarkFyre/personal-finance-platform/releases/latest) and run:

```bash
java -jar personal-finance-api-1.0.0.jar
```

*(Automatically launches your default web browser straight to the dashboard at `http://localhost:8080` in ~1.5 seconds!)*

---

### Option 3: Run from Source via Maven Wrapper

#### On Windows (PowerShell / Command Prompt):
```powershell
.\mvnw.cmd spring-boot:run
```

#### On Linux / macOS / WSL (Bash):
```bash
chmod +x mvnw
./mvnw spring-boot:run
```

---

### 🖥️ Accessing the Application

Once started, open your browser to:
- 🖥️ **Interactive Web Dashboard**: [http://localhost:8080](http://localhost:8080)
- 📖 **Interactive Swagger UI (API Docs)**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- 💓 **Cloud Health Probes**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- 🗄️ **H2 Database Console (Local Dev)**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) *(JDBC URL: `jdbc:h2:mem:financedb`, User: `sa`, Password: empty)*

---

## 🚀 Key Features

1. **🏦 Multi-Account Portfolio Tracking**:
   - Track Checking, Savings, Credit Card, Investment, and Cash accounts.
   - Real-time net worth calculation and atomic balance reconciliations.

2. **🎯 Monthly Category Budgets & Overspending Alerts**:
   - Set monthly spending limits per category (e.g. Dining, Housing, Utilities).
   - Real-time progress computation, percentage used, remaining allowance, and overspending warning flags.

3. **📥 Bank Statement CSV Bulk Importer**:
   - Upload bank statement `.csv` exports (Chase, Bank of America, Amex, or standard formats).
   - Intelligent keyword-based auto-categorization and atomic account balance updates.

4. **🔄 Recurring Subscriptions & Upcoming Bills Tracker**:
   - Monitor recurring billing cycles (Monthly, Weekly, Quarterly, Annual).
   - Real-time monthly burn rate calculation and overdue/due-soon alerts.
   - 1-Click payment processing that auto-records transaction entries and advances the next billing date.

5. **🎯 Savings Goals & Milestone Progress**:
   - Create financial milestones (Emergency Fund, Vacations, Large Purchases).
   - Dynamic progress percentage, target completion countdown, and seamless deposits/withdrawals linked to bank accounts.

6. **💱 Real-Time Multi-Currency Conversion**:
   - Live exchange rates integrated with the Frankfurter FX API.
   - On-the-fly currency conversions with thread-safe in-memory caching and fallback rate lookup.

7. **📊 Advanced Financial Analytics**:
   - Real-time category spending breakdowns via custom JPQL aggregation queries.
   - Monthly cash flow summaries (Income vs. Expenses, Net Savings, and Savings Rate %).
   - Paginated transaction querying with multi-criteria filtering.

8. **🐳 Cloud-Native & Production-Ready**:
   - Cloud health probes and metrics via Spring Boot Actuator (`/actuator/health`).
   - Containerized deployment with multi-stage `Dockerfile` and `docker-compose.yml`.
   - Continuous integration pipeline with automated testing and container publishing via GitHub Actions.
   - Dual environment profiles: `dev` (H2 zero-config in-memory DB + seed data) and `prod` (PostgreSQL / AWS RDS / Render / Railway).

---

## 🛠️ Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Java 21 / Java 23 |
| **Framework** | Spring Boot 3.3.5 (Spring Web, Spring Data JPA, Spring Validation, Actuator) |
| **Database** | PostgreSQL (Production/Docker), H2 In-Memory (Dev & Integration Testing) |
| **API Docs** | SpringDoc OpenAPI 3 / Swagger UI |
| **Testing** | JUnit 5, Mockito, Spring Boot Test / MockMvc (37 Tests) |
| **Containerization**| Docker (Multi-stage Eclipse Temurin JDK 21), Docker Compose |
| **Registry** | GitHub Container Registry (`ghcr.io/thedarkfyre/personal-finance-platform:latest`) |
| **CI/CD** | GitHub Actions (`.github/workflows/ci.yml`) |

---

## 🧪 Automated Testing

Execute the comprehensive automated test suite (37 unit & integration tests):

```bash
# Windows
.\mvnw.cmd test

# Linux / WSL / macOS
./mvnw test
```

---

## 📡 REST API Endpoints Overview

### 🏦 Accounts (`/api/v1/accounts`)
- `GET /api/v1/accounts` - List all financial accounts.
- `GET /api/v1/accounts/{id}` - Get account details by ID.
- `POST /api/v1/accounts` - Create a new account.
- `PUT /api/v1/accounts/{id}` - Update account details.
- `DELETE /api/v1/accounts/{id}` - Delete an account.

### 🏷️ Categories (`/api/v1/categories`)
- `GET /api/v1/categories` - List all income and expense categories.
- `POST /api/v1/categories` - Create custom category.

### 💳 Transactions & CSV Import (`/api/v1/transactions`)
- `GET /api/v1/transactions` - Filtered & paginated transactions (`?accountId=1&month=8&year=2026`).
- `POST /api/v1/transactions` - Record a transaction (atomically updates account balance).
- `POST /api/v1/transactions/import-csv` - Bulk import bank statement CSV with auto-categorization.
- `DELETE /api/v1/transactions/{id}` - Delete a transaction (reverts account balance).

### 🎯 Monthly Budgets (`/api/v1/budgets`)
- `GET /api/v1/budgets` - List configured category budgets.
- `GET /api/v1/budgets/progress?year=2026&month=8` - Real-time budget progress, spending caps, and alerts.
- `POST /api/v1/budgets` - Set or update monthly category budget.
- `DELETE /api/v1/budgets/{id}` - Remove budget limit.

### 🔄 Subscriptions & Bills (`/api/v1/subscriptions`)
- `GET /api/v1/subscriptions` - List recurring subscriptions.
- `GET /api/v1/subscriptions/summary` - Total monthly burn rate and upcoming bills.
- `POST /api/v1/subscriptions` - Track a new recurring subscription/bill.
- `POST /api/v1/subscriptions/{id}/pay` - 1-Click Pay: records transaction and rolls next due date forward.
- `PUT /api/v1/subscriptions/{id}/pause` - Pause subscription.
- `PUT /api/v1/subscriptions/{id}/resume` - Resume subscription.
- `DELETE /api/v1/subscriptions/{id}` - Delete tracked subscription.

### 🎯 Savings Goals (`/api/v1/goals`)
- `GET /api/v1/goals` - List savings goals and progress.
- `POST /api/v1/goals` - Create a new financial savings goal.
- `POST /api/v1/goals/{id}/deposit` - Contribute funds to a goal (deducts from linked account).
- `POST /api/v1/goals/{id}/withdraw` - Withdraw savings from a goal (refunds linked account).
- `DELETE /api/v1/goals/{id}` - Delete a savings goal.

### 💱 Currency Conversion (`/api/v1/currencies`)
- `GET /api/v1/currencies/rates?base=USD` - Live exchange rate table.
- `GET /api/v1/currencies/convert?from=USD&to=EUR&amount=100.00` - Convert between currencies.

### 📊 Analytics (`/api/v1/analytics`)
- `GET /api/v1/analytics/cash-flow` - Current month income, expenses, and savings rate.
- `GET /api/v1/analytics/spending-by-category` - Aggregated spending per category.

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
