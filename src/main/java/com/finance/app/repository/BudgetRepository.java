package com.finance.app.repository;

import com.finance.app.entity.Budget;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @EntityGraph(attributePaths = {"category"})
    List<Budget> findByMonthAndYear(int month, int year);

    Optional<Budget> findByCategoryIdAndMonthAndYear(Long categoryId, int month, int year);

    boolean existsByCategoryIdAndMonthAndYear(Long categoryId, int month, int year);

    List<Budget> findByUserIdAndMonthAndYear(Long userId, int month, int year);
}
