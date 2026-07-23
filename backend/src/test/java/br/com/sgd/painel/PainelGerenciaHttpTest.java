package br.com.sgd.painel;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class PainelGerenciaHttpTest {
  @Autowired MockMvc mvc;

  @Test
  @WithMockUser(roles = "ADMIN")
  void rejeitaAdminSemPerfilGerente() throws Exception {
    mvc.perform(
            get("/api/v1/painel/gerencia")
                .param("dataInicio", "2026-01-01")
                .param("dataFim", "2026-06-30"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "DISCIPULADOR")
  void rejeitaDiscipulador() throws Exception {
    mvc.perform(
            get("/api/v1/painel/gerencia")
                .param("dataInicio", "2026-01-01")
                .param("dataFim", "2026-06-30"))
        .andExpect(status().isForbidden());
  }
}
