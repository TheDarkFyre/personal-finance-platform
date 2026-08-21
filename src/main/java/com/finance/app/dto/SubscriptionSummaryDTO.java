package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionSummaryDTO {
    private BigDecimal monthlyBurnRate;
    private int activeCount;
    private int dueSoonCount;
    @Builder.Default
    private List<SubscriptionResponseDTO> subscriptions = new ArrayList<>();
}
