package com.microbiz.controller;

import com.microbiz.model.Produit;
import com.microbiz.service.ProduitService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/produits")
public class ProduitController {

    @Autowired private ProduitService produitService;

    @GetMapping
    public String liste(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "15") int size,
                        @RequestParam(defaultValue = "nom") String sort,
                        @RequestParam(defaultValue = "asc") String dir,
                        Model model) {
        String sortField = switch (sort) {
            case "categorie", "prixVente", "stockActuel", "id" -> sort;
            default -> "nom";
        };
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(5, size), 100), Sort.by(direction, sortField));
        Page<Produit> produitsPage = produitService.findAll(pageable);

        model.addAttribute("produits",  produitsPage.getContent());
        model.addAttribute("produitsPage", produitsPage);
        model.addAttribute("stockBas",  produitService.getProduitsStockBas());
        model.addAttribute("produit",   new Produit());
        model.addAttribute("sort", sortField);
        model.addAttribute("dir", direction.name().toLowerCase());
        model.addAttribute("size", pageable.getPageSize());
        return "produits";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("produit", new Produit());
        return "produit-form";
    }

    @GetMapping("/modifier/{id}")
    public String modifier(@PathVariable Long id, Model model) {
        model.addAttribute("produit", produitService.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable")));
        return "produit-form";
    }

    @PostMapping("/sauvegarder")
    public String sauvegarder(@Valid @ModelAttribute Produit produit,
                              BindingResult result,
                              Model model,
                              RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("produits", produitService.findAll());
            model.addAttribute("stockBas", produitService.getProduitsStockBas());
            return "produits";
        }
        boolean isNew = (produit.getId() == null);
        produitService.save(produit);
        ra.addFlashAttribute("succes",
                isNew ? "Produit « " + produit.getNom() + " » ajouté !"
                        : "Produit « " + produit.getNom() + " » modifié !");
        return "redirect:/produits";
    }

    // AMÉLIORATION 3 : réapprovisionnement inline
    @PostMapping("/reapprovisionner/{id}")
    public String reapprovisionner(@PathVariable Long id,
                                   @RequestParam int quantite,
                                   RedirectAttributes ra) {
        if (quantite <= 0) {
            ra.addFlashAttribute("erreur", "La quantité doit être positive.");
            return "redirect:/produits";
        }
        Produit p = produitService.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));
        p.setStockActuel(p.getStockActuel() + quantite);
        produitService.save(p);
        ra.addFlashAttribute("succes",
                "Stock de « " + p.getNom() + " » mis à jour : +" + quantite
                        + " unités (total : " + p.getStockActuel() + ").");
        return "redirect:/produits";
    }

    @PostMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            produitService.deleteById(id);
            ra.addFlashAttribute("succes", "Produit supprimé.");
        } catch (Exception e) {
            ra.addFlashAttribute("erreur",
                    "Impossible de supprimer ce produit — il est lié à des ventes existantes.");
        }
        return "redirect:/produits";
    }
}
