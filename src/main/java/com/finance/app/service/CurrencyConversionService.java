package com.finance.app.service;

import com.finance.app.dto.CurrencyConversionDTO;
import com.finance.app.dto.ExchangeRateResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CurrencyConversionService {

    private final RestTemplate restTemplate;
    private final Map<String, ExchangeRateResponseDTO> cache = new ConcurrentHashMap<>();

    private static final Map<String, BigDecimal> FALLBACK_USD_RATES = Map.of(
            "USD", new BigDecimal("1.00"),
            "EUR", new BigDecimal("0.92"),
            "GBP", new BigDecimal("0.78"),
            "CAD", new BigDecimal("1.36"),
            "JPY", new BigDecimal("154.50"),
            "INR", new BigDecimal("83.50"),
            "AUD", new BigDecimal("1.52"),
            "CHF", new BigDecimal("0.90")
    );

    public CurrencyConversionService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }

    public ExchangeRateResponseDTO getExchangeRates(String baseCurrency) {
        String base = baseCurrency != null ? baseCurrency.toUpperCase() : "USD";

        if (cache.containsKey(base)) {
            return cache.get(base);
        }

        try {
            String url = "https://api.frankfurter.app/latest?from=" + base;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("rates")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rawRates = (Map<String, Object>) response.get("rates");
                Map<String, BigDecimal> rates = new HashMap<>();
                rates.put(base, BigDecimal.ONE);

                rawRates.forEach((k, v) -> {
                    if (v instanceof Number num) {
                        rates.put(k, BigDecimal.valueOf(num.doubleValue()).setScale(4, RoundingMode.HALF_UP));
                    }
                });

                ExchangeRateResponseDTO dto = ExchangeRateResponseDTO.builder()
                        .base(base)
                        .date(LocalDate.now())
                        .rates(rates)
                        .build();

                cache.put(base, dto);
                return dto;
            }
        } catch (Exception e) {
            log.warn("Unable to fetch live currency rates from Frankfurter API: {}. Falling back to default rates.", e.getMessage());
        }

        // Return fallback rates
        ExchangeRateResponseDTO fallback = ExchangeRateResponseDTO.builder()
                .base("USD")
                .date(LocalDate.now())
                .rates(FALLBACK_USD_RATES)
                .build();

        return fallback;
    }

    public CurrencyConversionDTO convertCurrency(String from, String to, BigDecimal amount) {
        String baseFrom = from.toUpperCase();
        String baseTo = to.toUpperCase();

        if (baseFrom.equals(baseTo)) {
            return CurrencyConversionDTO.builder()
                    .from(baseFrom)
                    .to(baseTo)
                    .originalAmount(amount)
                    .rate(BigDecimal.ONE)
                    .convertedAmount(amount)
                    .build();
        }

        ExchangeRateResponseDTO ratesDto = getExchangeRates(baseFrom);
        BigDecimal rate = ratesDto.getRates().get(baseTo);

        if (rate == null) {
            // Fallback estimation using USD base
            ExchangeRateResponseDTO usdRates = getExchangeRates("USD");
            BigDecimal fromRateInUsd = usdRates.getRates().getOrDefault(baseFrom, BigDecimal.ONE);
            BigDecimal toRateInUsd = usdRates.getRates().getOrDefault(baseTo, BigDecimal.ONE);
            rate = toRateInUsd.divide(fromRateInUsd, 6, RoundingMode.HALF_UP);
        }

        BigDecimal convertedAmount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        return CurrencyConversionDTO.builder()
                .from(baseFrom)
                .to(baseTo)
                .originalAmount(amount)
                .rate(rate.setScale(4, RoundingMode.HALF_UP))
                .convertedAmount(convertedAmount)
                .build();
    }
}
