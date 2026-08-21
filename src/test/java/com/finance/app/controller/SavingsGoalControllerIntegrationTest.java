package com.finance.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.app.dto.SavingsGoalContributionDTO;
import com.finance.app.dto.SavingsGoalRequestDTO;
import com.finance.app.entity.Account;
import com.finance.app.entity.AccountType;
import com.finance.app.entity.GoalStatus;
import com.finance.app.entity.SavingsGoal;
import com.finance.app.repository.*;
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
class SavingsGoalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

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
                .name("Savings Account")
                .type(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .currency("USD")
                .build());
    }

    @Test
    @DisplayName("POST /api/v1/goals - Creates a new savings goal with initial deposit")
    void shouldCreateSavingsGoalViaApi() throws Exception {
        SavingsGoalRequestDTO request = SavingsGoalRequestDTO.builder()
                .name("Vacation in Hawaii")
                .targetAmount(new BigDecimal("3000.00"))
                .initialDeposit(new BigDecimal("500.00"))
                .accountId(testAccount.getId())
                .targetDate(LocalDate.now().plusMonths(4))
                .icon("🏝️")
                .color("#3B82F6")
                .build();

        mockMvc.perform(post("/api/v1/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Vacation in Hawaii")))
                .andExpect(jsonPath("$.currentAmount", is(500.00)))
                .andExpect(jsonPath("$.progressPercent", is(16.67)))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        // Account deducted: 5000 - 500 = 4500
        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("4500.00"), updatedAccount.getBalance());
    }

    @Test
    @DisplayName("POST /api/v1/goals/{id}/deposit - Deposits money into savings goal")
    void shouldDepositMoneyIntoGoalViaApi() throws Exception {
        SavingsGoal goal = savingsGoalRepository.save(SavingsGoal.builder()
                .name("MacBook Pro")
                .targetAmount(new BigDecimal("2000.00"))
                .currentAmount(new BigDecimal("1800.00"))
                .account(testAccount)
                .status(GoalStatus.IN_PROGRESS)
                .build());

        SavingsGoalContributionDTO contribution = SavingsGoalContributionDTO.builder()
                .amount(new BigDecimal("200.00"))
                .build();

        mockMvc.perform(post("/api/v1/goals/" + goal.getId() + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contribution)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount", is(2000.00)))
                .andExpect(jsonPath("$.progressPercent", is(100.00)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.isCompleted", is(true)));
    }
}
