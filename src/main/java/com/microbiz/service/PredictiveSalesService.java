package com.microbiz.service;

import com.microbiz.repository.VenteRepository;
import com.microbiz.security.TenantContext;
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

    @Autowired private VenteRepository venteRepository;
    @Autowired private CurrencyRateService currencyRateService;

    public Map<String, Double> previsionMensuelle(int monthsAhead) {
        Map<YearMonth, Double> monthly = loadMonthlySales();
        if (monthly.isEmpty()) {
            return Map.of();
        }

        List<Map.Entry<YearMonth, Double>> ordered = new ArrayList<>(monthly.entrySet());
        ordered.sort(Comparator.comparing(Map.Entry::getKey));
        double[] holt = computeHoltLinear(ordered, 0.45, 0.20);
        Map<Integer, Double> seasonalIndex = computeSeasonalIndex(ordered);

        int horizon = Math.min(Math.max(monthsAhead, 1), 6);
        Map<String, Double> forecast = new LinkedHashMap<>();
        LocalDate cursor = LocalDate.now().withDayOfMonth(1);
        for (int i = 1; i <= horizon; i++) {
            cursor = cursor.plusMonths(1);
            double trendForecast = Math.max(0.0, holt[0] + (holt[1] * i));
            double seasonalFactor = seasonalIndex.getOrDefault(cursor.getMonthValue(), 1.0);
            forecast.put(cursor.getYear() + "-" + String.format("%02d", cursor.getMonthValue()),
                    Math.max(0.0, trendForecast * seasonalFactor));
        }
        return forecast;
    }

    private Map<YearMonth, Double> loadMonthlySales() {
        Map<YearMonth, Double> monthly = new LinkedHashMap<>();
        for (Object[] row : venteRepository.sumByMonthAndDevise(TenantContext.getTenant(), null, null)) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            String devise = (String) row[2];
            double amount = ((Number) row[3]).doubleValue();
            YearMonth key = YearMonth.of(year, month);
            monthly.put(key, monthly.getOrDefault(key, 0.0) + currencyRateService.toBase(amount, devise));
        }
        return monthly;
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

    private Map<Integer, Double> computeSeasonalIndex(List<Map.Entry<YearMonth, Double>> ordered) {
        Map<Integer, Double> index = new LinkedHashMap<>();
        if (ordered.size() < 12) {
            return index;
        }

        double globalAverage = ordered.stream()
                .mapToDouble(Map.Entry::getValue)
                .average()
                .orElse(0.0);
        if (globalAverage <= 0) {
            return index;
        }

        Map<Integer, List<Double>> valuesByMonth = new LinkedHashMap<>();
        for (Map.Entry<YearMonth, Double> entry : ordered) {
            valuesByMonth.computeIfAbsent(entry.getKey().getMonthValue(), m -> new ArrayList<>())
                    .add(entry.getValue());
        }
        valuesByMonth.forEach((month, values) -> {
            double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(globalAverage);
            index.put(month, Math.max(0.25, Math.min(4.0, average / globalAverage)));
        });
        return index;
    }
}
