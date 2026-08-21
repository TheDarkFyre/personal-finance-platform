package com.finance.app.controller;

import com.finance.app.dto.CurrencyConversionDTO;
import com.finance.app.dto.ExchangeRateResponseDTO;
import com.finance.app.service.CurrencyConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Validated
@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
@Tag(name = "Currencies & Exchange Rates", description = "Live multi-currency exchange rates and conversions")
public class CurrencyController {

    private final CurrencyConversionService currencyConversionService;

    @GetMapping("/rates")
    @Operation(summary = "Get current exchange rates for a base currency (default: USD)")
    public ResponseEntity<ExchangeRateResponseDTO> getExchangeRates(
            @Parameter(description = "Base currency symbol, e.g. USD, EUR, GBP")
            @RequestParam(defaultValue = "USD") String base) {
        return ResponseEntity.ok(currencyConversionService.getExchangeRates(base));
    }

    @GetMapping("/convert")
    @Operation(summary = "Convert an amount between two currencies using real-time rates")
    public ResponseEntity<CurrencyConversionDTO> convertCurrency(
            @Parameter(description = "Source currency, e.g. USD") @RequestParam(defaultValue = "USD") String from,
            @Parameter(description = "Target currency, e.g. EUR") @RequestParam(defaultValue = "EUR") String to,
            @Parameter(description = "Amount to convert") @RequestParam @Positive(message = "Amount must be greater than zero") BigDecimal amount) {
        return ResponseEntity.ok(currencyConversionService.convertCurrency(from, to, amount));
    }
}
