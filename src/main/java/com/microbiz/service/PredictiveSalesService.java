package com.microbiz.service;

import com.microbiz.model.Vente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PredictiveSalesService {

    @Autowired private VenteService venteService;
    @Autowired private CurrencyRateService currencyRateService;

    public Map<String, Double> previsionMensuelle(int monthsAhead) {
        List<Vente> ventes = venteService.findAll();
        Map<YearMonth, Double> monthly = new LinkedHashMap<>();
        for (Vente v : ventes) {
            if (v.getDateVente() == null) continue;
            YearMonth key = YearMonth.from(v.getDateVente());
            double amount = currencyRateService.toBase(v.getMontantTotal(), v.getDevise());
            monthly.put(key, monthly.getOrDefault(key, 0.0) + amount);
        }

        if (monthly.isEmpty()) {
            return Map.of();
        }

        List<Map.Entry<YearMonth, Double>> ordered = new ArrayList<>(monthly.entrySet());
        ordered.sort(Comparator.comparing(Map.Entry::getKey));
        double[] holt = computeHoltLinear(ordered, 0.45, 0.20);

        int horizon = Math.min(Math.max(monthsAhead, 1), 3);
        Map<String, Double> forecast = new LinkedHashMap<>();
        LocalDate cursor = LocalDate.now().withDayOfMonth(1);
        for (int i = 1; i <= horizon; i++) {
            cursor = cursor.plusMonths(1);
            double value = Math.max(0.0, holt[0] + (holt[1] * i));
            forecast.put(cursor.getYear() + "-" + String.format("%02d", cursor.getMonthValue()), value);
        }
        return forecast;
    }

    private double[] computeHoltLinear(List<Map.Entry<YearMonth, Double>> ordered, double alpha, double beta) {
        double level = ordered.get(0).getValue();
        double trend = 0.0;
        if (ordered.size() > 1) {
            trend = ordered.get(1).getValue() - ordered.get(0).getValue();
        }
        for (int i = 1; i < ordered.size(); i++) {
            double value = ordered.get(i).getValue();
            double previousLevel = level;
            level = alpha * value + (1 - alpha) * (level + trend);
            trend = beta * (level - previousLevel) + (1 - beta) * trend;
        }
        return new double[]{level, trend};
    }
}
