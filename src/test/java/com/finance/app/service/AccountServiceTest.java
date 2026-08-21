package com.finance.app.service;

import com.finance.app.dto.AccountRequestDTO;
import com.finance.app.dto.AccountResponseDTO;
import com.finance.app.entity.Account;
import com.finance.app.entity.AccountType;
import com.finance.app.exception.ResourceNotFoundException;
import com.finance.app.repository.AccountRepository;
import com.finance.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .name("Savings Account")
                .type(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .currency("USD")
                .build();
    }

    @Test
    @DisplayName("Should retrieve all accounts")
    void shouldGetAllAccounts() {
        when(accountRepository.findAll()).thenReturn(List.of(testAccount));

        List<AccountResponseDTO> accounts = accountService.getAllAccounts();

        assertNotNull(accounts);
        assertEquals(1, accounts.size());
        assertEquals("Savings Account", accounts.get(0).getName());
        assertEquals(new BigDecimal("5000.00"), accounts.get(0).getBalance());
    }

    @Test
    @DisplayName("Should create a new account successfully")
    void shouldCreateAccount() {
        AccountRequestDTO request = AccountRequestDTO.builder()
                .name("New Checking")
                .type(AccountType.CHECKING)
                .initialBalance(new BigDecimal("1500.00"))
                .currency("USD")
                .build();

        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account a = invocation.getArgument(0);
            a.setId(2L);
            return a;
        });

        AccountResponseDTO response = accountService.createAccount(request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("New Checking", response.getName());
        assertEquals(new BigDecimal("1500.00"), response.getBalance());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when account ID not found")
    void shouldThrowExceptionWhenAccountNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.getAccountById(99L));
    }
}
