package com.microbiz.service;

import com.microbiz.model.Facture;
import com.microbiz.repository.FactureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FactureServiceTest {

    @Mock private FactureRepository factureRepository;
    @InjectMocks private FactureService factureService;

    @Test
    void createShouldGenerateNumberAndDefaultStatus() {
        Facture facture = Facture.builder()
                .type(Facture.TypeDocument.FACTURE)
                .clientNom("Client Test")
                .montantTtc(15000.0)
                .build();

        when(factureRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(factureRepository.existsByNumero("FAC-" + LocalDate.now().getYear() + "-00001")).thenReturn(false);
        when(factureRepository.save(any(Facture.class))).thenAnswer(inv -> inv.getArgument(0));

        Facture created = factureService.create(facture);

        assertNotNull(created.getNumero());
        assertEquals(Facture.StatutFacture.BROUILLON, created.getStatut());
        assertNotNull(created.getDateEmission());
    }

    @Test
    void updateStatutShouldRejectInvalidTransition() {
        Facture facture = Facture.builder()
                .id(10L)
                .numero("FAC-2026-00010")
                .statut(Facture.StatutFacture.PAYEE)
                .clientNom("Client")
                .montantTtc(5000.0)
                .build();

        when(factureRepository.findById(10L)).thenReturn(Optional.of(facture));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> factureService.updateStatut(10L, Facture.StatutFacture.BROUILLON));

        assertTrue(ex.getMessage().contains("Transition invalide"));
    }
}
