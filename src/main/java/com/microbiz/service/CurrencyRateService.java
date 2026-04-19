package com.microbiz.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CurrencyRateService {

    @Value("${microbiz.currency.base:XAF}")
    private String baseCurrency;
    @Value("${microbiz.currency.usd-to-base:600}")
    private double usdToBase;
    @Value("${microbiz.currency.eur-to-base:655.957}")
    private double eurToBase;
    @Value("${microbiz.currency.gnf-to-base:0.07}")
    private double gnfToBase;
    @Value("${microbiz.currency.api-url:}")
    private String currencyApiUrl;

    private final Map<String, Double> ratesToBase = new ConcurrentHashMap<>();
    private static final List<String> SUPPORTED_CURRENCIES = List.of("XAF", "EUR", "USD", "GNF");

    @PostConstruct
    public void initializeRates() {
        ratesToBase.put("XAF", 1.0);
        ratesToBase.put("CFA", 1.0);
        ratesToBase.put("USD", usdToBase);
        ratesToBase.put("EUR", eurToBase);
        ratesToBase.put("GNF", gnfToBase);
    }

    @Scheduled(cron = "${microbiz.currency.refresh-cron:0 0 */6 * * *}")
    public void refreshRates() {
        ratesToBase.put(baseCurrency.toUpperCase(), 1.0);
        ratesToBase.put("USD", usdToBase);
        ratesToBase.put("EUR", eurToBase);
        ratesToBase.put("GNF", gnfToBase);

        if (currencyApiUrl == null || currencyApiUrl.isBlank()) {
            return;
        }
        Map<String, Object> rates = fetchRatesWithRetry(3);
        if (rates != null) {
            mergeApiRate(rates, "USD");
            mergeApiRate(rates, "EUR");
            mergeApiRate(rates, "GNF");
            mergeApiRate(rates, "XAF");
        }
    }

    public double toBase(double amount, String currency) {
        String c = normalizeCurrency(currency);
        if (c.equals(baseCurrency.toUpperCase())) {
            return amount;
        }
        double rate = ratesToBase.getOrDefault(c, 1.0);
        return amount * rate;
    }

    public double fromBase(double amount, String targetCurrency) {
        String c = normalizeCurrency(targetCurrency);
        if (c.equals(baseCurrency.toUpperCase())) {
            return amount;
        }
        double rate = ratesToBase.getOrDefault(c, 1.0);
        return rate <= 0 ? amount : amount / rate;
    }

    public double convert(double amount, String fromCurrency, String toCurrency) {
        double baseAmount = toBase(amount, fromCurrency);
        return fromBase(baseAmount, toCurrency);
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public List<String> getSupportedCurrencies() {
        return SUPPORTED_CURRENCIES;
    }

    public boolean isSupported(String currency) {
        if (currency == null || currency.isBlank()) return false;
        return SUPPORTED_CURRENCIES.contains(currency.toUpperCase());
    }

    public String normalizeCurrency(String currency) {
        String normalized = (currency == null || currency.isBlank())
                ? baseCurrency
                : currency.toUpperCase();
        return isSupported(normalized) ? normalized : baseCurrency.toUpperCase();
    }

    public Map<String, Double> getRatesToBase() {
        return Map.copyOf(ratesToBase);
    }

    public void updateRate(String currency, double rateToBase) {
        if (currency == null || currency.isBlank() || rateToBase <= 0) return;
        ratesToBase.put(currency.toUpperCase(), rateToBase);
    }

    private void mergeApiRate(Map<String, Object> rates, String code) {
        Object raw = rates.get(code);
        if (raw instanceof Number n) {
            ratesToBase.put(code, n.doubleValue());
        }
    }

    private Map<String, Object> fetchRatesWithRetry(int maxAttempts) {
        RestTemplate rt = new RestTemplate();
        int attempts = Math.max(1, maxAttempts);
        for (int i = 1; i <= attempts; i++) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = rt.getForObject(currencyApiUrl, Map.class);
                if (response != null && response.get("rates") instanceof Map<?, ?> rawRates) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rates = (Map<String, Object>) rawRates;
                    return rates;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
