package com.finance.app.controller;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class CsvImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.finance.app.repository.SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private com.finance.app.repository.SubscriptionRepository subscriptionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        savingsGoalRepository.deleteAll();
        subscriptionRepository.deleteAll();
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        categoryRepository.deleteAll();



        testAccount = accountRepository.save(Account.builder()
                .name("Chase Checking " + System.currentTimeMillis())
                .type(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .build());

        categoryRepository.findByNameIgnoreCase("Food & Dining").orElseGet(() ->
                categoryRepository.save(Category.builder()
                        .name("Food & Dining")
                        .type(CategoryType.EXPENSE)
                        .build()));

        categoryRepository.findByNameIgnoreCase("Salary & Wages").orElseGet(() ->
                categoryRepository.save(Category.builder()
                        .name("Salary & Wages")
                        .type(CategoryType.INCOME)
                        .build()));
    }


    @Test
    @DisplayName("POST /api/v1/transactions/import-csv - Imports statement file and adjusts account balance")
    void shouldImportBankStatementViaApi() throws Exception {
        String csv = "Date,Description,Amount\n" +
                "2026-08-10,Whole Foods Groceries,-50.00\n" +
                "2026-08-15,Payroll Deposit,1500.00\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/transactions/import-csv")
                        .file(file)
                        .param("accountId", String.valueOf(testAccount.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalParsed", is(2)))
                .andExpect(jsonPath("$.totalImported", is(2)))
                .andExpect(jsonPath("$.totalSkipped", is(0)))
                .andExpect(jsonPath("$.importedTransactions", hasSize(2)));

        // Verify account balance: 1000 - 50 + 1500 = 2450.00
        Account updated = accountRepository.findById(testAccount.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("2450.00"), updated.getBalance());
    }
}
