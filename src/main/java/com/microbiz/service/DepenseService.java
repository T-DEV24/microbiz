package com.microbiz.service;

import com.microbiz.model.Depense;
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

    public List<Depense> findAll() {
        return depenseRepository.findAllByOrderByDateDepenseDesc();
    }

    public Optional<Depense> findById(Long id) { return depenseRepository.findById(id); }

    public Depense save(Depense d) { return depenseRepository.save(d); }

    public void deleteById(Long id) { depenseRepository.deleteById(id); }

    public List<Depense> getDepensesParPeriode(LocalDate debut, LocalDate fin) {
        return depenseRepository.findByDateDepenseBetweenOrderByDateDepenseDesc(debut, fin);
    }

    public Double getTotalParPeriode(LocalDate debut, LocalDate fin) {
        Double t = depenseRepository.calculerTotalParPeriode(debut, fin);
        return t != null ? t : 0.0;
    }

    public Double getTotalDepenses() {
        Double t = depenseRepository.calculerTotal();
        return t != null ? t : 0.0;
    }

    public Double getDepensesDuMois() {
        Double t = depenseRepository.calculerDepensesDuMois(
                LocalDate.now().getMonthValue(), LocalDate.now().getYear());
        return t != null ? t : 0.0;
    }


    public Map<String, Double> getDepensesParCategorie(LocalDate debut, LocalDate fin) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Depense depense : depenseRepository.findByDateDepenseBetweenOrderByDateDepenseDesc(debut, fin)) {
            String categorie = depense.getCategorie() == null || depense.getCategorie().isBlank()
                    ? "Autres"
                    : depense.getCategorie();
            double montant = depense.getMontant() == null ? 0.0 : depense.getMontant();
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