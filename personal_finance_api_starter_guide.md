# Personal Financial Management Platform (REST API)
### Complete Architectural Blueprint & Implementation Guide

This guide walks through building, testing, containerizing, and deploying a modern, cloud-native **Personal Financial Management REST API** using Java 21, Spring Boot 3, PostgreSQL, Docker, JUnit 5, and GitHub Actions CI/CD.

---

## 1. Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Java 17 or Java 21 |
| **Framework** | Spring Boot 3.x (Spring Web, Spring Data JPA, Spring Validation) |
| **Database** | PostgreSQL (Production / Local), H2 In-Memory (Integration Testing) |
| **API Documentation** | SpringDoc OpenAPI 3 / Swagger UI |
| **Testing** | JUnit 5, Mockito, Spring Boot Test / MockMvc |
| **Containerization** | Docker, Docker Compose |
| **CI/CD & Cloud** | GitHub Actions, Render / Railway / AWS Free Tier |

---

## 2. Project Architecture & Database Design

### Layered Architecture
```
[Client / Postman / Swagger UI]
            │ HTTP / JSON
            ▼
[Controller Layer (@RestController)]
  - Handles HTTP requests, query params, DTO validation (@Valid)
            │
            ▼
[Service Layer (@Service)]
  - Business logic, balance updates, transaction handling (@Transactional)
            │
            ▼
[Data Access Layer (@Repository)]
  - Spring Data JPA, custom JPQL queries, database operations
            │
            ▼
[Database (PostgreSQL / H2)]
```

### Relational Schema Design

```
 ┌─────────────────┐       1:N       ┌────────────────────────┐
 │      users      │────────────────▶│        accounts        │
 ├─────────────────┤                 ├────────────────────────┤
 │ id (PK)         │                 │ id (PK)                │
 │ email (UNIQUE)  │                 │ user_id (FK)           │
 │ password_hash   │                 │ name (e.g. Checking)   │
 │ first_name      │                 │ type (CHECKING/SAVINGS)│
 │ created_at      │                 │ balance (DECIMAL)      │
 └─────────────────┘                 └───────────┬────────────┘
                                                 │
                                                 │ 1:N
                                                 ▼
 ┌─────────────────┐       1:N       ┌────────────────────────┐
 │   categories    │────────────────▶│      transactions      │
 ├─────────────────┤                 ├────────────────────────┤
 │ id (PK)         │                 │ id (PK)                │
 │ name (e.g. Food)│                 │ account_id (FK)        │
 │ type (IN/OUT)   │                 │ category_id (FK)       │
 └─────────────────┘                 │ amount (DECIMAL)       │
                                     │ transaction_date       │
                                     │ description            │
                                     └────────────────────────┘
```

---

## 3. Step-by-Step Implementation Guide

### Step 1: Project Initialization
1. Navigate to [start.spring.io](https://start.spring.io/).
2. Select:
   - **Project:** Maven
   - **Language:** Java
   - **Spring Boot:** 3.3.x (or latest stable 3.x)
   - **Packaging:** Jar
   - **Java:** 17 or 21
3. Add the following dependencies:
   - `Spring Web`
   - `Spring Data JPA`
   - `PostgreSQL Driver`
   - `Validation`
   - `Lombok`
   - `H2 Database` (scope: test / runtime)
4. Add the Swagger UI dependency in `pom.xml`:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

---

### Step 2: Database Entities (JPA)

#### `Account.java`
```java
package com.finance.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Account name is required")
    private String name;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Account type is required")
    private AccountType type; // CHECKING, SAVINGS, CREDIT

    @NotNull
    private BigDecimal balance;
}
```

#### `Transaction.java`
```java
package com.finance.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotNull
    @Positive(message = "Transaction amount must be greater than zero")
    private BigDecimal amount;

    @NotNull
    private LocalDate transactionDate;

    private String description;
}
```

---

### Step 3: Repositories & Analytics Queries

#### `TransactionRepository.java`
```java
package com.finance.app.repository;

import com.finance.app.dto.CategorySummaryDTO;
import com.finance.app.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);

    @Query("SELECT new com.finance.app.dto.CategorySummaryDTO(t.category.name, SUM(t.amount)) " +
           "FROM Transaction t " +
           "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category.name")
    List<CategorySummaryDTO> getSpendingByCategory(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
```

---

### Step 4: Service Layer & Exception Handling

#### `TransactionService.java`
```java
package com.finance.app.service;

import com.finance.app.dto.TransactionRequestDTO;
import com.finance.app.entity.Account;
import com.finance.app.entity.Category;
import com.finance.app.entity.Transaction;
import com.finance.app.exception.ResourceNotFoundException;
import com.finance.app.repository.AccountRepository;
import com.finance.app.repository.CategoryRepository;
import com.finance.app.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepo;
    private final AccountRepository accountRepo;
    private final CategoryRepository categoryRepo;

    @Transactional
    public Transaction createTransaction(TransactionRequestDTO dto) {
        Account account = accountRepo.findById(dto.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + dto.getAccountId()));
        
        Category category = categoryRepo.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + dto.getCategoryId()));

        // Adjust Account Balance atomically
        if (category.getType().equals("EXPENSE")) {
            account.setBalance(account.getBalance().subtract(dto.getAmount()));
        } else {
            account.setBalance(account.getBalance().add(dto.getAmount()));
        }
        accountRepo.save(account);

        Transaction transaction = Transaction.builder()
                .account(account)
                .category(category)
                .amount(dto.getAmount())
                .transactionDate(dto.getTransactionDate())
                .description(dto.getDescription())
                .build();

        return transactionRepo.save(transaction);
    }
}
```

#### Centralized Exception Handling (`GlobalExceptionHandler.java`)
```java
package com.finance.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
}
```

---

### Step 5: REST Controllers

#### `TransactionController.java`
```java
package com.finance.app.controller;

import com.finance.app.dto.TransactionRequestDTO;
import com.finance.app.entity.Transaction;
import com.finance.app.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management and recording")
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Record a new transaction and update account balance")
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody TransactionRequestDTO request) {
        Transaction created = transactionService.createTransaction(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
}
```

---

### Step 6: Unit & Integration Testing

#### `TransactionServiceTest.java` (JUnit 5 + Mockito)
```java
package com.finance.app.service;

import com.finance.app.dto.TransactionRequestDTO;
import com.finance.app.entity.*;
import com.finance.app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepo;
    @Mock private AccountRepository accountRepo;
    @Mock private CategoryRepository categoryRepo;

    @InjectMocks private TransactionService transactionService;

    private Account testAccount;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder().id(1L).name("Checking").balance(new BigDecimal("1000.00")).build();
        testCategory = Category.builder().id(2L).name("Food").type("EXPENSE").build();
    }

    @Test
    void shouldCreateTransactionAndDeductAccountBalance() {
        TransactionRequestDTO dto = new TransactionRequestDTO(1L, 2L, new BigDecimal("150.00"), LocalDate.now(), "Dinner");

        when(accountRepo.findById(1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepo.findById(2L)).thenReturn(Optional.of(testCategory));
        when(transactionRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.createTransaction(dto);

        assertNotNull(result);
        assertEquals(new BigDecimal("850.00"), testAccount.getBalance());
        verify(transactionRepo, times(1)).save(any(Transaction.class));
    }
}
```

---

### Step 7: Containerization & Local Setup

#### `Dockerfile`
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### `docker-compose.yml`
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: finance_postgres
    environment:
      POSTGRES_DB: finance_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  app:
    build: .
    container_name: finance_api
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/finance_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
    depends_on:
      - postgres

volumes:
  pgdata:
```

---

### Step 8: CI/CD Pipeline (GitHub Actions)

Create `.github/workflows/ci.yml`:
```yaml
name: Java CI with Maven

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven
    - name: Run Test Suite
      run: mvn -B test
    - name: Build Application
      run: mvn -B package -DskipTests
```

---

## 4. Resulting Resume Bullet Points

```markdown
Personal Financial Management Platform & REST API
Java 21, Spring Boot 3, PostgreSQL, Docker, JUnit 5, GitHub Actions, AWS/Render
• Architected a cloud-native financial management REST API using Spring Boot and PostgreSQL, processing account balances, categorized transactions, and recurring expense workflows.
• Implemented multi-criteria querying with pagination, custom JPQL aggregations for monthly spending metrics, and centralized exception handling with HTTP status codes.
• Enforced transactional integrity with Spring Data JPA and secured endpoints using bean validation and structured DTO mapping.
• Authored comprehensive unit and integration test suites using JUnit 5 & Mockito; containerized the service using Docker and configured continuous testing via GitHub Actions CI/CD.
```
