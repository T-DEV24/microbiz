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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        when(factureRepository.findByIdAndTenantKeyForUpdate(1L, "default")).thenReturn(Optional.of(facture));
        when(paiementRepository.save(any(Paiement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paiementRepository.findByTenantKeyAndFactureIdOrderByDateEncaissementDescIdDesc("default", 1L))
                .thenReturn(List.of());
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

        when(factureRepository.findByIdAndTenantKeyForUpdate(2L, "default")).thenReturn(Optional.of(facture));
        when(paiementRepository.save(any(Paiement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paiementRepository.findByTenantKeyAndFactureIdOrderByDateEncaissementDescIdDesc("default", 2L))
                .thenReturn(List.of());
        when(currencyRateService.toBase(10000.0, "XAF")).thenReturn(10000.0);

        paiementService.create(2L, paiement);

        assertEquals(Facture.StatutFacture.PAYEE, facture.getStatut());
    }

    @Test
    void createShouldRejectOverpayment() {
        Facture facture = Facture.builder()
                .id(3L)
                .devise("XAF")
                .montantTtc(10000.0)
                .statut(Facture.StatutFacture.ENVOYEE)
                .clientNom("Client")
                .numero("FAC-2026-00003")
                .build();

        Paiement paiement = Paiement.builder()
                .montant(6000.0)
                .devise("XAF")
                .modePaiement(Paiement.ModePaiement.ESPECES)
                .build();
        Paiement ancienPaiement = Paiement.builder()
                .montant(5000.0)
                .devise("XAF")
                .modePaiement(Paiement.ModePaiement.ESPECES)
                .build();

        when(factureRepository.findByIdAndTenantKeyForUpdate(3L, "default")).thenReturn(Optional.of(facture));
        when(paiementRepository.findByTenantKeyAndFactureIdOrderByDateEncaissementDescIdDesc("default", 3L))
                .thenReturn(List.of(ancienPaiement));
        when(currencyRateService.toBase(5000.0, "XAF")).thenReturn(5000.0);
        when(currencyRateService.toBase(6000.0, "XAF")).thenReturn(6000.0);
        when(currencyRateService.toBase(10000.0, "XAF")).thenReturn(10000.0);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> paiementService.create(3L, paiement));
        assertEquals("Le montant saisi dépasse le reste à payer.", ex.getMessage());
    }

    @Test
    void createShouldRejectPaymentForCanceledInvoice() {
        Facture facture = Facture.builder()
                .id(4L)
                .devise("XAF")
                .montantTtc(10000.0)
                .statut(Facture.StatutFacture.ANNULEE)
                .clientNom("Client")
                .numero("FAC-2026-00004")
                .build();

        Paiement paiement = Paiement.builder()
                .montant(1000.0)
                .devise("XAF")
                .modePaiement(Paiement.ModePaiement.ESPECES)
                .build();

        when(factureRepository.findByIdAndTenantKeyForUpdate(4L, "default")).thenReturn(Optional.of(facture));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> paiementService.create(4L, paiement));
        assertEquals("Impossible d'encaisser un paiement sur une facture annulée.", ex.getMessage());
    }
}
