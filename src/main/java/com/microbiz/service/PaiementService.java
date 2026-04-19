package com.microbiz.service;

import com.microbiz.model.Facture;
import com.microbiz.model.Paiement;
import com.microbiz.repository.FactureRepository;
import com.microbiz.repository.PaiementRepository;
import com.microbiz.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PaiementService {

    @Autowired
    private PaiementRepository paiementRepository;
    @Autowired
    private FactureRepository factureRepository;
    @Autowired
    private CurrencyRateService currencyRateService;

    public Paiement create(Long factureId, Paiement paiement) {
        String tenant = TenantContext.getTenant();
        Facture facture = factureRepository.findByIdAndTenantKeyForUpdate(factureId, tenant)
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));

        if (facture.getStatut() == Facture.StatutFacture.ANNULEE) {
            throw new RuntimeException("Impossible d'encaisser un paiement sur une facture annulée.");
        }

        if (paiement.getMontant() == null || paiement.getMontant() <= 0) {
            throw new RuntimeException("Le montant du paiement doit être positif.");
        }

        if (paiement.getDevise() == null || paiement.getDevise().isBlank()) {
            paiement.setDevise(facture.getDevise());
        }

        double totalFactureBase = currencyRateService.toBase(
                facture.getMontantTtc() == null ? 0.0 : facture.getMontantTtc(),
                facture.getDevise()
        );
        double totalEncaisseBaseAvant = getTotalEncaisseBaseByFacture(factureId);
        double paiementBase = currencyRateService.toBase(paiement.getMontant(), paiement.getDevise());

        if (totalFactureBase > 0 && (totalEncaisseBaseAvant + paiementBase) > (totalFactureBase + 0.0001)) {
            throw new RuntimeException("Le montant saisi dépasse le reste à payer.");
        }

        paiement.setFacture(facture);
        paiement.setTenantKey(tenant);
        Paiement saved = paiementRepository.save(paiement);

        double totalEncaisseBase = totalEncaisseBaseAvant + paiementBase;

        if (totalEncaisseBase >= totalFactureBase && totalFactureBase > 0) {
            facture.setStatut(Facture.StatutFacture.PAYEE);
            factureRepository.save(facture);
        } else if (totalEncaisseBase > 0) {
            facture.setStatut(Facture.StatutFacture.PAIEMENT_PARTIEL);
            factureRepository.save(facture);
        }

        return saved;
    }

    public List<Paiement> findByFacture(Long factureId) {
        return paiementRepository.findByTenantKeyAndFactureIdOrderByDateEncaissementDescIdDesc(TenantContext.getTenant(), factureId);
    }

    public double getTotalEncaisseByFacture(Long factureId) {
        String tenant = TenantContext.getTenant();
        Facture facture = factureRepository.findByIdAndTenantKey(factureId, tenant)
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));
        double totalBase = getTotalEncaisseBaseByFacture(factureId);
        return currencyRateService.fromBase(totalBase, facture.getDevise());
    }

    public double getTotalEncaisseBaseByFacture(Long factureId) {
        return findByFacture(factureId).stream()
                .mapToDouble(p -> currencyRateService.toBase(p.getMontant() == null ? 0.0 : p.getMontant(), p.getDevise()))
                .sum();
    }

    public double getResteAPayer(Long factureId) {
        String tenant = TenantContext.getTenant();
        Facture facture = factureRepository.findByIdAndTenantKey(factureId, tenant)
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));
        double totalFacture = facture.getMontantTtc() == null ? 0.0 : facture.getMontantTtc();
        double totalFactureBase = currencyRateService.toBase(
                facture.getMontantTtc() == null ? 0.0 : facture.getMontantTtc(),
                facture.getDevise()
        );
        double resteBase = Math.max(totalFactureBase - getTotalEncaisseBaseByFacture(factureId), 0.0);
        if (totalFactureBase == 0) {
            return Math.max(totalFacture, 0.0);
        }
        return currencyRateService.fromBase(resteBase, facture.getDevise());
    }
}
