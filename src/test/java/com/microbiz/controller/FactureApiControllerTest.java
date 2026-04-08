package com.microbiz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microbiz.model.Facture;
import com.microbiz.service.FactureService;
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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FactureApiControllerTest {

    @Mock private FactureService factureService;
    @Mock private WebhookService webhookService;
    @InjectMocks private FactureApiController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createShouldReturnOk() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        Facture payload = Facture.builder()
                .clientNom("Client Test")
                .montantTtc(1200.0)
                .type(Facture.TypeDocument.FACTURE)
                .build();

        Facture saved = Facture.builder()
                .id(1L)
                .numero("FAC-2026-00001")
                .clientNom("Client Test")
                .montantTtc(1200.0)
                .type(Facture.TypeDocument.FACTURE)
                .statut(Facture.StatutFacture.BROUILLON)
                .build();

        when(factureService.create(any(Facture.class))).thenReturn(saved);
        doNothing().when(webhookService).publish(any(), any());

        mvc.perform(post("/api/v1/factures")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }
}
