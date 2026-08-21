package com.finance.app.dto;

import com.finance.app.entity.BillingFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class SubscriptionRequestDTO {

    @NotBlank(message = "Subscription name is required")
    @Size(max = 100, message = "Subscription name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotNull(message = "Billing frequency is required")
    private BillingFrequency frequency;

    @NotNull(message = "Next due date is required")
    private LocalDate nextDueDate;

    private Long userId;
}
