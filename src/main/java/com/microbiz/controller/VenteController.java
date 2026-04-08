package com.microbiz.controller;

import com.microbiz.model.*;
import com.microbiz.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/ventes")
public class VenteController {

    @Autowired private VenteService   venteService;
    @Autowired private ProduitService produitService;
    @Autowired private ClientService  clientService;

    @GetMapping
    public String liste(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            Model model,
            RedirectAttributes ra) {

        List<Vente> ventes;
        Double caFiltre = null;

        if (debut != null && fin != null) {
            if (fin.isBefore(debut)) {
                ra.addFlashAttribute("erreur", "La date de fin doit être postérieure ou égale à la date de début.");
                return "redirect:/ventes";
            }
            ventes   = venteService.getVentesParPeriode(debut, fin);
            caFiltre = venteService.getCAParPeriode(debut, fin);
            model.addAttribute("debut", debut);
            model.addAttribute("fin",   fin);
            model.addAttribute("filtreActif", true);
        } else {
            ventes = venteService.getVentesRecentes();
            model.addAttribute("filtreActif", false);
        }

        model.addAttribute("ventes",   ventes);
        model.addAttribute("produits", produitService.findAll());
        model.addAttribute("clients",  clientService.findAll());
        model.addAttribute("caJour",   venteService.getCADuJour());
        model.addAttribute("nbVentes", venteService.getNbTransactionsDuJour());
        model.addAttribute("caFiltre", caFiltre);
        return "ventes";
    }

    @PostMapping("/enregistrer")
    public String enregistrer(
            @RequestParam Long    produitId,
            @RequestParam(required = false) Long clientId,
            @RequestParam Integer quantite,
            @RequestParam Double  prixUnitaire,
            RedirectAttributes ra) {

        try {
            Produit produit = produitService.findById(produitId)
                    .orElseThrow(() -> new RuntimeException("Produit introuvable."));

            if (quantite <= 0)
                throw new RuntimeException("La quantité doit être au moins 1.");
            if (prixUnitaire <= 0)
                throw new RuntimeException("Le prix unitaire est invalide.");
            int stockActuel = produit.getStockActuel() == null ? 0 : produit.getStockActuel();
            if (stockActuel < quantite)
                throw new RuntimeException("Stock insuffisant — " + stockActuel + " unité(s) disponible(s).");

            Vente vente = new Vente();
            vente.setProduit(produit);
            vente.setQuantite(quantite);
            vente.setPrixUnitaire(prixUnitaire);
            if (clientId != null)
                vente.setClient(clientService.findById(clientId).orElse(null));

            venteService.enregistrerVente(vente);

            int stockRestant = stockActuel - quantite;
            long total = (long)(quantite * prixUnitaire);
            ra.addFlashAttribute("succes",
                    "Vente enregistrée — " + quantite + " × « " + produit.getNom()
                            + " » = " + String.format("%,d", total).replace(',', ' ')
                            + " FCFA. Stock restant : " + stockRestant + " unité(s).");

        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/ventes";
    }

    // AMÉLIORATION 2 : suppression restaure le stock + message explicite
    @GetMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            venteService.supprimerVente(id);
            ra.addFlashAttribute("succes",
                    "Vente annulée — le stock du produit a été automatiquement restauré.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/ventes";
    }
}
