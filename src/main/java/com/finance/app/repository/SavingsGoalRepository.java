package com.finance.app.repository;

import com.finance.app.entity.GoalStatus;
import com.finance.app.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByStatus(GoalStatus status);

    List<SavingsGoal> findByUserId(Long userId);
}
