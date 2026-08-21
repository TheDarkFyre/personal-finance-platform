package com.finance.app.service;

import com.finance.app.dto.SavingsGoalContributionDTO;
import com.finance.app.dto.SavingsGoalRequestDTO;
import com.finance.app.dto.SavingsGoalResponseDTO;
import com.finance.app.entity.Account;
import com.finance.app.entity.AccountType;
import com.finance.app.entity.GoalStatus;
import com.finance.app.entity.SavingsGoal;
import com.finance.app.repository.AccountRepository;
import com.finance.app.repository.SavingsGoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavingsGoalServiceTest {

    @Mock
    private SavingsGoalRepository savingsGoalRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private SavingsGoalService savingsGoalService;

    private Account testAccount;
    private SavingsGoal testGoal;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .name("High Yield Savings")
                .type(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .build();

        testGoal = SavingsGoal.builder()
                .id(10L)
                .name("Emergency Fund")
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(new BigDecimal("600.00"))
                .account(testAccount)
                .status(GoalStatus.IN_PROGRESS)
                .targetDate(LocalDate.now().plusMonths(6))
                .build();
    }

    @Test
    @DisplayName("Should create savings goal with initial deposit deducted from account")
    void shouldCreateSavingsGoal() {
        SavingsGoalRequestDTO request = SavingsGoalRequestDTO.builder()
                .name("Emergency Fund")
                .targetAmount(new BigDecimal("1000.00"))
                .initialDeposit(new BigDecimal("200.00"))
                .accountId(1L)
                .targetDate(LocalDate.now().plusMonths(6))
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> {
            SavingsGoal g = inv.getArgument(0);
            g.setId(10L);
            return g;
        });

        SavingsGoalResponseDTO response = savingsGoalService.createGoal(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Emergency Fund", response.getName());
        assertEquals(new BigDecimal("200.00"), response.getCurrentAmount());
        assertEquals(new BigDecimal("20.00"), response.getProgressPercent());
        // Verify account balance deducted by 200: 5000 - 200 = 4800
        assertEquals(new BigDecimal("4800.00"), testAccount.getBalance());
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    @DisplayName("Should deposit to savings goal, adjust linked account balance, and mark COMPLETED when target reached")
    void shouldDepositToGoalAndComplete() {
        when(savingsGoalRepository.findById(10L)).thenReturn(Optional.of(testGoal));
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        // Deposit $400 to reach $1000 target
        SavingsGoalContributionDTO contribution = SavingsGoalContributionDTO.builder()
                .amount(new BigDecimal("400.00"))
                .build();

        SavingsGoalResponseDTO response = savingsGoalService.depositToGoal(10L, contribution);

        assertNotNull(response);
        assertEquals(new BigDecimal("1000.00"), response.getCurrentAmount());
        assertEquals(new BigDecimal("100.00"), response.getProgressPercent());
        assertEquals(GoalStatus.COMPLETED, response.getStatus());
        assertTrue(response.isCompleted());
        // Verify account deducted: 5000 - 400 = 4600
        assertEquals(new BigDecimal("4600.00"), testAccount.getBalance());
    }

    @Test
    @DisplayName("Should withdraw from savings goal and refund linked account")
    void shouldWithdrawFromGoal() {
        when(savingsGoalRepository.findById(10L)).thenReturn(Optional.of(testGoal));
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        SavingsGoalContributionDTO withdrawal = SavingsGoalContributionDTO.builder()
                .amount(new BigDecimal("200.00"))
                .build();

        SavingsGoalResponseDTO response = savingsGoalService.withdrawFromGoal(10L, withdrawal);

        assertNotNull(response);
        assertEquals(new BigDecimal("400.00"), response.getCurrentAmount());
        // Verify account refunded: 5000 + 200 = 5200
        assertEquals(new BigDecimal("5200.00"), testAccount.getBalance());
    }
}
