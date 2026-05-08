package com.microbiz.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
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
    private volatile LocalDateTime lastRefreshAt;

    @PostConstruct
    public void initializeRates() {
        loadFallbackRates();
        refreshRates();
        lastRefreshAt = LocalDateTime.now();
    }

    @Scheduled(cron = "${microbiz.currency.refresh-cron:0 0 */6 * * *}")
    public void refreshRates() {
        loadFallbackRates();

        Map<String, Object> response = fetchRatesWithRetry(resolveApiUrl(), 3);
        if (response != null) {
            mergeApiRates(response);
        }
        lastRefreshAt = LocalDateTime.now();
    }

    public void refreshRatesNow() {
        refreshRates();
    }

    public double toBase(double amount, String currency) {
        String c = normalizeCurrency(currency);
        if (c.equals(baseCurrency.toUpperCase(Locale.ROOT))) {
            return amount;
        }
        double rate = ratesToBase.getOrDefault(c, 1.0);
        return amount * rate;
    }

    public double fromBase(double amount, String targetCurrency) {
        String c = normalizeCurrency(targetCurrency);
        if (c.equals(baseCurrency.toUpperCase(Locale.ROOT))) {
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
        return SUPPORTED_CURRENCIES.contains(currency.toUpperCase(Locale.ROOT));
    }

    public String normalizeCurrency(String currency) {
        String normalized = (currency == null || currency.isBlank())
                ? baseCurrency
                : currency.toUpperCase(Locale.ROOT);
        return isSupported(normalized) ? normalized : baseCurrency.toUpperCase(Locale.ROOT);
    }

    public Map<String, Double> getRatesToBase() {
        return Map.copyOf(ratesToBase);
    }

    public LocalDateTime getLastRefreshAt() {
        return lastRefreshAt;
    }

    public void updateRate(String currency, double rateToBase) {
        if (currency == null || currency.isBlank() || rateToBase <= 0) return;
        ratesToBase.put(currency.toUpperCase(Locale.ROOT), rateToBase);
    }

    private void loadFallbackRates() {
        ratesToBase.put("XAF", 1.0);
        ratesToBase.put("CFA", 1.0);
        ratesToBase.put(baseCurrency.toUpperCase(Locale.ROOT), 1.0);
        ratesToBase.put("USD", usdToBase);
        ratesToBase.put("EUR", eurToBase);
        ratesToBase.put("GNF", gnfToBase);
    }

    private String resolveApiUrl() {
        if (currencyApiUrl == null || currencyApiUrl.isBlank()) {
            return null;
        }
        return currencyApiUrl.replace("{base}", baseCurrency.toUpperCase(Locale.ROOT));
    }

    private void mergeApiRates(Map<String, Object> response) {
        Object rawRates = response.get("conversion_rates");
        if (!(rawRates instanceof Map<?, ?>)) {
            rawRates = response.get("rates");
        }
        if (!(rawRates instanceof Map<?, ?> rates)) {
            return;
        }

        for (String code : SUPPORTED_CURRENCIES) {
            Object raw = rates.get(code);
            if (raw instanceof Number n && n.doubleValue() > 0) {
                ratesToBase.put(code, 1.0 / n.doubleValue());
            }
        }
        ratesToBase.put(baseCurrency.toUpperCase(Locale.ROOT), 1.0);
    }

    private Map<String, Object> fetchRatesWithRetry(String url, int maxAttempts) {
        if (url == null || url.isBlank()) {
            return null;
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(2000);
        RestTemplate rt = new RestTemplate(requestFactory);
        int attempts = Math.max(1, maxAttempts);
        for (int i = 1; i <= attempts; i++) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = rt.getForObject(url, Map.class);
                if (response != null) {
                    return response;
                }
            } catch (Exception ignored) {
                // Keep configured fallback rates if the external provider is unavailable.
            }
        }
        return null;
    }
}
