package com.microbiz.controller;

import com.microbiz.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.time.LocalDate;

@Controller
@RequestMapping("/statistiques")
public class StatistiqueController {

    @Autowired private StatistiqueService statistiqueService;
    @Autowired private VenteService       venteService;
    @Autowired private DepenseService     depenseService;
    @Autowired private ProduitService     produitService;
    @Autowired private ClientService      clientService;
    @Autowired private RapportService     rapportService;

    @GetMapping
    public String statistiques(Model model) {
        model.addAttribute("caTotal",          statistiqueService.getChiffreAffairesTotal());
        model.addAttribute("caMois",           statistiqueService.getChiffreAffairesDuMois());
        model.addAttribute("benefice",         statistiqueService.getBeneficeNet());
        model.addAttribute("marge",            statistiqueService.getMargeBeneficiaire());
        model.addAttribute("depenses",         depenseService.getTotalDepenses());
        model.addAttribute("evolutionCA",      statistiqueService.getEvolutionMensuelle());
        model.addAttribute("depensesCategories", depenseService.getDepensesParCategorie());
        model.addAttribute("topProduits",      venteService.getTopProduits(5));
        model.addAttribute("nbVentes",         venteService.countAll());
        model.addAttribute("nbProduits",       produitService.countAll());
        model.addAttribute("nbClients",        clientService.countAll());
        return "statistiques";
    }

    // NOUVELLE FONCTIONNALITÉ 3 : Export PDF
    @GetMapping("/export-pdf")
    public void exportPdf(HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=rapport-microbiz-" + LocalDate.now() + ".pdf");
        byte[] pdf = rapportService.genererRapportPDF();
        response.getOutputStream().write(pdf);
        response.getOutputStream().flush();
    }
}
 