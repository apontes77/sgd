package br.com.sgd.painel;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PainelAdminHttpTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsultaPainelVazio() throws Exception {
        mvc.perform(get("/api/v1/painel/admin").param("dataInicio", "2026-01-01").param("dataFim", "2026-06-30"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.resumo.presentes").value(0))
            .andExpect(jsonPath("$.resumo.percentualPresenca").value(0)).andExpect(jsonPath("$.sexos.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void rejeitaUsuarioQueNaoEAdmin() throws Exception {
        mvc.perform(get("/api/v1/painel/admin").param("dataInicio", "2026-01-01").param("dataFim", "2026-06-30"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void periodoInvalidoSegueProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/painel/admin").param("dataInicio", "2026-07-01").param("dataFim", "2026-06-30"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.detail").isNotEmpty());
    }
}
