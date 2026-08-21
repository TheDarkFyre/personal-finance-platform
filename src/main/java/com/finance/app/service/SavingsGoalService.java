package com.finance.app.service;

import com.finance.app.dto.SavingsGoalContributionDTO;
import com.finance.app.dto.SavingsGoalRequestDTO;
import com.finance.app.dto.SavingsGoalResponseDTO;
import com.finance.app.entity.Account;
import com.finance.app.entity.GoalStatus;
import com.finance.app.entity.SavingsGoal;
import com.finance.app.entity.User;
import com.finance.app.exception.ResourceNotFoundException;
import com.finance.app.repository.AccountRepository;
import com.finance.app.repository.SavingsGoalRepository;
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
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SavingsGoalResponseDTO> getAllGoals(GoalStatus status) {
        List<SavingsGoal> goals;
        if (status != null) {
            goals = savingsGoalRepository.findByStatus(status);
        } else {
            goals = savingsGoalRepository.findAll();
        }
        return goals.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SavingsGoalResponseDTO getGoalById(Long id) {
        return mapToResponseDTO(findGoalEntityById(id));
    }

    @Transactional
    public SavingsGoalResponseDTO createGoal(SavingsGoalRequestDTO dto) {
        Account account = null;
        if (dto.getAccountId() != null) {
            account = accountRepository.findById(dto.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + dto.getAccountId()));
        }

        User user = null;
        if (dto.getUserId() != null) {
            user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + dto.getUserId()));
        }

        BigDecimal initialDeposit = dto.getInitialDeposit() != null ? dto.getInitialDeposit() : BigDecimal.ZERO;
        if (initialDeposit.compareTo(BigDecimal.ZERO) > 0 && account != null) {
            account.setBalance(account.getBalance().subtract(initialDeposit));
            accountRepository.save(account);
        }

        GoalStatus status = initialDeposit.compareTo(dto.getTargetAmount()) >= 0 ? GoalStatus.COMPLETED : GoalStatus.IN_PROGRESS;

        SavingsGoal goal = SavingsGoal.builder()
                .name(dto.getName())
                .targetAmount(dto.getTargetAmount())
                .currentAmount(initialDeposit)
                .targetDate(dto.getTargetDate())
                .account(account)
                .icon(dto.getIcon() != null ? dto.getIcon() : "🎯")
                .color(dto.getColor() != null ? dto.getColor() : "#10B981")
                .status(status)
                .user(user)
                .build();

        SavingsGoal saved = savingsGoalRepository.save(goal);
        return mapToResponseDTO(saved);
    }

    @Transactional
    public SavingsGoalResponseDTO depositToGoal(Long goalId, SavingsGoalContributionDTO dto) {
        SavingsGoal goal = findGoalEntityById(goalId);

        // Adjust linked account if present
        if (goal.getAccount() != null) {
            Account account = goal.getAccount();
            account.setBalance(account.getBalance().subtract(dto.getAmount()));
            accountRepository.save(account);
        }

        BigDecimal newAmount = goal.getCurrentAmount().add(dto.getAmount());
        goal.setCurrentAmount(newAmount);

        if (newAmount.compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
        }

        SavingsGoal updated = savingsGoalRepository.save(goal);
        return mapToResponseDTO(updated);
    }

    @Transactional
    public SavingsGoalResponseDTO withdrawFromGoal(Long goalId, SavingsGoalContributionDTO dto) {
        SavingsGoal goal = findGoalEntityById(goalId);

        if (dto.getAmount().compareTo(goal.getCurrentAmount()) > 0) {
            throw new IllegalArgumentException("Withdrawal amount exceeds current goal savings balance ($" + goal.getCurrentAmount() + ")");
        }

        // Refund linked account if present
        if (goal.getAccount() != null) {
            Account account = goal.getAccount();
            account.setBalance(account.getBalance().add(dto.getAmount()));
            accountRepository.save(account);
        }

        BigDecimal newAmount = goal.getCurrentAmount().subtract(dto.getAmount());
        goal.setCurrentAmount(newAmount);

        if (newAmount.compareTo(goal.getTargetAmount()) < 0 && goal.getStatus() == GoalStatus.COMPLETED) {
            goal.setStatus(GoalStatus.IN_PROGRESS);
        }

        SavingsGoal updated = savingsGoalRepository.save(goal);
        return mapToResponseDTO(updated);
    }

    @Transactional
    public void deleteGoal(Long id) {
        SavingsGoal goal = findGoalEntityById(id);
        savingsGoalRepository.delete(goal);
    }

    public SavingsGoal findGoalEntityById(Long id) {
        return savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Goal not found with ID: " + id));
    }

    public SavingsGoalResponseDTO mapToResponseDTO(SavingsGoal goal) {
        BigDecimal target = goal.getTargetAmount();
        BigDecimal current = goal.getCurrentAmount();
        BigDecimal remaining = target.subtract(current).max(BigDecimal.ZERO);

        BigDecimal progress = BigDecimal.ZERO;
        if (target.compareTo(BigDecimal.ZERO) > 0) {
            progress = current.multiply(BigDecimal.valueOf(100)).divide(target, 2, RoundingMode.HALF_UP);
        }

        Long daysRemaining = null;
        if (goal.getTargetDate() != null) {
            daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate());
        }

        boolean isCompleted = current.compareTo(target) >= 0 || goal.getStatus() == GoalStatus.COMPLETED;

        return SavingsGoalResponseDTO.builder()
                .id(goal.getId())
                .name(goal.getName())
                .targetAmount(target)
                .currentAmount(current)
                .remainingAmount(remaining)
                .progressPercent(progress)
                .targetDate(goal.getTargetDate())
                .daysRemaining(daysRemaining)
                .icon(goal.getIcon())
                .color(goal.getColor())
                .status(goal.getStatus())
                .accountId(goal.getAccount() != null ? goal.getAccount().getId() : null)
                .accountName(goal.getAccount() != null ? goal.getAccount().getName() : "Unlinked")
                .isCompleted(isCompleted)
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }
}
