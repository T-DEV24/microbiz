package com.microbiz.service;

import com.microbiz.model.Facture;
import com.microbiz.model.Paiement;
import com.microbiz.repository.FactureRepository;
import com.microbiz.repository.PaiementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaiementServiceTest {

    @Mock private PaiementRepository paiementRepository;
    @Mock private FactureRepository factureRepository;
    @Mock private CurrencyRateService currencyRateService;

    @InjectMocks private PaiementService paiementService;

    @Test
    void createShouldSetPartielWhenPartialPayment() {
        Facture facture = Facture.builder()
                .id(1L)
                .devise("XAF")
                .montantTtc(10000.0)
                .statut(Facture.StatutFacture.ENVOYEE)
                .clientNom("Client")
                .numero("FAC-2026-00001")
                .build();

        Paiement paiement = Paiement.builder()
                .montant(5000.0)
                .devise("XAF")
                .modePaiement(Paiement.ModePaiement.ESPECES)
                .build();

        when(factureRepository.findByIdAndTenantKey(1L, "default")).thenReturn(Optional.of(facture));
        when(paiementRepository.save(any(Paiement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paiementRepository.findByTenantKeyAndFactureIdOrderByDateEncaissementDescIdDesc("default", 1L))
                .thenReturn(List.of(paiement));
        when(currencyRateService.toBase(5000.0, "XAF")).thenReturn(5000.0);
        when(currencyRateService.toBase(10000.0, "XAF")).thenReturn(10000.0);

        paiementService.create(1L, paiement);

        assertEquals(Facture.StatutFacture.PAIEMENT_PARTIEL, facture.getStatut());
    }

    @Test
    void createShouldSetPayeeWhenTotalReached() {
        Facture facture = Facture.builder()
                .id(2L)
                .devise("XAF")
                .montantTtc(10000.0)
                .statut(Facture.StatutFacture.ENVOYEE)
                .clientNom("Client")
                .numero("FAC-2026-00002")
                .build();

        Paiement paiement = Paiement.builder()
                .montant(10000.0)
                .devise("XAF")
                .modePaiement(Paiement.ModePaiement.VIREMENT)
                .build();

        when(factureRepository.findByIdAndTenantKey(2L, "default")).thenReturn(Optional.of(facture));
        when(paiementRepository.save(any(Paiement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paiementRepository.findByTenantKeyAndFactureIdOrderByDateEncaissementDescIdDesc("default", 2L))
                .thenReturn(List.of(paiement));
        when(currencyRateService.toBase(10000.0, "XAF")).thenReturn(10000.0);

        paiementService.create(2L, paiement);

        assertEquals(Facture.StatutFacture.PAYEE, facture.getStatut());
    }
}
