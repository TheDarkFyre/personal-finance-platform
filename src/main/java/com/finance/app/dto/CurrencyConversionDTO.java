package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyConversionDTO {
    private String from;
    private String to;
    private BigDecimal originalAmount;
    private BigDecimal rate;
    private BigDecimal convertedAmount;
}
