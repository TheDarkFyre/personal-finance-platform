package com.finance.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.finance.app.entity.GoalStatus;
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
public class SavingsGoalResponseDTO {
    private Long id;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal progressPercent;
    private LocalDate targetDate;
    private Long daysRemaining;
    private String icon;
    private String color;
    private GoalStatus status;
    private Long accountId;
    private String accountName;

    @JsonProperty("isCompleted")
    private boolean isCompleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
