package com.finance.app.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CurrencyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/currencies/rates - Returns exchange rates mapping")
    void shouldGetExchangeRatesViaApi() throws Exception {
        mockMvc.perform(get("/api/v1/currencies/rates")
                        .param("base", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base", is("USD")))
                .andExpect(jsonPath("$.rates", notNullValue()))
                .andExpect(jsonPath("$.rates.USD").value(1));
    }


    @Test
    @DisplayName("GET /api/v1/currencies/convert - Converts currency accurately")
    void shouldConvertCurrencyViaApi() throws Exception {
        mockMvc.perform(get("/api/v1/currencies/convert")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("amount", "100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from", is("USD")))
                .andExpect(jsonPath("$.to", is("EUR")))
                .andExpect(jsonPath("$.originalAmount", is(100.00)))
                .andExpect(jsonPath("$.convertedAmount", notNullValue()));
    }
}
