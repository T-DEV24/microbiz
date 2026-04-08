package com.microbiz.controller;

import com.microbiz.service.ClientService;
import com.microbiz.service.DepenseService;
import com.microbiz.service.ProduitService;
import com.microbiz.service.RapportService;
import com.microbiz.service.StatistiqueService;
import com.microbiz.service.VenteService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Controller
@RequestMapping("/statistiques")
public class StatistiqueController {

    @Autowired private StatistiqueService statistiqueService;
    @Autowired private VenteService venteService;
    @Autowired private DepenseService depenseService;
    @Autowired private ProduitService produitService;
    @Autowired private ClientService clientService;
    @Autowired private RapportService rapportService;

    @GetMapping
    public String statistiques(@RequestParam(defaultValue = "mois") String periode,
                               @RequestParam(defaultValue = "5") int top,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                               Model model) {

        int topN = Math.min(Math.max(top, 3), 10);
        if (debut != null && fin != null && debut.isAfter(fin)) {
            model.addAttribute("erreur", "La date de début doit être antérieure ou égale à la date de fin.");
            LocalDate tmp = debut;
            debut = fin;
            fin = tmp;
        }

        Double caTotal;
        Double depenses;
        Map<String, Double> evolution;
        Map<String, Double> depensesCategories;

        if (debut != null && fin != null) {
            caTotal = statistiqueService.getChiffreAffairesParPeriode(debut, fin);
            depenses = statistiqueService.getTotalDepensesParPeriode(debut, fin);
            evolution = statistiqueService.getEvolutionParFiltre(periode, debut, fin);
            depensesCategories = depenseService.getDepensesParCategorie(debut, fin);
        } else {
            caTotal = statistiqueService.getChiffreAffairesTotal();
            depenses = depenseService.getTotalDepenses();
            depensesCategories = depenseService.getDepensesParCategorie();
            if ("semaine".equals(periode)) {
                evolution = statistiqueService.getEvolutionHebdomadaire(8);
            } else if ("semestre".equals(periode)) {
                evolution = statistiqueService.getEvolutionSemestrielle();
            } else {
                evolution = statistiqueService.getEvolutionMensuelle();
            }
        }

        double benefice = caTotal - depenses;
        double marge = caTotal > 0 ? (benefice / caTotal) * 100.0 : 0.0;

        model.addAttribute("caTotal", caTotal);
        model.addAttribute("caMois", statistiqueService.getChiffreAffairesDuMois());
        model.addAttribute("benefice", benefice);
        model.addAttribute("marge", marge);
        model.addAttribute("depenses", depenses);
        model.addAttribute("evolutionCA", evolution);
        model.addAttribute("depensesCategories", depensesCategories);
        model.addAttribute("topProduits", venteService.getTopProduits(topN, debut, fin));
        model.addAttribute("nbVentes", venteService.countAll(debut, fin));
        model.addAttribute("nbProduits", produitService.countAll());
        model.addAttribute("nbClients", clientService.countAll());
        model.addAttribute("periode", periode);
        model.addAttribute("top", topN);
        model.addAttribute("debut", debut);
        model.addAttribute("fin", fin);

        if (debut != null && fin != null) {
            LocalDate previousEnd = debut.minusDays(1);
            long days = ChronoUnit.DAYS.between(debut, fin) + 1;
            LocalDate previousStart = previousEnd.minusDays(days - 1);

            double caPrev = statistiqueService.getChiffreAffairesParPeriode(previousStart, previousEnd);
            double depPrev = statistiqueService.getTotalDepensesParPeriode(previousStart, previousEnd);
            double benPrev = caPrev - depPrev;
            double margePrev = caPrev > 0 ? (benPrev / caPrev) * 100.0 : 0.0;

            model.addAttribute("variationCa", statistiqueService.calculerVariationPourcentage(caTotal, caPrev));
            model.addAttribute("variationDepenses", statistiqueService.calculerVariationPourcentage(depenses, depPrev));
            model.addAttribute("variationBenefice", statistiqueService.calculerVariationPourcentage(benefice, benPrev));
            model.addAttribute("variationMarge", statistiqueService.calculerVariationPourcentage(marge, margePrev));
            model.addAttribute("periodeComparaison", previousStart + " au " + previousEnd);
        }

        return "statistiques";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(defaultValue = "mois") String periode,
                                            @RequestParam(defaultValue = "5") int top,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        int topN = Math.min(Math.max(top, 3), 10);
        String csv = rapportService.genererRapportCsv(periode, topN, debut, fin);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rapport-microbiz-" + LocalDate.now() + ".csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/export-pdf")
    public void exportPdf(@RequestParam(defaultValue = "mois") String periode,
                          @RequestParam(defaultValue = "5") int top,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                          HttpServletResponse response) throws Exception {
        int topN = Math.min(Math.max(top, 3), 10);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=rapport-microbiz-" + LocalDate.now() + ".pdf");
        byte[] pdf = rapportService.genererRapportPDF(periode, topN, debut, fin);
        response.getOutputStream().write(pdf);
        response.getOutputStream().flush();
    }
}
