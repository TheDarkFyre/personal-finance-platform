package com.finance.app.service;

import com.finance.app.dto.SubscriptionRequestDTO;
import com.finance.app.dto.SubscriptionResponseDTO;
import com.finance.app.dto.SubscriptionSummaryDTO;
import com.finance.app.dto.TransactionRequestDTO;
import com.finance.app.dto.TransactionResponseDTO;
import com.finance.app.entity.*;
import com.finance.app.repository.AccountRepository;
import com.finance.app.repository.CategoryRepository;
import com.finance.app.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private Account testAccount;
    private Category testCategory;
    private Subscription netflixSub;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .name("Checking Account")
                .type(AccountType.CHECKING)
                .balance(new BigDecimal("2000.00"))
                .build();

        testCategory = Category.builder()
                .id(10L)
                .name("Entertainment")
                .type(CategoryType.EXPENSE)
                .icon("🎬")
                .build();

        netflixSub = Subscription.builder()
                .id(100L)
                .name("Netflix")
                .amount(new BigDecimal("19.99"))
                .category(testCategory)
                .account(testAccount)
                .frequency(BillingFrequency.MONTHLY)
                .nextDueDate(LocalDate.now().plusDays(5))
                .status(SubscriptionStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should create a new recurring subscription")
    void shouldCreateSubscription() {
        SubscriptionRequestDTO request = SubscriptionRequestDTO.builder()
                .name("Netflix")
                .amount(new BigDecimal("19.99"))
                .categoryId(10L)
                .accountId(1L)
                .frequency(BillingFrequency.MONTHLY)
                .nextDueDate(LocalDate.now().plusDays(5))
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(testCategory));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        SubscriptionResponseDTO response = subscriptionService.createSubscription(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Netflix", response.getName());
        assertEquals(new BigDecimal("19.99"), response.getAmount());
        assertEquals(BillingFrequency.MONTHLY, response.getFrequency());
        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Should calculate monthly burn rate and upcoming bills count")
    void shouldCalculateSubscriptionSummary() {
        Subscription annualGym = Subscription.builder()
                .id(101L)
                .name("Annual Gym Membership")
                .amount(new BigDecimal("1200.00"))
                .category(testCategory)
                .account(testAccount)
                .frequency(BillingFrequency.ANNUAL) // $1200 / 12 = $100/mo
                .nextDueDate(LocalDate.now().plusDays(2))
                .status(SubscriptionStatus.ACTIVE)
                .build();

        when(subscriptionRepository.findByStatusOrderByNextDueDateAsc(SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(netflixSub, annualGym));
        when(subscriptionRepository.findAll()).thenReturn(List.of(netflixSub, annualGym));

        SubscriptionSummaryDTO summary = subscriptionService.getSubscriptionSummary();

        assertNotNull(summary);
        assertEquals(2, summary.getActiveCount());
        // $19.99 + $100.00 = $119.99 monthly burn rate
        assertEquals(new BigDecimal("119.99"), summary.getMonthlyBurnRate());
        assertEquals(2, summary.getDueSoonCount());
    }

    @Test
    @DisplayName("Should process 1-click subscription payment and advance next due date by 1 month")
    void shouldPaySubscriptionAndAdvanceDueDate() {
        LocalDate originalDueDate = LocalDate.of(2026, 8, 15);
        netflixSub.setNextDueDate(originalDueDate);

        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(netflixSub));
        when(transactionService.createTransaction(any(TransactionRequestDTO.class)))
                .thenReturn(TransactionResponseDTO.builder().id(999L).amount(new BigDecimal("19.99")).build());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponseDTO result = subscriptionService.paySubscription(100L);

        assertNotNull(result);
        assertEquals(999L, result.getId());
        // Verify next due date was advanced from 2026-08-15 to 2026-09-15
        assertEquals(LocalDate.of(2026, 9, 15), netflixSub.getNextDueDate());
        verify(transactionService, times(1)).createTransaction(any(TransactionRequestDTO.class));
        verify(subscriptionRepository, times(1)).save(netflixSub);
    }

    @Test
    @DisplayName("Should pause and resume subscription status")
    void shouldPauseAndResumeSubscription() {
        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(netflixSub));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionResponseDTO paused = subscriptionService.updateStatus(100L, SubscriptionStatus.PAUSED);
        assertEquals(SubscriptionStatus.PAUSED, paused.getStatus());

        SubscriptionResponseDTO resumed = subscriptionService.updateStatus(100L, SubscriptionStatus.ACTIVE);
        assertEquals(SubscriptionStatus.ACTIVE, resumed.getStatus());
    }
}
