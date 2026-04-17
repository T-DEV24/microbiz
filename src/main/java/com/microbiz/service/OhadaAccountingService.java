package com.microbiz.service;

import com.microbiz.model.Depense;
import com.microbiz.model.Vente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OhadaAccountingService {

    @Autowired private VenteService venteService;
    @Autowired private DepenseService depenseService;
    @Autowired private CurrencyRateService currencyRateService;

    public List<Map<String, Object>> genererJournal(LocalDate debut, LocalDate fin) {
        LocalDate from = debut != null ? debut : LocalDate.now().withDayOfMonth(1);
        LocalDate to = fin != null ? fin : LocalDate.now();
        List<Map<String, Object>> entries = new ArrayList<>();

        for (Vente vente : venteService.getVentesParPeriode(from, to)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", vente.getDateVente());
            row.put("journal", "VENTES");
            row.put("compte", "701000");
            row.put("libelle", "Vente " + (vente.getProduit() != null ? vente.getProduit().getNom() : ""));
            row.put("debit", 0.0);
            row.put("credit", currencyRateService.toBase(vente.getMontantTotal(), vente.getDevise()));
            entries.add(row);
        }

        for (Depense depense : depenseService.getDepensesParPeriode(from, to)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", depense.getDateDepense());
            row.put("journal", "ACHATS/CHARGES");
            row.put("compte", mapCompte(depense.getCategorie()));
            row.put("libelle", depense.getDescription());
            row.put("debit", currencyRateService.toBase(depense.getMontant() != null ? depense.getMontant() : 0.0, depense.getDevise()));
            row.put("credit", 0.0);
            entries.add(row);
        }

        entries.sort(Comparator.comparing(e -> (LocalDate) e.get("date")));
        return entries;
    }

    private String mapCompte(String categorie) {
        if (categorie == null) return "600000";
        return switch (categorie.toLowerCase()) {
            case "matieres premieres" -> "601000";
            case "salaires" -> "661000";
            case "loyer" -> "622000";
            case "transport" -> "624000";
            case "electricite" -> "606100";
            case "eau" -> "606300";
            case "internet", "telecom", "télécom" -> "626000";
            case "marketing", "publicite", "publicité" -> "623000";
            case "maintenance", "entretien" -> "612200";
            case "impots", "impôts", "taxes" -> "646000";
            case "assurance" -> "616000";
            case "frais bancaires" -> "627000";
            default -> "600000";
        };
    }
}
