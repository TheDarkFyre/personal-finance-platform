package com.finance.app.service;

import com.finance.app.dto.CashFlowSummaryDTO;
import com.finance.app.dto.CategorySummaryDTO;
import com.finance.app.dto.MonthlySummaryDTO;
import com.finance.app.entity.CategoryType;
import com.finance.app.repository.AccountRepository;
import com.finance.app.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<CategorySummaryDTO> getSpendingByCategory(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        return transactionRepository.getCategorySummaryByTypeAndDateRange(CategoryType.EXPENSE, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<CategorySummaryDTO> getIncomeByCategory(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        return transactionRepository.getCategorySummaryByTypeAndDateRange(CategoryType.INCOME, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public MonthlySummaryDTO getMonthlySummary(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        BigDecimal totalIncome = transactionRepository.getTotalAmountByTypeAndDateRange(CategoryType.INCOME, startDate, endDate);
        BigDecimal totalExpenses = transactionRepository.getTotalAmountByTypeAndDateRange(CategoryType.EXPENSE, startDate, endDate);

        BigDecimal netSavings = totalIncome.subtract(totalExpenses);
        BigDecimal savingsRate = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netSavings.multiply(BigDecimal.valueOf(100)).divide(totalIncome, 2, RoundingMode.HALF_UP);
        }

        return MonthlySummaryDTO.builder()
                .year(year)
                .month(month)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netSavings(netSavings)
                .savingsRatePercent(savingsRate)
                .build();
    }

    @Transactional(readOnly = true)
    public CashFlowSummaryDTO getCashFlowSummary(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        BigDecimal totalBalance = accountRepository.getTotalNetWorth();
        BigDecimal totalIncome = transactionRepository.getTotalAmountByTypeAndDateRange(CategoryType.INCOME, startDate, endDate);
        BigDecimal totalExpenses = transactionRepository.getTotalAmountByTypeAndDateRange(CategoryType.EXPENSE, startDate, endDate);
        BigDecimal netCashFlow = totalIncome.subtract(totalExpenses);

        List<CategorySummaryDTO> expenses = transactionRepository.getCategorySummaryByTypeAndDateRange(CategoryType.EXPENSE, startDate, endDate);
        List<CategorySummaryDTO> income = transactionRepository.getCategorySummaryByTypeAndDateRange(CategoryType.INCOME, startDate, endDate);

        return CashFlowSummaryDTO.builder()
                .totalBalance(totalBalance)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netCashFlow(netCashFlow)
                .expenseBreakdown(expenses)
                .incomeBreakdown(income)
                .build();
    }
}
