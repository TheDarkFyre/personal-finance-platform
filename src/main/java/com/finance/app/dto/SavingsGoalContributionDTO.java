package com.finance.app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoalContributionDTO {

    @NotNull(message = "Contribution amount is required")
    @Positive(message = "Contribution amount must be greater than zero")
    private BigDecimal amount;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;
}
