package com.microbiz.controller;

import com.microbiz.model.AchatCommande;
import com.microbiz.service.AchatCommandeService;
import com.microbiz.service.WebhookService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/achats")
public class AchatApiController {

    @Autowired private AchatCommandeService achatCommandeService;
    @Autowired private WebhookService webhookService;

    @GetMapping
    public List<AchatCommande> list() {
        return achatCommandeService.findAll();
    }

    @PostMapping
    public AchatCommande create(@Valid @RequestBody AchatCommande achatCommande) {
        return achatCommandeService.create(achatCommande);
    }

    @PostMapping("/{id}/reception")
    public AchatCommande receptionner(@PathVariable Long id) {
        AchatCommande achat = achatCommandeService.receptionner(id);
        webhookService.publish("achat.reception", Map.of(
                "id", achat.getId(),
                "produitId", achat.getProduit().getId(),
                "quantite", achat.getQuantite(),
                "statut", achat.getStatut().name()
        ));
        return achat;
    }
}
