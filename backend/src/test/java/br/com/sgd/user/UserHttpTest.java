package br.com.sgd.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserHttpTest {
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;

  @Test
  @WithMockUser(roles = "ADMIN")
  void administradorAtribuiAdminADiscipuladorExistente() throws Exception {
    long id = criarUsuario("João Líder", "DISCIPULADOR");

    mvc.perform(
            patch("/api/v1/usuarios/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"perfis\":[\"DISCIPULADOR\",\"ADMIN\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.perfis.length()").value(2))
        .andExpect(jsonPath("$.perfis[?(@ == 'ADMIN')]").isNotEmpty())
        .andExpect(jsonPath("$.perfis[?(@ == 'DISCIPULADOR')]").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void administradorCriaUsuarioComPerfisAcumulados() throws Exception {
    String email = "lider-admin-" + UUID.randomUUID() + "@sgd.local";
    mvc.perform(
            post("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nome\":\"Ana\",\"email\":\""
                        + email
                        + "\",\"senha\":\"senha-inicial-segura\",\"perfis\":[\"DISCIPULADOR\",\"ADMIN\"]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.perfis.length()").value(2))
        .andExpect(jsonPath("$.perfis[?(@ == 'ADMIN')]").isNotEmpty())
        .andExpect(jsonPath("$.perfis[?(@ == 'DISCIPULADOR')]").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void rejeitaAtualizacaoComListaDePerfisVazia() throws Exception {
    long id = criarUsuario("Maria", "GERENTE");

    mvc.perform(
            patch("/api/v1/usuarios/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"perfis\":[]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "DISCIPULADOR")
  void discipuladorNaoPodeAlterarPerfis() throws Exception {
    mvc.perform(
            patch("/api/v1/usuarios/{id}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"perfis\":[\"ADMIN\"]}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void listaUsuariosPorTrechoDoNome() throws Exception {
    criarUsuario("João Líder", "DISCIPULADOR");
    criarUsuario("Maria Gestora", "GERENTE");

    mvc.perform(get("/api/v1/usuarios").param("busca", "joão"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.nome == 'João Líder')]").isNotEmpty())
        .andExpect(jsonPath("$.content[?(@.nome == 'Maria Gestora')]").isEmpty());
  }

  private long criarUsuario(String nome, String perfil) throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/usuarios")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"nome\":\""
                            + nome
                            + "\",\"email\":\""
                            + perfil.toLowerCase()
                            + "-"
                            + UUID.randomUUID()
                            + "@sgd.local\",\"senha\":\"senha-inicial-segura\",\"perfis\":[\""
                            + perfil
                            + "\"]}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(body).get("id").asLong();
  }
}
