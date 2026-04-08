package com.microbiz.controller;

import com.microbiz.model.Facture;
import com.microbiz.service.FactureService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/factures")
public class FactureController {

    @Autowired private FactureService factureService;

    @GetMapping
    public String index(Model model) {
        if (!model.containsAttribute("facture")) {
            model.addAttribute("facture", Facture.builder().build());
        }
        model.addAttribute("factures", factureService.findAll());
        model.addAttribute("statuts", Facture.StatutFacture.values());
        model.addAttribute("types", Facture.TypeDocument.values());
        return "factures";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("facture") Facture facture,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("factures", factureService.findAll());
            model.addAttribute("statuts", Facture.StatutFacture.values());
            model.addAttribute("types", Facture.TypeDocument.values());
            return "factures";
        }
        Facture created = factureService.create(facture);
        ra.addFlashAttribute("succes", "Facture " + created.getNumero() + " créée avec succès.");
        return "redirect:/factures";
    }

    @PostMapping("/{id}/statut")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam Facture.StatutFacture statut,
                               RedirectAttributes ra) {
        Facture updated = factureService.updateStatut(id, statut);
        ra.addFlashAttribute("succes", "Statut de " + updated.getNumero() + " mis à jour : " + updated.getStatut());
        return "redirect:/factures";
    }
}
