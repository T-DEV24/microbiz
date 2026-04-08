package com.microbiz.controller;

import com.microbiz.model.Depense;
import com.microbiz.service.DepenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/depenses")
public class DepenseController {

    @Autowired private DepenseService depenseService;

    @GetMapping
    public String liste(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            Model model) {

        List<Depense> depenses;
        Double totalFiltre = null;

        if (debut != null && fin != null) {
            depenses    = depenseService.getDepensesParPeriode(debut, fin);
            totalFiltre = depenseService.getTotalParPeriode(debut, fin);
            model.addAttribute("debut", debut);
            model.addAttribute("fin",   fin);
            model.addAttribute("filtreActif", true);
        } else {
            depenses = depenseService.findAll();
            model.addAttribute("filtreActif", false);
        }

        model.addAttribute("depenses",      depenses);
        model.addAttribute("totalDepenses", depenseService.getTotalDepenses());
        model.addAttribute("depensesMois",  depenseService.getDepensesDuMois());
        model.addAttribute("parCategorie",  depenseService.getDepensesParCategorie());
        model.addAttribute("totalFiltre",   totalFiltre);
        return "depenses";
    }

    @PostMapping("/sauvegarder")
    public String sauvegarder(@ModelAttribute Depense depense, RedirectAttributes ra) {
        if (depense.getMontant() == null || depense.getMontant() <= 0) {
            ra.addFlashAttribute("erreur", "Le montant doit être positif.");
            return "redirect:/depenses";
        }
        depenseService.save(depense);
        ra.addFlashAttribute("succes",
                "Dépense de "
                        + String.format("%,.0f", depense.getMontant()).replace(',', ' ')
                        + " FCFA enregistrée !");
        return "redirect:/depenses";
    }

    @GetMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        depenseService.deleteById(id);
        ra.addFlashAttribute("succes", "Dépense supprimée.");
        return "redirect:/depenses";
    }
}
