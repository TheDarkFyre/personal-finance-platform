package com.finance.app.repository;

import com.finance.app.dto.CategorySummaryDTO;
import com.finance.app.entity.CategoryType;
import com.finance.app.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);

    Page<Transaction> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("SELECT new com.finance.app.dto.CategorySummaryDTO(t.category.name, SUM(t.amount)) " +
           "FROM Transaction t " +
           "WHERE t.category.type = :categoryType AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category.name " +
           "ORDER BY SUM(t.amount) DESC")
    List<CategorySummaryDTO> getCategorySummaryByTypeAndDateRange(
            @Param("categoryType") CategoryType categoryType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) " +
           "FROM Transaction t " +
           "WHERE t.category.type = :categoryType AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalAmountByTypeAndDateRange(
            @Param("categoryType") CategoryType categoryType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) " +
           "FROM Transaction t " +
           "WHERE t.category.id = :categoryId AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalSpentByCategoryIdAndDateRange(
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}

