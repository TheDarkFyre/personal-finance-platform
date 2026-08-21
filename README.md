# 💰 Personal Financial Management Platform (REST API)

A modern, cloud-native **Personal Financial Management REST API** built with **Java 21/23**, **Spring Boot 3.3**, **PostgreSQL**, **Spring Data JPA**, **Docker**, **SpringDoc OpenAPI 3 (Swagger UI)**, **Spring Boot Actuator**, and **GitHub Actions CI/CD**.

---

## 🚀 Key Features

- **Multi-Account Management**: Supports Checking, Savings, Credit Card, Investment, and Cash accounts with live net worth calculation.
- **Categorized Cash Flow**: Multi-tiered income and expense categorization with icons and color tags.
- **Transactional Integrity**: Atomic balance updates upon transaction creation, deletion (with balance reversion), and updates using `@Transactional`.
- **Advanced Financial Analytics**:
  - Real-time spending breakdown by category with custom JPQL queries.
  - Monthly cash flow summaries (Income vs. Expenses, Net Savings, and Savings Rate %).
  - Paginated transaction querying with multi-criteria filtering (by account, category, and date range).
- **Cloud-Native & Production-Ready**:
  - Cloud health probes and metrics via Spring Boot Actuator (`/actuator/health`).
  - Containerized deployment with multi-stage `Dockerfile` and `docker-compose.yml`.
  - Continuous integration pipeline with automated testing via GitHub Actions.
  - Dual environment profiles: `dev` (H2 zero-config in-memory DB + seed data) and `prod` (PostgreSQL / AWS RDS / Render / Railway).
- **Developer Experience**:
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

#### On Linux / WSL / macOS (Bash):
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

# Linux / WSL
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
- `GET /api/v1/categories` - List categories (optional `?type=EXPENSE` or `?type=INCOME`).
- `GET /api/v1/categories/{id}` - Get category details.
- `POST /api/v1/categories` - Create a new category.
- `PUT /api/v1/categories/{id}` - Update category.
- `DELETE /api/v1/categories/{id}` - Delete category.

### 💳 Transactions (`/api/v1/transactions`)
- `GET /api/v1/transactions` - Paginated transaction history (filters: `accountId`, `categoryId`, `startDate`, `endDate`).
- `GET /api/v1/transactions/{id}` - Get transaction by ID.
- `POST /api/v1/transactions` - Record transaction (atomically adjusts account balance).
- `DELETE /api/v1/transactions/{id}` - Delete transaction (atomically reverts account balance).

### 📊 Analytics & Reports (`/api/v1/analytics`)
- `GET /api/v1/analytics/spending-by-category` - Aggregated spending breakdown for date range.
- `GET /api/v1/analytics/income-by-category` - Aggregated income breakdown for date range.
- `GET /api/v1/analytics/monthly-summary?year=2026&month=8` - Monthly income, expenses, and savings rate.
- `GET /api/v1/analytics/cash-flow` - Complete cash flow and net worth overview.

---

## ☁️ Cloud Deployment Guide

### Deploying to Render / Railway / AWS
1. **Set Environment Variables**:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `SPRING_DATASOURCE_URL=jdbc:postgresql://<db-host>:5432/<db-name>`
   - `SPRING_DATASOURCE_USERNAME=<db-user>`
   - `SPRING_DATASOURCE_PASSWORD=<db-password>`
2. **Build Command**: `./mvnw clean package -DskipTests`
3. **Start Command**: `java -jar target/personal-finance-api-1.0.0.jar`
