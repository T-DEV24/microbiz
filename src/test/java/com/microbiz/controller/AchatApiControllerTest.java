package com.microbiz.controller;

import com.microbiz.model.AchatCommande;
import com.microbiz.model.Produit;
import com.microbiz.service.AchatCommandeService;
import com.microbiz.service.WebhookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AchatApiControllerTest {

    @Mock private AchatCommandeService achatCommandeService;
    @Mock private WebhookService webhookService;
    @InjectMocks private AchatApiController controller;

    @Test
    void receptionnerShouldReturnOk() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        AchatCommande achat = AchatCommande.builder()
                .id(1L)
                .produit(Produit.builder().id(2L).nom("Jus").build())
                .quantite(5)
                .coutUnitaire(300.0)
                .statut(AchatCommande.StatutAchat.RECEPTIONNEE)
                .build();

        when(achatCommandeService.receptionner(1L)).thenReturn(achat);
        doNothing().when(webhookService).publish(any(), any());

        mvc.perform(post("/api/v1/achats/1/reception"))
                .andExpect(status().isOk());
    }
}
