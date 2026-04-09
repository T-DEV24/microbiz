package com.microbiz.service;

import com.microbiz.model.Vente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PredictiveSalesService {

    @Autowired private VenteService venteService;
    @Autowired private CurrencyRateService currencyRateService;

    public Map<String, Double> previsionMensuelle(int monthsAhead) {
        List<Vente> ventes = venteService.findAll();
        Map<String, Double> monthly = new LinkedHashMap<>();
        for (Vente v : ventes) {
            if (v.getDateVente() == null) continue;
            String key = v.getDateVente().getYear() + "-" + String.format("%02d", v.getDateVente().getMonthValue());
            double amount = currencyRateService.toBase(v.getMontantTotal(), v.getDevise());
            monthly.put(key, monthly.getOrDefault(key, 0.0) + amount);
        }

        double avg = monthly.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double trend = computeTrend(monthly);
        Map<String, Double> forecast = new LinkedHashMap<>();
        LocalDate cursor = LocalDate.now().withDayOfMonth(1);
        for (int i = 1; i <= monthsAhead; i++) {
            cursor = cursor.plusMonths(1);
            double value = Math.max(0.0, avg + (trend * i));
            forecast.put(cursor.getYear() + "-" + String.format("%02d", cursor.getMonthValue()), value);
        }
        return forecast;
    }

    private double computeTrend(Map<String, Double> monthly) {
        if (monthly.size() < 2) return 0.0;
        double first = monthly.values().stream().findFirst().orElse(0.0);
        double last = monthly.values().stream().reduce((a, b) -> b).orElse(first);
        return (last - first) / (monthly.size() - 1);
    }
}
