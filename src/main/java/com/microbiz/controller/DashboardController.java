package com.microbiz.controller;

import com.microbiz.service.*;
import com.microbiz.security.PmeAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class DashboardController {

    @Autowired private StatistiqueService statistiqueService;
    @Autowired private VenteService       venteService;
    @Autowired private ProduitService     produitService;
    @Autowired private DepenseService     depenseService;
    @Autowired private CurrencyRateService currencyRateService;
    @Autowired private PredictiveSalesService predictiveSalesService;
    @Autowired private PmeAccess pmeAccess;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(
            @RequestParam(defaultValue = "mois") String periode,
            Model model,
            Authentication authentication) {

        boolean accesFinance = pmeAccess.canAccessFinance(authentication);

        // Métriques financières
        model.addAttribute("ca",       statistiqueService.getChiffreAffairesTotal());
        model.addAttribute("accesFinance", accesFinance);
        model.addAttribute("depenses", accesFinance ? depenseService.getTotalDepenses() : 0.0);
        model.addAttribute("benefice", accesFinance ? statistiqueService.getBeneficeNet() : 0.0);
        model.addAttribute("marge",    accesFinance ? statistiqueService.getMargeBeneficiaire() : 0.0);
        model.addAttribute("caJour",   venteService.getCADuJour());
        model.addAttribute("nbTransactions", venteService.getNbTransactionsDuJour());
        model.addAttribute("devisePrincipale", currencyRateService.getBaseCurrency());

        // AMÉLIORATION 1 : évolution selon la période choisie
        if ("semaine".equals(periode)) {
            model.addAttribute("evolutionCA", statistiqueService.getEvolutionHebdomadaire(8));
        } else if ("semestre".equals(periode)) {
            model.addAttribute("evolutionCA", statistiqueService.getEvolutionSemestrielle());
        } else {
            model.addAttribute("evolutionCA", statistiqueService.getEvolutionMensuelle());
        }
        model.addAttribute("periode", periode);

        // Données complémentaires
        model.addAttribute("depensesCategories", accesFinance ? depenseService.getDepensesParCategorie() : Map.of());
        model.addAttribute("topProduits",        venteService.getTopProduits(5));
        model.addAttribute("ventesRecentes",     venteService.getVentesRecentes());
        model.addAttribute("stockBas",           produitService.getProduitsStockBas());
        model.addAttribute("previsionsVentes",   predictiveSalesService.previsionMensuelle(3));

        return "dashboard";
    }

    @GetMapping("/api/kpis")
    @ResponseBody
    public Map<String, Object> kpis(Authentication authentication) {
        boolean accesFinance = pmeAccess.canAccessFinance(authentication);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ca", statistiqueService.getChiffreAffairesTotal());
        if (accesFinance) {
            result.put("depenses", depenseService.getTotalDepenses());
            result.put("benefice", statistiqueService.getBeneficeNet());
            result.put("marge", statistiqueService.getMargeBeneficiaire());
        }
        result.put("caJour", venteService.getCADuJour());
        result.put("nbTransactions", venteService.getNbTransactionsDuJour());
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
