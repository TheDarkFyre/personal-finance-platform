package com.finance.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.app.dto.TransactionRequestDTO;
import com.finance.app.entity.Account;
import com.finance.app.entity.AccountType;
import com.finance.app.entity.Category;
import com.finance.app.entity.CategoryType;
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
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Account testAccount;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        categoryRepository.deleteAll();

        testAccount = accountRepository.save(Account.builder()
                .name("Integration Test Account")
                .type(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .name("Dining")
                .type(CategoryType.EXPENSE)
                .build());
    }

    @Test
    @DisplayName("POST /api/v1/transactions - Successfully creates transaction and updates balance")
    void shouldCreateTransactionViaApi() throws Exception {
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .amount(new BigDecimal("75.50"))
                .transactionDate(LocalDate.now())
                .description("Dinner with friends")
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.amount", is(75.50)))
                .andExpect(jsonPath("$.categoryName", is("Dining")))
                .andExpect(jsonPath("$.accountName", is("Integration Test Account")));

        // Verify account balance in database was updated to 924.50
        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("924.50"), updatedAccount.getBalance());
    }

    @Test
    @DisplayName("POST /api/v1/transactions - Returns 400 Bad Request when amount is negative or missing")
    void shouldReturnBadRequestForInvalidPayload() throws Exception {
        TransactionRequestDTO invalidRequest = TransactionRequestDTO.builder()
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .amount(new BigDecimal("-10.00")) // Invalid negative
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.amount", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/accounts - Retrieves accounts list")
    void shouldFetchAccounts() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", is("Integration Test Account")));
    }
}
