package com.finance.app.service;

import com.finance.app.dto.TransactionRequestDTO;
import com.finance.app.dto.TransactionResponseDTO;
import com.finance.app.entity.Account;
import com.finance.app.entity.Category;
import com.finance.app.entity.CategoryType;
import com.finance.app.entity.Transaction;
import com.finance.app.exception.ResourceNotFoundException;
import com.finance.app.repository.AccountRepository;
import com.finance.app.repository.CategoryRepository;
import com.finance.app.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> getTransactions(Long accountId, Long categoryId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Transaction> transactions;

        if (accountId != null) {
            transactions = transactionRepository.findByAccountId(accountId, pageable);
        } else if (categoryId != null) {
            transactions = transactionRepository.findByCategoryId(categoryId, pageable);
        } else if (startDate != null && endDate != null) {
            transactions = transactionRepository.findByTransactionDateBetween(startDate, endDate, pageable);
        } else {
            transactions = transactionRepository.findAll(pageable);
        }

        return transactions.map(this::mapToResponseDTO);
    }

    @Transactional(readOnly = true)
    public TransactionResponseDTO getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));
        return mapToResponseDTO(transaction);
    }

    @Transactional
    public TransactionResponseDTO createTransaction(TransactionRequestDTO dto) {
        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + dto.getAccountId()));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + dto.getCategoryId()));

        // Atomically adjust Account balance based on Category type
        if (category.getType() == CategoryType.EXPENSE) {
            account.setBalance(account.getBalance().subtract(dto.getAmount()));
        } else {
            account.setBalance(account.getBalance().add(dto.getAmount()));
        }
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .account(account)
                .category(category)
                .amount(dto.getAmount())
                .transactionDate(dto.getTransactionDate())
                .description(dto.getDescription())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return mapToResponseDTO(saved);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));

        Account account = transaction.getAccount();
        Category category = transaction.getCategory();

        // Revert the transaction effect on Account balance
        if (category.getType() == CategoryType.EXPENSE) {
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        } else {
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        }
        accountRepository.save(account);

        transactionRepository.delete(transaction);
    }

    public TransactionResponseDTO mapToResponseDTO(Transaction transaction) {
        return TransactionResponseDTO.builder()
                .id(transaction.getId())
                .accountId(transaction.getAccount().getId())
                .accountName(transaction.getAccount().getName())
                .categoryId(transaction.getCategory().getId())
                .categoryName(transaction.getCategory().getName())
                .categoryType(transaction.getCategory().getType().name())
                .amount(transaction.getAmount())
                .transactionDate(transaction.getTransactionDate())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
