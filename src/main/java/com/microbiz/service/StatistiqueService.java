package com.microbiz.service;

import com.microbiz.repository.VenteRepository;
import com.microbiz.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class StatistiqueService {

    @Autowired private VenteService venteService;
    @Autowired private VenteRepository venteRepository;
    @Autowired private DepenseService depenseService;
    @Autowired private CurrencyRateService currencyRateService;

    public Double getChiffreAffairesTotal() {
        String tenant = TenantContext.getTenant();
        return venteRepository.sumByDevise(tenant).stream()
                .mapToDouble(row -> currencyRateService.toBase(((Number) row[1]).doubleValue(), (String) row[0]))
                .sum();
    }

    public Double getChiffreAffairesDuMois() {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        return venteService.getVentesParPeriode(
                        LocalDate.of(year, month, 1),
                        LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth())
                ).stream()
                .mapToDouble(v -> currencyRateService.toBase(v.getMontantTotal(), v.getDevise()))
                .sum();
    }

    public Double getChiffreAffairesParPeriode(LocalDate debut, LocalDate fin) {
        return venteService.getVentesParPeriode(debut, fin).stream()
                .mapToDouble(v -> currencyRateService.toBase(v.getMontantTotal(), v.getDevise()))
                .sum();
    }

    public Double getBeneficeNet() {
        return getChiffreAffairesTotal() - getTotalDepenses();
    }

    public Double getTotalDepenses() {
        return depenseService.getTotalDepenses();
    }

    public Double getTotalDepensesParPeriode(LocalDate debut, LocalDate fin) {
        return depenseService.getTotalParPeriode(debut, fin);
    }

    public Double getMargeBeneficiaire() {
        double ca = getChiffreAffairesTotal();
        return ca > 0 ? (getBeneficeNet() / ca) * 100 : 0.0;
    }

    /** AMÉLIORATION 1 : Evolution MENSUELLE */
    public Map<String, Double> getEvolutionMensuelle() {
        Map<String, Double> result = new LinkedHashMap<>();
        String[] mois = {"Jan","Fev","Mar","Avr","Mai","Jun","Jul","Aou","Sep","Oct","Nov","Dec"};
        for (Object[] row : venteRepository.sumByMonthAndDevise(TenantContext.getTenant(), null, null)) {
            int y = ((Number) row[0]).intValue();
            int m = ((Number) row[1]).intValue();
            String devise = (String) row[2];
            double montant = ((Number) row[3]).doubleValue();
            String key = mois[m - 1] + " " + y;
            double caBase = currencyRateService.toBase(montant, devise);
            result.put(key, result.getOrDefault(key, 0.0) + caBase);
        }
        return result;
    }

    /** AMÉLIORATION 1 : Evolution HEBDOMADAIRE — n dernières semaines */
    public Map<String, Double> getEvolutionHebdomadaire(int nbSemaines) {
        LocalDate depuis = LocalDate.now().minusWeeks(nbSemaines);
        WeekFields wf = WeekFields.ISO;
        Map<String, Double> result = new LinkedHashMap<>();
        for (var vente : venteService.getVentesParPeriode(depuis, LocalDate.now())) {
            if (vente.getDateVente() == null || vente.getDateVente().isBefore(depuis)) continue;
            int sem = vente.getDateVente().get(wf.weekOfWeekBasedYear());
            int annee = vente.getDateVente().getYear();
            String key = "S" + sem + "/" + annee;
            double caBase = currencyRateService.toBase(vente.getMontantTotal(), vente.getDevise());
            result.put(key, result.getOrDefault(key, 0.0) + caBase);
        }
        return result;
    }

    /** Évolution SEMESTRIELLE (S1/S2) à partir des données mensuelles */
    public Map<String, Double> getEvolutionSemestrielle() {
        Map<String, Double> result = new LinkedHashMap<>();
        for (var vente : venteService.findAll()) {
            if (vente.getDateVente() == null) continue;
            int mois = vente.getDateVente().getMonthValue();
            int annee = vente.getDateVente().getYear();
            double ca = currencyRateService.toBase(vente.getMontantTotal(), vente.getDevise());
            String semestre = mois <= 6 ? "S1 " + annee : "S2 " + annee;
            result.put(semestre, result.getOrDefault(semestre, 0.0) + ca);
        }
        return result;
    }

    public Map<String, Double> getEvolutionParFiltre(String periode, LocalDate debut, LocalDate fin) {
        Map<String, Double> result = new LinkedHashMap<>();
        WeekFields wf = WeekFields.ISO;
        for (var vente : venteService.getVentesParPeriode(debut, fin)) {
            if (vente.getDateVente() == null) continue;
            String key;
            LocalDate d = vente.getDateVente();
            if ("semaine".equals(periode)) {
                key = "S" + d.get(wf.weekOfWeekBasedYear()) + "/" + d.getYear();
            } else if ("semestre".equals(periode)) {
                key = (d.getMonthValue() <= 6 ? "S1 " : "S2 ") + d.getYear();
            } else {
                String[] mois = {"Jan","Fev","Mar","Avr","Mai","Jun","Jul","Aou","Sep","Oct","Nov","Dec"};
                key = mois[d.getMonthValue() - 1] + " " + d.getYear();
            }
            result.put(key, result.getOrDefault(key, 0.0) + currencyRateService.toBase(vente.getMontantTotal(), vente.getDevise()));
        }
        return result;
    }

    public double calculerVariationPourcentage(double courant, double precedent) {
        if (precedent == 0) return courant == 0 ? 0 : 100;
        return ((courant - precedent) / precedent) * 100.0;
    }
}
