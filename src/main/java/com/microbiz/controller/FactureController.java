package com.microbiz.controller;

import com.microbiz.model.Facture;
import com.microbiz.model.FactureLigne;
import com.microbiz.service.CurrencyRateService;
import com.microbiz.service.FactureService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/factures")
public class FactureController {

    @Autowired private FactureService factureService;
    @Autowired private CurrencyRateService currencyRateService;

    @GetMapping
    public String index(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) Facture.StatutFacture statut,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                        Model model) {
        if (!model.containsAttribute("facture")) {
            model.addAttribute("facture", Facture.builder().build());
        }
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 5), 50), Sort.by("dateEmission").descending());
        Page<Facture> facturesPage = factureService.search(q, statut, debut, fin, pageable);
        model.addAttribute("factures", facturesPage.getContent());
        model.addAttribute("facturesPage", facturesPage);
        model.addAttribute("statuts", Facture.StatutFacture.values());
        model.addAttribute("types", Facture.TypeDocument.values());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("statut", statut);
        model.addAttribute("debut", debut);
        model.addAttribute("fin", fin);
        model.addAttribute("size", pageable.getPageSize());
        model.addAttribute("devises", java.util.List.of("XAF", "EUR", "USD", "GNF"));
        model.addAttribute("devisePrincipale", currencyRateService.getBaseCurrency());
        return "factures";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("facture") Facture facture,
                         @RequestParam(required = false) String ligneDescription,
                         @RequestParam(required = false) Integer ligneQuantite,
                         @RequestParam(required = false) Double lignePrixUnitaire,
                         @RequestParam(required = false, defaultValue = "0") Double ligneRemise,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (ligneDescription != null && !ligneDescription.isBlank()
                && ligneQuantite != null && ligneQuantite > 0
                && lignePrixUnitaire != null && lignePrixUnitaire > 0) {
            facture.addLigne(FactureLigne.builder()
                    .description(ligneDescription.trim())
                    .quantite(ligneQuantite)
                    .prixUnitaire(lignePrixUnitaire)
                    .remise(ligneRemise != null ? ligneRemise : 0.0)
                    .build());
        }
        if (result.hasErrors()) {
            model.addAttribute("factures", factureService.search(null, null, null, null, PageRequest.of(0, 10)).getContent());
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
        try {
            Facture updated = factureService.updateStatut(id, statut);
            ra.addFlashAttribute("succes", "Statut de " + updated.getNumero() + " mis à jour : " + updated.getStatut());
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/factures";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        Facture facture = factureService.findById(id);
        byte[] pdf = factureService.genererPdf(id);
        String numero = facture.getNumero();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + numero + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
