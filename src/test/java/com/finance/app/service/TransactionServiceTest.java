package com.finance.app.service;

import com.finance.app.dto.TransactionRequestDTO;
import com.finance.app.dto.TransactionResponseDTO;
import com.finance.app.entity.Account;
import com.finance.app.entity.AccountType;
import com.finance.app.entity.Category;
import com.finance.app.entity.CategoryType;
import com.finance.app.entity.Transaction;
import com.finance.app.exception.ResourceNotFoundException;
import com.finance.app.repository.AccountRepository;
import com.finance.app.repository.CategoryRepository;
import com.finance.app.repository.TransactionRepository;
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
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account testAccount;
    private Category expenseCategory;
    private Category incomeCategory;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .name("Checking Account")
                .type(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .build();

        expenseCategory = Category.builder()
                .id(10L)
                .name("Food & Dining")
                .type(CategoryType.EXPENSE)
                .build();

        incomeCategory = Category.builder()
                .id(20L)
                .name("Salary")
                .type(CategoryType.INCOME)
                .build();
    }

    @Test
    @DisplayName("Should create expense transaction and deduct account balance atomically")
    void shouldCreateExpenseTransactionAndDeductBalance() {
        TransactionRequestDTO dto = TransactionRequestDTO.builder()
                .accountId(1L)
                .categoryId(10L)
                .amount(new BigDecimal("150.00"))
                .transactionDate(LocalDate.now())
                .description("Groceries")
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(expenseCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(100L);
            return t;
        });

        TransactionResponseDTO response = transactionService.createTransaction(dto);

        assertNotNull(response);
        assertEquals(new BigDecimal("150.00"), response.getAmount());
        assertEquals("Food & Dining", response.getCategoryName());
        assertEquals("EXPENSE", response.getCategoryType());
        // Verify balance was deducted from 1000.00 to 850.00
        assertEquals(new BigDecimal("850.00"), testAccount.getBalance());

        verify(accountRepository, times(1)).save(testAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should create income transaction and increment account balance")
    void shouldCreateIncomeTransactionAndIncrementBalance() {
        TransactionRequestDTO dto = TransactionRequestDTO.builder()
                .accountId(1L)
                .categoryId(20L)
                .amount(new BigDecimal("2500.00"))
                .transactionDate(LocalDate.now())
                .description("Biweekly Paycheck")
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(incomeCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(101L);
            return t;
        });

        TransactionResponseDTO response = transactionService.createTransaction(dto);

        assertNotNull(response);
        assertEquals(new BigDecimal("2500.00"), response.getAmount());
        assertEquals("Salary", response.getCategoryName());
        // Verify balance was incremented from 1000.00 to 3500.00
        assertEquals(new BigDecimal("3500.00"), testAccount.getBalance());

        verify(accountRepository, times(1)).save(testAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when account does not exist")
    void shouldThrowExceptionWhenAccountNotFound() {
        TransactionRequestDTO dto = TransactionRequestDTO.builder()
                .accountId(999L)
                .categoryId(10L)
                .amount(new BigDecimal("50.00"))
                .transactionDate(LocalDate.now())
                .build();

        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.createTransaction(dto));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should delete transaction and revert account balance")
    void shouldDeleteTransactionAndRevertBalance() {
        Transaction tx = Transaction.builder()
                .id(50L)
                .account(testAccount)
                .category(expenseCategory)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));

        transactionService.deleteTransaction(50L);

        // Reverting 100.00 expense from 1000.00 should result in 1100.00
        assertEquals(new BigDecimal("1100.00"), testAccount.getBalance());
        verify(accountRepository, times(1)).save(testAccount);
        verify(transactionRepository, times(1)).delete(tx);
    }
}
