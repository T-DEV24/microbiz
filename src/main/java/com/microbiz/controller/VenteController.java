package com.microbiz.controller;

import com.microbiz.model.*;
import com.microbiz.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

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
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateVente") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            Model model,
            RedirectAttributes ra) {

        List<String> allowedSorts = List.of("dateVente", "prixUnitaire", "quantite", "id");
        String sortField = allowedSorts.contains(sort) ? sort : "dateVente";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(5, size), 100), Sort.by(direction, sortField));

        Page<Vente> ventesPage;
        Double caFiltre = null;
        boolean filtreActif;

        if (debut != null && fin != null) {
            if (fin.isBefore(debut)) {
                ra.addFlashAttribute("erreur", "La date de fin doit être postérieure ou égale à la date de début.");
                return "redirect:/ventes";
            }
            ventesPage = venteService.getVentesFiltrees(debut, fin, q, pageable);
            model.addAttribute("debut", debut);
            model.addAttribute("fin",   fin);
            filtreActif = true;
        } else {
            ventesPage = venteService.getVentesFiltrees(null, null, q, pageable);
            filtreActif = false;
        }

        String recherche = q == null ? "" : q.trim();
        if (!recherche.isEmpty()) {
            filtreActif = true;
        }

        double caVisible = ventesPage.getContent().stream()
                .mapToDouble(Vente::getMontantTotal)
                .sum();
        if (filtreActif) {
            caFiltre = caVisible;
        }

        model.addAttribute("ventes",   ventesPage.getContent());
        model.addAttribute("ventesPage", ventesPage);
        model.addAttribute("produits", produitService.findAll());
        model.addAttribute("clients",  clientService.findAll());
        model.addAttribute("caJour",   venteService.getCADuJour());
        model.addAttribute("nbVentes", venteService.getNbTransactionsDuJour());
        model.addAttribute("caFiltre", caFiltre);
        model.addAttribute("filtreActif", filtreActif);
        model.addAttribute("q", recherche);
        model.addAttribute("sort", sortField);
        model.addAttribute("dir", direction.name().toLowerCase());
        model.addAttribute("size", pageable.getPageSize());
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
    @PostMapping("/supprimer/{id}")
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
