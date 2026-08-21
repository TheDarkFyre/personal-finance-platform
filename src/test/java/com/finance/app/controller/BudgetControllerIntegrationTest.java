package com.finance.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.app.dto.BudgetRequestDTO;
import com.finance.app.entity.Account;
import com.finance.app.entity.AccountType;
import com.finance.app.entity.Category;
import com.finance.app.entity.CategoryType;
import com.finance.app.entity.Transaction;
import com.finance.app.repository.AccountRepository;
import com.finance.app.repository.BudgetRepository;
import com.finance.app.repository.CategoryRepository;
import com.finance.app.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class BudgetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.finance.app.repository.SubscriptionRepository subscriptionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Category testCategory;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        categoryRepository.deleteAll();


        testCategory = categoryRepository.save(Category.builder()
                .name("Groceries")
                .type(CategoryType.EXPENSE)
                .icon("🛒")
                .color("#34D399")
                .build());

        testAccount = accountRepository.save(Account.builder()
                .name("Checking Account")
                .type(AccountType.CHECKING)
                .balance(new BigDecimal("2000.00"))
                .currency("USD")
                .build());
    }

    @Test
    @DisplayName("POST /api/v1/budgets - Successfully creates a monthly category budget")
    void shouldCreateBudgetViaApi() throws Exception {
        BudgetRequestDTO request = BudgetRequestDTO.builder()
                .categoryId(testCategory.getId())
                .limitAmount(new BigDecimal("400.00"))
                .month(8)
                .year(2026)
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.categoryName", is("Groceries")))
                .andExpect(jsonPath("$.limitAmount", is(400.00)))
                .andExpect(jsonPath("$.month", is(8)))
                .andExpect(jsonPath("$.year", is(2026)));
    }

    @Test
    @DisplayName("GET /api/v1/budgets/progress - Returns budget progress and overspending status")
    void shouldGetBudgetProgressViaApi() throws Exception {
        // 1. Create a budget: $300 for Groceries in current month
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        BudgetRequestDTO request = BudgetRequestDTO.builder()
                .categoryId(testCategory.getId())
                .limitAmount(new BigDecimal("300.00"))
                .month(month)
                .year(year)
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 2. Create a transaction of $100 for Groceries
        transactionRepository.save(Transaction.builder()
                .account(testAccount)
                .category(testCategory)
                .amount(new BigDecimal("100.00"))
                .transactionDate(today)
                .description("Supermarket run")
                .build());

        // 3. Fetch progress
        mockMvc.perform(get("/api/v1/budgets/progress")
                        .param("year", String.valueOf(year))
                        .param("month", String.valueOf(month)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].categoryName", is("Groceries")))
                .andExpect(jsonPath("$[0].limitAmount", is(300.00)))
                .andExpect(jsonPath("$[0].spentAmount", is(100.00)))
                .andExpect(jsonPath("$[0].remainingAmount", is(200.00)))
                .andExpect(jsonPath("$[0].percentageUsed", is(33.33)))
                .andExpect(jsonPath("$[0].isOverBudget", is(false)));
    }
}
