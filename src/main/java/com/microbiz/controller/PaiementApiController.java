package com.microbiz.controller;

import com.microbiz.model.Paiement;
import com.microbiz.service.PaiementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/paiements")
public class PaiementApiController {

    @Autowired
    private PaiementService paiementService;

    @PostMapping
    public Paiement create(@Valid @RequestBody PaiementCreateRequest request) {
        Paiement paiement = Paiement.builder()
                .montant(request.montant())
                .devise(request.devise())
                .modePaiement(request.modePaiement())
                .dateEncaissement(request.dateEncaissement())
                .reference(request.reference())
                .build();
        return paiementService.create(request.factureId(), paiement);
    }

    @GetMapping("/{factureId}")
    public Map<String, Object> listByFacture(@PathVariable Long factureId) {
        List<Paiement> paiements = paiementService.findByFacture(factureId);
        double totalEncaisse = paiementService.getTotalEncaisseByFacture(factureId);
        return Map.of(
                "factureId", factureId,
                "totalEncaisse", totalEncaisse,
                "paiements", paiements
        );
    }

    public record PaiementCreateRequest(
            @NotNull Long factureId,
            @NotNull Double montant,
            String devise,
            @NotNull Paiement.ModePaiement modePaiement,
            java.time.LocalDate dateEncaissement,
            String reference
    ) {}
}
