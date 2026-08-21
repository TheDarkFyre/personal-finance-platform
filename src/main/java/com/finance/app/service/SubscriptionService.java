package com.finance.app.service;

import com.finance.app.dto.SubscriptionRequestDTO;
import com.finance.app.dto.SubscriptionResponseDTO;
import com.finance.app.dto.SubscriptionSummaryDTO;
import com.finance.app.dto.TransactionRequestDTO;
import com.finance.app.dto.TransactionResponseDTO;
import com.finance.app.entity.*;
import com.finance.app.exception.ResourceNotFoundException;
import com.finance.app.repository.AccountRepository;
import com.finance.app.repository.CategoryRepository;
import com.finance.app.repository.SubscriptionRepository;
import com.finance.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    @Transactional(readOnly = true)
    public List<SubscriptionResponseDTO> getAllSubscriptions(SubscriptionStatus status) {
        List<Subscription> subs;
        if (status != null) {
            subs = subscriptionRepository.findByStatusOrderByNextDueDateAsc(status);
        } else {
            subs = subscriptionRepository.findAll();
        }
        return subs.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubscriptionResponseDTO getSubscriptionById(Long id) {
        return mapToResponseDTO(findSubscriptionEntityById(id));
    }

    @Transactional(readOnly = true)
    public SubscriptionSummaryDTO getSubscriptionSummary() {
        List<Subscription> activeSubs = subscriptionRepository.findByStatusOrderByNextDueDateAsc(SubscriptionStatus.ACTIVE);

        BigDecimal monthlyBurnRate = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        int dueSoonCount = 0;

        for (Subscription sub : activeSubs) {
            BigDecimal monthlyCost = calculateMonthlyEquivalent(sub.getAmount(), sub.getFrequency());
            monthlyBurnRate = monthlyBurnRate.add(monthlyCost);

            long daysUntil = ChronoUnit.DAYS.between(today, sub.getNextDueDate());
            if (daysUntil <= 7) {
                dueSoonCount++;
            }
        }

        List<SubscriptionResponseDTO> allDtos = subscriptionRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        return SubscriptionSummaryDTO.builder()
                .monthlyBurnRate(monthlyBurnRate.setScale(2, RoundingMode.HALF_UP))
                .activeCount(activeSubs.size())
                .dueSoonCount(dueSoonCount)
                .subscriptions(allDtos)
                .build();
    }

    @Transactional
    public SubscriptionResponseDTO createSubscription(SubscriptionRequestDTO dto) {
        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + dto.getAccountId()));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + dto.getCategoryId()));

        User user = null;
        if (dto.getUserId() != null) {
            user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + dto.getUserId()));
        }

        Subscription sub = Subscription.builder()
                .name(dto.getName())
                .amount(dto.getAmount())
                .category(category)
                .account(account)
                .frequency(dto.getFrequency())
                .nextDueDate(dto.getNextDueDate())
                .status(SubscriptionStatus.ACTIVE)
                .user(user)
                .build();

        Subscription saved = subscriptionRepository.save(sub);
        return mapToResponseDTO(saved);
    }

    @Transactional
    public SubscriptionResponseDTO updateSubscription(Long id, SubscriptionRequestDTO dto) {
        Subscription sub = findSubscriptionEntityById(id);
        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + dto.getAccountId()));
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + dto.getCategoryId()));

        sub.setName(dto.getName());
        sub.setAmount(dto.getAmount());
        sub.setCategory(category);
        sub.setAccount(account);
        sub.setFrequency(dto.getFrequency());
        sub.setNextDueDate(dto.getNextDueDate());

        Subscription updated = subscriptionRepository.save(sub);
        return mapToResponseDTO(updated);
    }

    @Transactional
    public SubscriptionResponseDTO updateStatus(Long id, SubscriptionStatus newStatus) {
        Subscription sub = findSubscriptionEntityById(id);
        sub.setStatus(newStatus);
        return mapToResponseDTO(subscriptionRepository.save(sub));
    }

    @Transactional
    public TransactionResponseDTO paySubscription(Long id) {
        Subscription sub = findSubscriptionEntityById(id);

        // 1. Record the payment transaction
        TransactionRequestDTO txRequest = TransactionRequestDTO.builder()
                .accountId(sub.getAccount().getId())
                .categoryId(sub.getCategory().getId())
                .amount(sub.getAmount())
                .transactionDate(LocalDate.now())
                .description("Recurring: " + sub.getName())
                .build();

        TransactionResponseDTO txResponse = transactionService.createTransaction(txRequest);

        // 2. Advance the next due date based on frequency
        LocalDate nextDate = advanceDueDate(sub.getNextDueDate(), sub.getFrequency());
        sub.setNextDueDate(nextDate);
        subscriptionRepository.save(sub);

        return txResponse;
    }

    @Transactional
    public void deleteSubscription(Long id) {
        Subscription sub = findSubscriptionEntityById(id);
        subscriptionRepository.delete(sub);
    }

    public Subscription findSubscriptionEntityById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with ID: " + id));
    }

    private BigDecimal calculateMonthlyEquivalent(BigDecimal amount, BillingFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> amount.multiply(BigDecimal.valueOf(52)).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
            case MONTHLY -> amount;
            case QUARTERLY -> amount.divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP);
            case ANNUAL -> amount.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        };
    }

    private LocalDate advanceDueDate(LocalDate currentDate, BillingFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> currentDate.plusWeeks(1);
            case MONTHLY -> currentDate.plusMonths(1);
            case QUARTERLY -> currentDate.plusMonths(3);
            case ANNUAL -> currentDate.plusYears(1);
        };
    }

    public SubscriptionResponseDTO mapToResponseDTO(Subscription sub) {
        LocalDate today = LocalDate.now();
        long daysUntil = ChronoUnit.DAYS.between(today, sub.getNextDueDate());
        boolean isOverdue = daysUntil < 0 && sub.getStatus() == SubscriptionStatus.ACTIVE;

        return SubscriptionResponseDTO.builder()
                .id(sub.getId())
                .name(sub.getName())
                .amount(sub.getAmount())
                .categoryId(sub.getCategory().getId())
                .categoryName(sub.getCategory().getName())
                .categoryIcon(sub.getCategory().getIcon())
                .categoryColor(sub.getCategory().getColor())
                .accountId(sub.getAccount().getId())
                .accountName(sub.getAccount().getName())
                .frequency(sub.getFrequency())
                .nextDueDate(sub.getNextDueDate())
                .status(sub.getStatus())
                .daysUntilDue(daysUntil)
                .isOverdue(isOverdue)
                .createdAt(sub.getCreatedAt())
                .updatedAt(sub.getUpdatedAt())
                .build();
    }
}
