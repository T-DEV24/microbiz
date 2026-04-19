package com.microbiz.controller;

import com.microbiz.model.Paiement;
import com.microbiz.service.PaiementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/paiements")
public class PaiementApiController {

    @Autowired
    private PaiementService paiementService;

    @PostMapping
    public PaiementResponse create(@Valid @RequestBody PaiementCreateRequest request) {
        Paiement paiement = Paiement.builder()
                .montant(request.montant())
                .devise(request.devise())
                .modePaiement(request.modePaiement())
                .dateEncaissement(request.dateEncaissement())
                .reference(request.reference())
                .build();
        Paiement saved = paiementService.create(request.factureId(), paiement);
        return toResponse(saved);
    }

    @GetMapping("/{factureId}")
    public Map<String, Object> listByFacture(@PathVariable Long factureId) {
        List<Paiement> paiements = paiementService.findByFacture(factureId);
        double totalEncaisse = paiementService.getTotalEncaisseByFacture(factureId);
        double resteAPayer = paiementService.getResteAPayer(factureId);
        List<PaiementResponse> paiementResponses = paiements.stream()
                .map(this::toResponse)
                .toList();
        return Map.of(
                "factureId", factureId,
                "totalEncaisse", totalEncaisse,
                "resteAPayer", resteAPayer,
                "paiements", paiementResponses
        );
    }

    public record PaiementCreateRequest(
            @NotNull Long factureId,
            @NotNull @Positive Double montant,
            String devise,
            @NotNull Paiement.ModePaiement modePaiement,
            java.time.LocalDate dateEncaissement,
            String reference
    ) {}

    private PaiementResponse toResponse(Paiement paiement) {
        return new PaiementResponse(
                paiement.getId(),
                paiement.getMontant(),
                paiement.getDevise(),
                paiement.getModePaiement(),
                paiement.getDateEncaissement(),
                paiement.getReference()
        );
    }

    public record PaiementResponse(
            Long id,
            Double montant,
            String devise,
            Paiement.ModePaiement modePaiement,
            LocalDate dateEncaissement,
            String reference
    ) {}
}
