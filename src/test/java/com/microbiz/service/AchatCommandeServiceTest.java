package com.microbiz.service;

import com.microbiz.model.AchatCommande;
import com.microbiz.model.Depense;
import com.microbiz.model.Produit;
import com.microbiz.repository.AchatCommandeRepository;
import com.microbiz.repository.ProduitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchatCommandeServiceTest {

    @Mock private AchatCommandeRepository achatCommandeRepository;
    @Mock private ProduitRepository produitRepository;
    @Mock private DepenseService depenseService;
    @InjectMocks private AchatCommandeService achatCommandeService;

    @Test
    void createShouldRejectInvalidQuantity() {
        Produit p = Produit.builder().id(1L).nom("Jus").build();
        AchatCommande achat = AchatCommande.builder()
                .produit(p)
                .quantite(0)
                .coutUnitaire(200.0)
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> achatCommandeService.create(achat));
        assertTrue(ex.getMessage().contains("quantité"));
    }

    @Test
    void receptionnerShouldUpdateStockAndCreateDepense() {
        Produit p = Produit.builder().id(1L).nom("Jus").stockActuel(5).build();
        AchatCommande achat = AchatCommande.builder()
                .id(99L)
                .produit(p)
                .quantite(3)
                .coutUnitaire(200.0)
                .statut(AchatCommande.StatutAchat.COMMANDEE)
                .build();

        when(achatCommandeRepository.findById(99L)).thenReturn(Optional.of(achat));
        when(produitRepository.findById(1L)).thenReturn(Optional.of(p));
        when(achatCommandeRepository.save(any(AchatCommande.class))).thenAnswer(inv -> inv.getArgument(0));

        AchatCommande result = achatCommandeService.receptionner(99L);

        assertEquals(AchatCommande.StatutAchat.RECEPTIONNEE, result.getStatut());
        assertEquals(8, p.getStockActuel());
        verify(produitRepository).save(p);

        ArgumentCaptor<Depense> depenseCaptor = ArgumentCaptor.forClass(Depense.class);
        verify(depenseService).save(depenseCaptor.capture());
        assertEquals(600.0, depenseCaptor.getValue().getMontant());
    }
}
