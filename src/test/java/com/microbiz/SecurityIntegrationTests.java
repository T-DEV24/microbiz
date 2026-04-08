package com.microbiz;

import com.microbiz.service.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientService clientService;

    @Test
    @WithMockUser(username = "admin@microbiz.com", roles = {"ADMIN"})
    void deleteEndpointWithoutCsrfShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/clients/supprimer/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@microbiz.com", roles = {"ADMIN"})
    void deleteEndpointWithCsrfShouldNotBeForbidden() throws Exception {
        doNothing().when(clientService).deleteById(1L);
        mockMvc.perform(post("/clients/supprimer/1").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
