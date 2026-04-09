package com.microbiz.service;

import com.microbiz.model.Depense;
import com.microbiz.security.TenantContext;
import com.microbiz.repository.DepenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class DepenseService {

    @Autowired private DepenseRepository depenseRepository;
    @Autowired private CurrencyRateService currencyRateService;

    public List<Depense> findAll() {
        String tenant = TenantContext.getTenant();
        return depenseRepository.findAllByOrderByDateDepenseDesc().stream()
                .filter(d -> tenant.equals(d.getTenantKey()))
                .toList();
    }

    public Optional<Depense> findById(Long id) { return depenseRepository.findById(id); }

    public Depense save(Depense d) {
        if (d.getDevise() == null || d.getDevise().isBlank()) {
            d.setDevise("XAF");
        }
        if (d.getTenantKey() == null || d.getTenantKey().isBlank()) {
            d.setTenantKey(TenantContext.getTenant());
        }
        return depenseRepository.save(d);
    }

    public void deleteById(Long id) { depenseRepository.deleteById(id); }

    public List<Depense> getDepensesParPeriode(LocalDate debut, LocalDate fin) {
        String tenant = TenantContext.getTenant();
        return depenseRepository.findByDateDepenseBetweenOrderByDateDepenseDesc(debut, fin).stream()
                .filter(d -> tenant.equals(d.getTenantKey()))
                .toList();
    }

    public Double getTotalParPeriode(LocalDate debut, LocalDate fin) {
        return depenseRepository.findByDateDepenseBetweenOrderByDateDepenseDesc(debut, fin).stream()
                .mapToDouble(d -> currencyRateService.toBase(d.getMontant() != null ? d.getMontant() : 0.0, d.getDevise()))
                .sum();
    }

    public Double getTotalDepenses() {
        return findAll().stream()
                .mapToDouble(d -> currencyRateService.toBase(d.getMontant() != null ? d.getMontant() : 0.0, d.getDevise()))
                .sum();
    }

    public Double getDepensesDuMois() {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        return findAll().stream()
                .filter(d -> d.getDateDepense() != null && d.getDateDepense().getMonthValue() == month && d.getDateDepense().getYear() == year)
                .mapToDouble(d -> currencyRateService.toBase(d.getMontant() != null ? d.getMontant() : 0.0, d.getDevise()))
                .sum();
    }


    public Map<String, Double> getDepensesParCategorie(LocalDate debut, LocalDate fin) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Depense depense : depenseRepository.findByDateDepenseBetweenOrderByDateDepenseDesc(debut, fin)) {
            String categorie = depense.getCategorie() == null || depense.getCategorie().isBlank()
                    ? "Autres"
                    : depense.getCategorie();
            double montant = currencyRateService.toBase(depense.getMontant() == null ? 0.0 : depense.getMontant(), depense.getDevise());
            result.put(categorie, result.getOrDefault(categorie, 0.0) + montant);
        }
        return result;
    }

    public Map<String, Double> getDepensesParCategorie() {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Object[] row : depenseRepository.getDepensesParCategorie()) {
            result.put((String) row[0], ((Number) row[1]).doubleValue());
        }
        return result;
    }
}
