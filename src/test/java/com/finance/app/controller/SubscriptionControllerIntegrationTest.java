package com.finance.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.app.dto.SubscriptionRequestDTO;
import com.finance.app.entity.*;
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
class SubscriptionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.finance.app.repository.SavingsGoalRepository savingsGoalRepository;

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
    private Category testCategory;

    @BeforeEach
    void setUp() {
        savingsGoalRepository.deleteAll();
        subscriptionRepository.deleteAll();
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        categoryRepository.deleteAll();


        testAccount = accountRepository.save(Account.builder()
                .name("Primary Checking")
                .type(AccountType.CHECKING)
                .balance(new BigDecimal("1500.00"))
                .currency("USD")
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .name("Media & Subs")
                .type(CategoryType.EXPENSE)
                .icon("🎬")
                .build());
    }

    @Test
    @DisplayName("POST /api/v1/subscriptions - Creates a new recurring subscription")
    void shouldCreateSubscriptionViaApi() throws Exception {
        SubscriptionRequestDTO request = SubscriptionRequestDTO.builder()
                .name("Spotify Duo")
                .amount(new BigDecimal("14.99"))
                .categoryId(testCategory.getId())
                .accountId(testAccount.getId())
                .frequency(BillingFrequency.MONTHLY)
                .nextDueDate(LocalDate.now().plusDays(10))
                .build();

        mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Spotify Duo")))
                .andExpect(jsonPath("$.amount", is(14.99)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("POST /api/v1/subscriptions/{id}/pay - Deducts balance and rolls next due date forward")
    void shouldPaySubscriptionViaApi() throws Exception {
        Subscription sub = subscriptionRepository.save(Subscription.builder()
                .name("Gym Membership")
                .amount(new BigDecimal("50.00"))
                .category(testCategory)
                .account(testAccount)
                .frequency(BillingFrequency.MONTHLY)
                .nextDueDate(LocalDate.now())
                .status(SubscriptionStatus.ACTIVE)
                .build());

        mockMvc.perform(post("/api/v1/subscriptions/" + sub.getId() + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(50.00)))
                .andExpect(jsonPath("$.description", containsString("Gym Membership")));

        // Verify account balance: 1500 - 50 = 1450.00
        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("1450.00"), updatedAccount.getBalance());

        // Verify next due date advanced by 1 month
        Subscription updatedSub = subscriptionRepository.findById(sub.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.now().plusMonths(1), updatedSub.getNextDueDate());
    }

    @Test
    @DisplayName("GET /api/v1/subscriptions/summary - Returns monthly recurring burn rate")
    void shouldGetSubscriptionSummaryViaApi() throws Exception {
        subscriptionRepository.save(Subscription.builder()
                .name("Netflix")
                .amount(new BigDecimal("20.00"))
                .category(testCategory)
                .account(testAccount)
                .frequency(BillingFrequency.MONTHLY)
                .nextDueDate(LocalDate.now().plusDays(3))
                .status(SubscriptionStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/subscriptions/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCount", is(1)))
                .andExpect(jsonPath("$.monthlyBurnRate", is(20.00)))
                .andExpect(jsonPath("$.dueSoonCount", is(1)));
    }
}
