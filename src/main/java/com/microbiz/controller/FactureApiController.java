package com.microbiz.controller;

import com.microbiz.model.Facture;
import com.microbiz.service.FactureService;
import com.microbiz.service.WebhookService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/factures")
public class FactureApiController {

    @Autowired private FactureService factureService;
    @Autowired private WebhookService webhookService;

    @GetMapping
    public List<Facture> list() {
        return factureService.findAll();
    }

    @PostMapping
    public Facture create(@Valid @RequestBody Facture facture) {
        Facture created = factureService.create(facture);
        webhookService.publish("facture.created", Map.of(
                "id", created.getId(),
                "numero", created.getNumero(),
                "type", created.getType().name(),
                "statut", created.getStatut().name(),
                "montantTtc", created.getMontantTtc()
        ));
        return created;
    }

    @PatchMapping("/{id}/statut")
    public Facture changeStatus(@PathVariable Long id,
                                @RequestParam Facture.StatutFacture statut) {
        Facture updated = factureService.updateStatut(id, statut);
        webhookService.publish("facture.status_changed", Map.of(
                "id", updated.getId(),
                "numero", updated.getNumero(),
                "statut", updated.getStatut().name()
        ));
        return updated;
    }
}
