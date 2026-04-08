package com.microbiz.controller;

import com.microbiz.model.Fournisseur;
import com.microbiz.service.FournisseurService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fournisseurs")
public class FournisseurApiController {

    @Autowired private FournisseurService fournisseurService;

    @GetMapping
    public List<Fournisseur> list() {
        return fournisseurService.findAll();
    }

    @PostMapping
    public Fournisseur create(@Valid @RequestBody Fournisseur fournisseur) {
        return fournisseurService.save(fournisseur);
    }
}
