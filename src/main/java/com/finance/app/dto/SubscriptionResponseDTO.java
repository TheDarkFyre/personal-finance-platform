package com.finance.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.finance.app.entity.BillingFrequency;
import com.finance.app.entity.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponseDTO {
    private Long id;
    private String name;
    private BigDecimal amount;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private Long accountId;
    private String accountName;
    private BillingFrequency frequency;
    private LocalDate nextDueDate;
    private SubscriptionStatus status;
    private long daysUntilDue;

    @JsonProperty("isOverdue")
    private boolean isOverdue;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
