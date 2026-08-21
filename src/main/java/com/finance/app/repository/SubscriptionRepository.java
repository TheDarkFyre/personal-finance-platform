package com.finance.app.repository;

import com.finance.app.entity.Subscription;
import com.finance.app.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByStatus(SubscriptionStatus status);

    List<Subscription> findByStatusOrderByNextDueDateAsc(SubscriptionStatus status);

    List<Subscription> findByNextDueDateBetweenAndStatus(LocalDate start, LocalDate end, SubscriptionStatus status);

    List<Subscription> findByUserId(Long userId);
}
