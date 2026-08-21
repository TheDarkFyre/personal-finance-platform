package com.finance.app.service;

import com.finance.app.dto.AccountRequestDTO;
import com.finance.app.dto.AccountResponseDTO;
import com.finance.app.entity.Account;
import com.finance.app.entity.User;
import com.finance.app.exception.ResourceNotFoundException;
import com.finance.app.repository.AccountRepository;
import com.finance.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CurrencyConversionService currencyConversionService;

    @Transactional(readOnly = true)
    public List<AccountResponseDTO> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountResponseDTO getAccountById(Long id) {
        Account account = findAccountEntityById(id);
        return mapToResponseDTO(account);
    }

    @Transactional(readOnly = true)
    public Account findAccountEntityById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + id));
    }

    @Transactional
    public AccountResponseDTO createAccount(AccountRequestDTO dto) {
        User user = null;
        if (dto.getUserId() != null) {
            user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + dto.getUserId()));
        }

        Account account = Account.builder()
                .name(dto.getName())
                .type(dto.getType())
                .balance(dto.getInitialBalance() != null ? dto.getInitialBalance() : BigDecimal.ZERO)
                .currency(dto.getCurrency() != null ? dto.getCurrency().toUpperCase() : "USD")
                .user(user)
                .build();

        Account saved = accountRepository.save(account);
        return mapToResponseDTO(saved);
    }

    @Transactional
    public AccountResponseDTO updateAccount(Long id, AccountRequestDTO dto) {
        Account account = findAccountEntityById(id);
        account.setName(dto.getName());
        account.setType(dto.getType());
        if (dto.getCurrency() != null) {
            account.setCurrency(dto.getCurrency().toUpperCase());
        }
        Account updated = accountRepository.save(account);
        return mapToResponseDTO(updated);
    }

    @Transactional
    public void deleteAccount(Long id) {
        Account account = findAccountEntityById(id);
        accountRepository.delete(account);
    }

    public BigDecimal getTotalNetWorth() {
        List<Account> accounts = accountRepository.findAll();
        BigDecimal totalUsd = BigDecimal.ZERO;

        for (Account account : accounts) {
            String curr = (account.getCurrency() != null && !account.getCurrency().isBlank())
                    ? account.getCurrency().trim().toUpperCase()
                    : "USD";
            BigDecimal balance = (account.getBalance() != null) ? account.getBalance() : BigDecimal.ZERO;

            if ("USD".equals(curr)) {
                totalUsd = totalUsd.add(balance);
            } else {
                BigDecimal converted = currencyConversionService.convertCurrency(curr, "USD", balance).getConvertedAmount();
                totalUsd = totalUsd.add(converted != null ? converted : balance);
            }
        }
        return totalUsd.setScale(2, RoundingMode.HALF_UP);
    }

    public AccountResponseDTO mapToResponseDTO(Account account) {
        boolean isOverdrawn = account.getType() != com.finance.app.entity.AccountType.CREDIT_CARD
                && account.getBalance() != null
                && account.getBalance().compareTo(BigDecimal.ZERO) < 0;

        return AccountResponseDTO.builder()
                .id(account.getId())
                .name(account.getName())
                .type(account.getType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .isOverdrawn(isOverdrawn)
                .userId(account.getUser() != null ? account.getUser().getId() : null)
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
