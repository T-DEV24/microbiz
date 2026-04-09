package com.microbiz.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyRateServiceTest {

    private CurrencyRateService service;

    @BeforeEach
    void setUp() {
        service = new CurrencyRateService();
        ReflectionTestUtils.setField(service, "baseCurrency", "XAF");
        ReflectionTestUtils.setField(service, "usdToBase", 600.0);
        ReflectionTestUtils.setField(service, "eurToBase", 655.957);
        ReflectionTestUtils.setField(service, "gnfToBase", 0.07);
        ReflectionTestUtils.setField(service, "currencyApiUrl", "");
        service.refreshRates();
    }

    @Test
    void shouldConvertToAndFromBase() {
        double usdAmount = 10.0;
        double inBase = service.toBase(usdAmount, "USD");
        assertEquals(6000.0, inBase, 0.0001);

        double backToUsd = service.fromBase(inBase, "USD");
        assertEquals(usdAmount, backToUsd, 0.0001);
    }

    @Test
    void shouldConvertBetweenCurrencies() {
        double eurAmount = 10.0;
        double convertedToUsd = service.convert(eurAmount, "EUR", "USD");
        assertTrue(convertedToUsd > 10.0);
    }

    @Test
    void shouldNormalizeUnsupportedCurrencyToBase() {
        assertEquals("XAF", service.normalizeCurrency("abc"));
        assertEquals("XAF", service.normalizeCurrency(""));
        assertEquals("USD", service.normalizeCurrency("usd"));
    }
}
