package br.com.sgd.painel;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PainelDesempenhoHttpTest {
  @Autowired MockMvc mvc;

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminConsultaDesempenhoVazio() throws Exception {
    mvc.perform(
            get("/api/v1/painel/desempenho-discipulados")
                .param("dataInicio", "2026-01-01")
                .param("dataFim", "2026-06-30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dataInicio").value("2026-01-01"))
        .andExpect(jsonPath("$.dataFim").value("2026-06-30"))
        .andExpect(jsonPath("$.discipulados").isArray());
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  void rejeitaUsuarioQueNaoEAdmin() throws Exception {
    mvc.perform(
            get("/api/v1/painel/desempenho-discipulados")
                .param("dataInicio", "2026-01-01")
                .param("dataFim", "2026-06-30"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void periodoInvalidoSegueProblemDetails() throws Exception {
    mvc.perform(
            get("/api/v1/painel/desempenho-discipulados")
                .param("dataInicio", "2026-07-01")
                .param("dataFim", "2026-06-30"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").isNotEmpty());
  }
}
