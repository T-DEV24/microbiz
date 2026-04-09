package com.microbiz.controller;

import com.microbiz.model.Fournisseur;
import com.microbiz.service.FournisseurService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fournisseurs")
public class FournisseurApiController {

    @Autowired private FournisseurService fournisseurService;

    @GetMapping
    public Page<Fournisseur> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false) String q) {
        int pageSafe = Math.max(0, page);
        int sizeSafe = Math.min(Math.max(5, size), 100);
        return fournisseurService.rechercher(q, PageRequest.of(pageSafe, sizeSafe));
    }

    @PostMapping
    public Fournisseur create(@Valid @RequestBody Fournisseur fournisseur) {
        return fournisseurService.save(fournisseur);
    }
}
