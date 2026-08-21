# 💰 Personal Financial Management Platform (REST API)

A modern, cloud-native **Personal Financial Management Platform REST API** built with **Java 21/23**, **Spring Boot 3.3**, **PostgreSQL**, **Spring Data JPA**, **Docker**, **SpringDoc OpenAPI 3 (Swagger UI)**, **Spring Boot Actuator**, and **GitHub Actions CI/CD**.

---

## 🚀 Key Features

1. **Multi-Account Portfolio Tracking**:
   - Track Checking, Savings, Credit Card, Investment, and Cash accounts.
   - Real-time net worth calculation and balance reconciliations.

2. **Monthly Category Budgets & Overspending Alerts**:
   - Set monthly spending limits per category (e.g. Dining, Housing, Utilities).
   - Real-time progress computation, percentage used, remaining allowance, and overspending warning flags.

3. **Bank Statement CSV Bulk Importer**:
   - Upload bank statement `.csv` exports (Chase, Bank of America, Amex, or standard formats).
   - Intelligent keyword-based auto-categorization and atomic account balance updates.

4. **Recurring Subscriptions & Upcoming Bills Tracker**:
   - Monitor recurring billing cycles (Monthly, Weekly, Quarterly, Annual).
   - Real-time monthly burn rate calculation and overdue/due-soon alerts.
   - 1-Click payment processing that auto-records transaction entries and advances the next billing date.

5. **Savings Goals & Milestone Progress**:
   - Create financial milestones (Emergency Fund, Vacations, Large Purchases).
   - Dynamic progress percentage, target completion countdown, and seamless deposits/withdrawals linked to bank accounts.

6. **Advanced Financial Analytics**:
   - Real-time category spending breakdowns via custom JPQL aggregation queries.
   - Monthly cash flow summaries (Income vs. Expenses, Net Savings, and Savings Rate %).
   - Paginated transaction querying with multi-criteria filtering.

7. **Cloud-Native & Production-Ready**:
   - Cloud health probes and metrics via Spring Boot Actuator (`/actuator/health`).
   - Containerized deployment with multi-stage `Dockerfile` and `docker-compose.yml`.
   - Continuous integration pipeline with automated testing via GitHub Actions.
   - Dual environment profiles: `dev` (H2 zero-config in-memory DB + seed data) and `prod` (PostgreSQL / AWS RDS / Render / Railway).

8. **Developer Experience**:
   - Interactive Swagger UI documentation at `/swagger-ui.html`.
   - Built-in visual dashboard frontend at `http://localhost:8080/`.
   - VS Code `requests.http` test collection for 1-click endpoint verification.

---

## 🛠️ Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Java 21 / Java 23 |
| **Framework** | Spring Boot 3.3.5 (Spring Web, Spring Data JPA, Spring Validation, Actuator) |
| **Database** | PostgreSQL (Production/Docker), H2 In-Memory (Dev & Integration Testing) |
| **API Docs** | SpringDoc OpenAPI 3 / Swagger UI |
| **Testing** | JUnit 5, Mockito, Spring Boot Test / MockMvc |
| **Containerization**| Docker (Multi-stage Eclipse Temurin JDK 21), Docker Compose |
| **CI/CD** | GitHub Actions (`.github/workflows/ci.yml`) |

---

## ⚡ Quick Start

### 1. Run Locally (Zero-Config with In-Memory H2 & Demo Data)

#### On Windows (PowerShell / CMD):
```powershell
.\mvnw.cmd spring-boot:run
```

#### On Linux / macOS / WSL (Bash):
```bash
chmod +x mvnw
./mvnw spring-boot:run
```

Once started, access:
- 🖥️ **Interactive Web Dashboard**: [http://localhost:8080](http://localhost:8080)
- 📖 **Swagger UI Documentation**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- 💓 **Cloud Health Probes**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- 🗄️ **H2 Database Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) *(JDBC URL: `jdbc:h2:mem:financedb`, User: `sa`, Password: empty)*

---

### 2. Run with Docker & PostgreSQL

Start both the Spring Boot API and PostgreSQL database with one command:

```bash
docker-compose up --build -d
```

To stop:
```bash
docker-compose down
```

---

### 3. Run Automated Tests

Execute unit and integration tests:

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

### 📊 Analytics (`/api/v1/analytics`)
- `GET /api/v1/analytics/cash-flow` - Current month income, expenses, and savings rate.
- `GET /api/v1/analytics/spending-by-category` - Aggregated spending per category.

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
