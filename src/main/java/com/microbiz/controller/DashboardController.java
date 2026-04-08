package com.microbiz.controller;

import com.microbiz.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    @Autowired private StatistiqueService statistiqueService;
    @Autowired private VenteService       venteService;
    @Autowired private ProduitService     produitService;
    @Autowired private DepenseService     depenseService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(
            @RequestParam(defaultValue = "mois") String periode,
            Model model) {

        // Métriques financières
        model.addAttribute("ca",       statistiqueService.getChiffreAffairesTotal());
        model.addAttribute("depenses", depenseService.getTotalDepenses());
        model.addAttribute("benefice", statistiqueService.getBeneficeNet());
        model.addAttribute("marge",    statistiqueService.getMargeBeneficiaire());
        model.addAttribute("caJour",   venteService.getCADuJour());
        model.addAttribute("nbTransactions", venteService.getNbTransactionsDuJour());

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
        model.addAttribute("depensesCategories", depenseService.getDepensesParCategorie());
        model.addAttribute("topProduits",        venteService.getTopProduits(5));
        model.addAttribute("ventesRecentes",     venteService.getVentesRecentes());
        model.addAttribute("stockBas",           produitService.getProduitsStockBas());

        return "dashboard";
    }
}
