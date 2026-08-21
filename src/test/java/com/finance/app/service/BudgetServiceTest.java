package com.finance.app.service;

import com.finance.app.dto.BudgetProgressDTO;
import com.finance.app.dto.BudgetRequestDTO;
import com.finance.app.dto.BudgetResponseDTO;
import com.finance.app.entity.Budget;
import com.finance.app.entity.Category;
import com.finance.app.entity.CategoryType;
import com.finance.app.repository.BudgetRepository;
import com.finance.app.repository.CategoryRepository;
import com.finance.app.repository.TransactionRepository;
import com.finance.app.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetService budgetService;

    private Category testCategory;
    private Budget testBudget;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Food & Dining")
                .type(CategoryType.EXPENSE)
                .icon("🍔")
                .color("#F87171")
                .build();

        testBudget = Budget.builder()
                .id(10L)
                .category(testCategory)
                .limitAmount(new BigDecimal("500.00"))
                .month(8)
                .year(2026)
                .build();
    }

    @Test
    @DisplayName("Should create new monthly category budget")
    void shouldCreateBudget() {
        BudgetRequestDTO request = BudgetRequestDTO.builder()
                .categoryId(1L)
                .limitAmount(new BigDecimal("500.00"))
                .month(8)
                .year(2026)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.findByCategoryIdAndMonthAndYear(1L, 8, 2026)).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget b = invocation.getArgument(0);
            b.setId(10L);
            return b;
        });

        BudgetResponseDTO response = budgetService.createOrUpdateBudget(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Food & Dining", response.getCategoryName());
        assertEquals(new BigDecimal("500.00"), response.getLimitAmount());
        verify(budgetRepository, times(1)).save(any(Budget.class));
    }

    @Test
    @DisplayName("Should calculate budget progress accurately when UNDER limit")
    void shouldCalculateBudgetProgressUnderLimit() {
        when(budgetRepository.findByMonthAndYear(8, 2026)).thenReturn(List.of(testBudget));
        // Mock $200 spent out of $500 limit via grouped batch query
        List<Object[]> batchResult = List.<Object[]>of(new Object[]{1L, new BigDecimal("200.00")});
        when(transactionRepository.getTotalSpentGroupedByCategoryId(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(batchResult);

        List<BudgetProgressDTO> progressList = budgetService.getBudgetProgressForMonth(2026, 8);

        assertNotNull(progressList);
        assertEquals(1, progressList.size());
        BudgetProgressDTO progress = progressList.get(0);
        assertEquals(new BigDecimal("500.00"), progress.getLimitAmount());
        assertEquals(new BigDecimal("200.00"), progress.getSpentAmount());
        assertEquals(new BigDecimal("300.00"), progress.getRemainingAmount());
        assertEquals(new BigDecimal("40.00"), progress.getPercentageUsed());
        assertFalse(progress.isOverBudget());
    }

    @Test
    @DisplayName("Should calculate budget progress and trigger alert when OVER limit")
    void shouldCalculateBudgetProgressOverLimit() {
        when(budgetRepository.findByMonthAndYear(8, 2026)).thenReturn(List.of(testBudget));
        // Mock $600 spent out of $500 limit via grouped batch query
        List<Object[]> batchResult = List.<Object[]>of(new Object[]{1L, new BigDecimal("600.00")});
        when(transactionRepository.getTotalSpentGroupedByCategoryId(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(batchResult);

        List<BudgetProgressDTO> progressList = budgetService.getBudgetProgressForMonth(2026, 8);

        assertNotNull(progressList);
        assertEquals(1, progressList.size());
        BudgetProgressDTO progress = progressList.get(0);
        assertEquals(new BigDecimal("500.00"), progress.getLimitAmount());
        assertEquals(new BigDecimal("600.00"), progress.getSpentAmount());
        assertEquals(new BigDecimal("-100.00"), progress.getRemainingAmount());
        assertEquals(new BigDecimal("120.00"), progress.getPercentageUsed());
        assertTrue(progress.isOverBudget());
    }

    @Test
    @DisplayName("Should delete budget successfully")
    void shouldDeleteBudget() {
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(testBudget));

        budgetService.deleteBudget(10L);

        verify(budgetRepository, times(1)).delete(testBudget);
    }
}
