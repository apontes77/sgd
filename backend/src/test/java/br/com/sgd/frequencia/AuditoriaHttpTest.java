package br.com.sgd.frequencia;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.sgd.audit.AuditLog;
import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditoriaHttpTest {
  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired AuditLogRepository audit;

  @Test
  @WithMockUser(roles = "ADMIN")
  void filtraAuditoriaEPreservaDetalhesInvalidosComoTexto() throws Exception {
    User user =
        users.save(
            new User(
                "Auditor",
                "auditor-" + UUID.randomUUID() + "@sgd.local",
                "hash",
                Set.of(Role.ADMIN)));
    audit.save(new AuditLog(user, "USUARIO", "JSON", "{\"campo\":\"valor\"}"));
    audit.save(new AuditLog(user, "USUARIO", "TEXTO", "detalhe legado"));
    audit.flush();

    mvc.perform(
            get("/api/v1/auditoria")
                .param("entidade", "USUARIO")
                .param("usuarioId", user.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[0].detalhes").exists())
        .andExpect(jsonPath("$.content[1].detalhes").exists());
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  void exigeAdministrador() throws Exception {
    mvc.perform(get("/api/v1/auditoria")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void rejeitaTamanhoDePaginaInvalido() throws Exception {
    mvc.perform(get("/api/v1/auditoria").param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }
}
