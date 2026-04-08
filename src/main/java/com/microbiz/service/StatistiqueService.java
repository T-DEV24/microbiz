package com.microbiz.service;

import com.microbiz.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class StatistiqueService {

    @Autowired private VenteRepository   venteRepository;
    @Autowired private DepenseRepository depenseRepository;

    public Double getChiffreAffairesTotal() {
        Double ca = venteRepository.calculerCATotal();
        return ca != null ? ca : 0.0;
    }

    public Double getChiffreAffairesDuMois() {
        Double ca = venteRepository.calculerCADuMois(
                LocalDate.now().getMonthValue(), LocalDate.now().getYear());
        return ca != null ? ca : 0.0;
    }

    public Double getBeneficeNet() {
        return getChiffreAffairesTotal() - getTotalDepenses();
    }

    public Double getTotalDepenses() {
        Double d = depenseRepository.calculerTotal();
        return d != null ? d : 0.0;
    }

    public Double getMargeBeneficiaire() {
        double ca = getChiffreAffairesTotal();
        return ca > 0 ? (getBeneficeNet() / ca) * 100 : 0.0;
    }

    /** AMÉLIORATION 1 : Evolution MENSUELLE */
    public Map<String, Double> getEvolutionMensuelle() {
        String[] mois = {"Jan","Fev","Mar","Avr","Mai","Jun",
                "Jul","Aou","Sep","Oct","Nov","Dec"};
        Map<String, Double> result = new LinkedHashMap<>();
        for (Object[] row : venteRepository.getEvolutionMensuelle()) {
            int m  = ((Number) row[0]).intValue();
            int y  = ((Number) row[1]).intValue();
            double ca = ((Number) row[2]).doubleValue();
            result.put(mois[m - 1] + " " + y, ca);
        }
        return result;
    }

    /** AMÉLIORATION 1 : Evolution HEBDOMADAIRE — n dernières semaines */
    public Map<String, Double> getEvolutionHebdomadaire(int nbSemaines) {
        LocalDate depuis = LocalDate.now().minusWeeks(nbSemaines);
        Map<String, Double> result = new LinkedHashMap<>();
        for (Object[] row : venteRepository.getEvolutionHebdomadaire(depuis)) {
            int sem   = ((Number) row[0]).intValue();
            int annee = ((Number) row[1]).intValue();
            double ca = ((Number) row[2]).doubleValue();
            result.put("S" + sem + "/" + annee, ca);
        }
        return result;
    }

    /** Évolution SEMESTRIELLE (S1/S2) à partir des données mensuelles */
    public Map<String, Double> getEvolutionSemestrielle() {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Object[] row : venteRepository.getEvolutionMensuelle()) {
            int mois = ((Number) row[0]).intValue();
            int annee = ((Number) row[1]).intValue();
            double ca = ((Number) row[2]).doubleValue();

            String semestre = mois <= 6 ? "S1 " + annee : "S2 " + annee;
            result.put(semestre, result.getOrDefault(semestre, 0.0) + ca);
        }
        return result;
    }
}
