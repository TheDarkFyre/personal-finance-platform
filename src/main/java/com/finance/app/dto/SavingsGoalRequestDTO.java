package com.finance.app.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoalRequestDTO {

    @NotBlank(message = "Goal name is required")
    @Size(max = 100, message = "Goal name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Target amount is required")
    @Positive(message = "Target amount must be greater than zero")
    private BigDecimal targetAmount;

    @PositiveOrZero(message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit;

    @FutureOrPresent(message = "Target date must be today or in the future")
    private LocalDate targetDate;

    @Positive(message = "Account ID must be greater than zero")
    private Long accountId;

    @Size(max = 50, message = "Icon cannot exceed 50 characters")
    private String icon;

    @Size(max = 20, message = "Color cannot exceed 20 characters")
    private String color;

    @Positive(message = "User ID must be greater than zero")
    private Long userId;
}
