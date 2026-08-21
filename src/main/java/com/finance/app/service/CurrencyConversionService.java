package com.finance.app.service;

import com.finance.app.dto.CurrencyConversionDTO;
import com.finance.app.dto.ExchangeRateResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CurrencyConversionService {

    private record CachedRate(ExchangeRateResponseDTO dto, Instant expiresAt) {}

    private final RestTemplate restTemplate;
    private final Map<String, CachedRate> cache = new ConcurrentHashMap<>();
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");

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
        String base = baseCurrency != null ? baseCurrency.trim().toUpperCase() : "USD";
        if (!CURRENCY_CODE_PATTERN.matcher(base).matches()) {
            base = "USD";
        }

        CachedRate cached = cache.get(base);
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
            return cached.dto();
        }

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl("https://api.frankfurter.app/latest")
                    .queryParam("from", base)
                    .build()
                    .toUri();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);

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

                cache.put(base, new CachedRate(dto, Instant.now().plus(CACHE_TTL)));
                return dto;
            }
        } catch (Exception e) {
            log.warn("Unable to fetch live currency rates from Frankfurter API: {}. Falling back to default rates.", e.getMessage());
        }

        // Generate triangulated fallback rates relative to requested base
        Map<String, BigDecimal> triangulatedRates = new HashMap<>();
        BigDecimal baseInUsd = FALLBACK_USD_RATES.getOrDefault(base, BigDecimal.ONE);

        FALLBACK_USD_RATES.forEach((currency, rateInUsd) -> {
            BigDecimal triangulatedRate = rateInUsd.divide(baseInUsd, 4, RoundingMode.HALF_UP);
            triangulatedRates.put(currency, triangulatedRate);
        });

        ExchangeRateResponseDTO fallback = ExchangeRateResponseDTO.builder()
                .base(base)
                .date(LocalDate.now())
                .rates(triangulatedRates)
                .build();

        return fallback;
    }

    public CurrencyConversionDTO convertCurrency(String from, String to, BigDecimal amount) {
        String baseFrom = from != null ? from.trim().toUpperCase() : "USD";
        String baseTo = to != null ? to.trim().toUpperCase() : "USD";

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
        BigDecimal rate = ratesDto.getRates() != null ? ratesDto.getRates().get(baseTo) : null;

        if (rate == null) {
            // Fallback estimation using USD base triangulation
            BigDecimal fromInUsd = FALLBACK_USD_RATES.getOrDefault(baseFrom, BigDecimal.ONE);
            BigDecimal toInUsd = FALLBACK_USD_RATES.getOrDefault(baseTo, BigDecimal.ONE);
            rate = toInUsd.divide(fromInUsd, 6, RoundingMode.HALF_UP);
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
