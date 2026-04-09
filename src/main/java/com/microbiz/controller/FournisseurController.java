package com.microbiz.controller;

import com.microbiz.model.Fournisseur;
import com.microbiz.service.AuditLogService;
import com.microbiz.service.FournisseurService;
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
@RequestMapping("/fournisseurs")
public class FournisseurController {

    @Autowired private FournisseurService fournisseurService;
    @Autowired private AuditLogService auditLogService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "15") int size,
                       @RequestParam(defaultValue = "nom") String sort,
                       @RequestParam(defaultValue = "asc") String dir,
                       @RequestParam(required = false) String q,
                       Model model) {
        String sortField = switch (sort) {
            case "email", "telephone", "id" -> sort;
            default -> "nom";
        };
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(5, size), 100), Sort.by(direction, sortField));
        Page<Fournisseur> fournisseursPage = fournisseurService.rechercher(q, pageable);

        model.addAttribute("fournisseurs", fournisseursPage.getContent());
        model.addAttribute("fournisseursPage", fournisseursPage);
        model.addAttribute("fournisseur", new Fournisseur());
        model.addAttribute("sort", sortField);
        model.addAttribute("dir", direction.name().toLowerCase());
        model.addAttribute("size", pageable.getPageSize());
        model.addAttribute("q", q == null ? "" : q);
        return "fournisseurs";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("fournisseur", new Fournisseur());
        return "fournisseur-form";
    }

    @GetMapping("/modifier/{id}")
    public String modifier(@PathVariable Long id, Model model) {
        model.addAttribute("fournisseur", fournisseurService.findById(id));
        return "fournisseur-form";
    }

    @PostMapping("/sauvegarder")
    public String sauvegarder(@Valid @ModelAttribute Fournisseur fournisseur,
                              BindingResult result,
                              RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "fournisseur-form";
        }
        boolean isNew = fournisseur.getId() == null;
        Fournisseur saved = fournisseurService.save(fournisseur);
        auditLogService.log(isNew ? "CREATE" : "UPDATE", "FOURNISSEUR", saved.getId(), saved.getNom());
        ra.addFlashAttribute("succes", isNew ? "Fournisseur ajouté." : "Fournisseur modifié.");
        return "redirect:/fournisseurs";
    }

    @PostMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        fournisseurService.deleteById(id);
        auditLogService.log("DELETE", "FOURNISSEUR", id, "Suppression fournisseur");
        ra.addFlashAttribute("succes", "Fournisseur supprimé.");
        return "redirect:/fournisseurs";
    }
}
