package com.finance.app.service;

import com.finance.app.dto.CurrencyConversionDTO;
import com.finance.app.dto.ExchangeRateResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyConversionServiceTest {

    private CurrencyConversionService currencyConversionService;

    @BeforeEach
    void setUp() {
        currencyConversionService = new CurrencyConversionService(new RestTemplateBuilder());
    }

    @Test
    @DisplayName("Should return exchange rates for base USD")
    void shouldGetExchangeRates() {
        ExchangeRateResponseDTO rates = currencyConversionService.getExchangeRates("USD");

        assertNotNull(rates);
        assertNotNull(rates.getRates());
        assertTrue(rates.getRates().containsKey("USD"));
        assertEquals(BigDecimal.ONE, rates.getRates().get("USD"));
    }

    @Test
    @DisplayName("Should convert amount correctly between currencies")
    void shouldConvertCurrency() {
        BigDecimal amount = new BigDecimal("100.00");
        CurrencyConversionDTO result = currencyConversionService.convertCurrency("USD", "EUR", amount);

        assertNotNull(result);
        assertEquals("USD", result.getFrom());
        assertEquals("EUR", result.getTo());
        assertEquals(amount, result.getOriginalAmount());
        assertNotNull(result.getRate());
        assertNotNull(result.getConvertedAmount());
        assertTrue(result.getConvertedAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should return identical amount when converting same currency")
    void shouldReturnSameAmountForIdenticalCurrency() {
        BigDecimal amount = new BigDecimal("250.00");
        CurrencyConversionDTO result = currencyConversionService.convertCurrency("USD", "USD", amount);

        assertNotNull(result);
        assertEquals(amount, result.getConvertedAmount());
        assertEquals(BigDecimal.ONE, result.getRate());
    }
}
