package com.finance.app.service;

import com.finance.app.dto.CurrencyConversionDTO;
import com.finance.app.dto.ExchangeRateResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("Should triangulate cross-currency conversions correctly using fallback rates when external API is unreachable")
    void shouldTriangulateCrossCurrencyFallback() {
        org.springframework.web.client.RestTemplate mockRestTemplate = mock(org.springframework.web.client.RestTemplate.class);
        when(mockRestTemplate.getForObject(any(java.net.URI.class), eq(java.util.Map.class)))
                .thenThrow(new org.springframework.web.client.RestClientException("Simulated Network Error"));

        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.setConnectTimeout(any())).thenReturn(builder);
        when(builder.setReadTimeout(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mockRestTemplate);

        CurrencyConversionService offlineService = new CurrencyConversionService(builder);

        BigDecimal amount = new BigDecimal("100.00");
        CurrencyConversionDTO result = offlineService.convertCurrency("EUR", "GBP", amount);

        assertNotNull(result);
        assertEquals("EUR", result.getFrom());
        assertEquals("GBP", result.getTo());
        // EUR in USD is 0.92, GBP in USD is 0.78 => 0.78 / 0.92 ~ 0.8478 rate => 84.78 GBP
        assertEquals(new BigDecimal("84.78"), result.getConvertedAmount());
        assertEquals(new BigDecimal("0.8478"), result.getRate());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when amount is null")
    void shouldThrowExceptionWhenAmountIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                currencyConversionService.convertCurrency("USD", "EUR", null));
    }
}
