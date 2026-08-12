package br.com.sgd.lideranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.organizacao.FaixaEtaria;
import br.com.sgd.organizacao.Gerencia;
import br.com.sgd.organizacao.GerenciaRepository;
import br.com.sgd.organizacao.Sexo;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChamadaLiderancaHttpTest {
  private static final String SENHA = "senha-inicial-segura";
  private static final LocalDate DATA = LocalDate.of(2026, 8, 14);

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository usuarios;
  @Autowired GerenciaRepository gerencias;
  @Autowired DiscipuladoRepository discipulados;
  @Autowired PasswordEncoder passwords;

  private User admin;
  private User lider;
  private User coLider;
  private User gerente;
  private Discipulado alpha;

  @BeforeEach
  void preparar() {
    String s = UUID.randomUUID().toString();
    admin = usuario("Admin", "admin-cl-" + s, Role.ADMIN);
    lider = usuario("Líder Alpha", "lider-cl-" + s, Role.DISCIPULADOR);
    coLider = usuario("Co Alpha", "co-cl-" + s, Role.CO_LIDER);
    gerente = usuario("Gerente", "gerente-cl-" + s, Role.GERENTE);
    Gerencia centro =
        gerencias.saveAndFlush(
            new Gerencia("Centro CL", Sexo.MASCULINO, Set.of(FaixaEtaria.DE_15_MAIS), gerente));
    alpha = new Discipulado("Alpha CL", Sexo.MASCULINO, FaixaEtaria.DE_15_MAIS, centro, lider);
    alpha.replaceCoLideres(Set.of(coLider));
    alpha = discipulados.saveAndFlush(alpha);
  }

  @Test
  void adminConsultaGradeESalvaPresencasComObservacoes() throws Exception {
    String token = token(admin);

    mvc.perform(
            get("/api/v1/chamadas-lideranca")
                .param("data", DATA.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(DATA.toString()))
        .andExpect(jsonPath("$.discipulados[?(@.discipuladoNome == 'Alpha CL')]").isNotEmpty())
        .andExpect(
            jsonPath("$.discipulados[?(@.discipuladoNome == 'Alpha CL')].presencas.length()")
                .value(org.hamcrest.Matchers.hasItem(2)));

    String payload =
        """
        {
          "data": "%s",
          "observacaoGeral": "Culto tranquilo",
          "discipulados": [
            {
              "discipuladoId": %d,
              "observacao": "Líder chegou atrasado",
              "presencas": [
                {"usuarioId": %d, "papel": "DISCIPULADOR", "situacao": "PRESENTE"},
                {"usuarioId": %d, "papel": "CO_LIDER", "situacao": "AUSENTE"}
              ]
            }
          ]
        }
        """
            .formatted(DATA, alpha.getId(), lider.getId(), coLider.getId());

    mvc.perform(
            put("/api/v1/chamadas-lideranca")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.observacaoGeral").value("Culto tranquilo"))
        .andExpect(jsonPath("$.discipulados[0].observacao").value("Líder chegou atrasado"));

    String body =
        mvc.perform(
                get("/api/v1/chamadas-lideranca")
                    .param("data", DATA.toString())
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode root = json.readTree(body);
    assertThat(root.get("observacaoGeral").asText()).isEqualTo("Culto tranquilo");
    assertThat(root.toString()).contains("PRESENTE").contains("AUSENTE");
  }

  @Test
  void naoAdminRecebeForbidden() throws Exception {
    mvc.perform(
            get("/api/v1/chamadas-lideranca")
                .param("data", DATA.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(token(lider))))
        .andExpect(status().isForbidden());
  }

  private User usuario(String nome, String prefixo, Role... perfis) {
    return usuarios.saveAndFlush(
        new User(nome, prefixo + "@sgd.local", passwords.encode(SENHA), Set.of(perfis)));
  }

  private String token(User usuario) throws Exception {
    String response =
        mvc.perform(
                post("/api/v1/autenticacao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"" + usuario.getEmail() + "\",\"senha\":\"" + SENHA + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(response).get("accessToken").asText();
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }
}
