package com.finance.app.service;

import com.finance.app.dto.BudgetProgressDTO;
import com.finance.app.dto.BudgetRequestDTO;
import com.finance.app.dto.BudgetResponseDTO;
import com.finance.app.entity.Budget;
import com.finance.app.entity.Category;
import com.finance.app.entity.User;
import com.finance.app.exception.ResourceNotFoundException;
import com.finance.app.repository.BudgetRepository;
import com.finance.app.repository.CategoryRepository;
import com.finance.app.repository.TransactionRepository;
import com.finance.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<BudgetResponseDTO> getBudgetsForMonth(int year, int month) {
        return budgetRepository.findByMonthAndYear(month, year).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BudgetProgressDTO> getBudgetProgressForMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<Budget> budgets = budgetRepository.findByMonthAndYear(month, year);

        return budgets.stream().map(budget -> {
            BigDecimal spent = transactionRepository.getTotalSpentByCategoryIdAndDateRange(
                    budget.getCategory().getId(), startDate, endDate);

            BigDecimal limit = budget.getLimitAmount();
            BigDecimal remaining = limit.subtract(spent);
            boolean isOver = spent.compareTo(limit) > 0;

            BigDecimal percentageUsed = BigDecimal.ZERO;
            if (limit.compareTo(BigDecimal.ZERO) > 0) {
                percentageUsed = spent.multiply(BigDecimal.valueOf(100))
                        .divide(limit, 2, RoundingMode.HALF_UP);
            }

            return BudgetProgressDTO.builder()
                    .budgetId(budget.getId())
                    .categoryId(budget.getCategory().getId())
                    .categoryName(budget.getCategory().getName())
                    .categoryIcon(budget.getCategory().getIcon())
                    .categoryColor(budget.getCategory().getColor())
                    .limitAmount(limit)
                    .spentAmount(spent)
                    .remainingAmount(remaining)
                    .percentageUsed(percentageUsed)
                    .isOverBudget(isOver)
                    .month(month)
                    .year(year)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public BudgetResponseDTO createOrUpdateBudget(BudgetRequestDTO dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + dto.getCategoryId()));

        User user = null;
        if (dto.getUserId() != null) {
            user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + dto.getUserId()));
        }

        Optional<Budget> existing = budgetRepository.findByCategoryIdAndMonthAndYear(
                dto.getCategoryId(), dto.getMonth(), dto.getYear());

        Budget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setLimitAmount(dto.getLimitAmount());
        } else {
            budget = Budget.builder()
                    .category(category)
                    .limitAmount(dto.getLimitAmount())
                    .month(dto.getMonth())
                    .year(dto.getYear())
                    .user(user)
                    .build();
        }

        Budget saved = budgetRepository.save(budget);
        return mapToResponseDTO(saved);
    }

    @Transactional
    public void deleteBudget(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with ID: " + id));
        budgetRepository.delete(budget);
    }

    public BudgetResponseDTO mapToResponseDTO(Budget budget) {
        return BudgetResponseDTO.builder()
                .id(budget.getId())
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getName())
                .categoryIcon(budget.getCategory().getIcon())
                .categoryColor(budget.getCategory().getColor())
                .limitAmount(budget.getLimitAmount())
                .month(budget.getMonth())
                .year(budget.getYear())
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }
}
