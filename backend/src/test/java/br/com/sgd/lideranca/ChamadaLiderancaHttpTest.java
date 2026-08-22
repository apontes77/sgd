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
            jsonPath("$.discipulados[?(@.discipuladoNome == 'Alpha CL')].sexo")
                .value(org.hamcrest.Matchers.hasItem("MASCULINO")))
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
  void adminSalvaPresencasParciaisEMesclaNasSeguintes() throws Exception {
    String token = token(admin);
    String primeiro =
        """
        {
          "data": "%s",
          "observacaoGeral": null,
          "discipulados": [
            {
              "discipuladoId": %d,
              "observacao": null,
              "presencas": [
                {"usuarioId": %d, "papel": "DISCIPULADOR", "situacao": "PRESENTE"}
              ]
            }
          ]
        }
        """
            .formatted(DATA, alpha.getId(), lider.getId());

    mvc.perform(
            put("/api/v1/chamadas-lideranca")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(primeiro))
        .andExpect(status().isOk());

    JsonNode depoisDoPrimeiro = discipuladoPorNome(consultar(token), "Alpha CL");
    assertThat(situacao(depoisDoPrimeiro, lider.getId())).isEqualTo("PRESENTE");
    assertThat(situacao(depoisDoPrimeiro, coLider.getId())).isNull();

    String segundo =
        """
        {
          "data": "%s",
          "observacaoGeral": "Parcial",
          "discipulados": [
            {
              "discipuladoId": %d,
              "observacao": "Co-líder faltou",
              "presencas": [
                {"usuarioId": %d, "papel": "CO_LIDER", "situacao": "AUSENTE"}
              ]
            }
          ]
        }
        """
            .formatted(DATA, alpha.getId(), coLider.getId());

    mvc.perform(
            put("/api/v1/chamadas-lideranca")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(segundo))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.observacaoGeral").value("Parcial"));

    JsonNode depoisDoSegundo = discipuladoPorNome(consultar(token), "Alpha CL");
    assertThat(depoisDoSegundo.get("observacao").asText()).isEqualTo("Co-líder faltou");
    assertThat(situacao(depoisDoSegundo, lider.getId())).isEqualTo("PRESENTE");
    assertThat(situacao(depoisDoSegundo, coLider.getId())).isEqualTo("AUSENTE");
  }

  @Test
  void rejeitaPresencaDeQuemNaoLideraODiscipulado() throws Exception {
    String payload =
        """
        {
          "data": "%s",
          "observacaoGeral": null,
          "discipulados": [
            {
              "discipuladoId": %d,
              "observacao": null,
              "presencas": [
                {"usuarioId": %d, "papel": "DISCIPULADOR", "situacao": "PRESENTE"}
              ]
            }
          ]
        }
        """
            .formatted(DATA, alpha.getId(), gerente.getId());

    mvc.perform(
            put("/api/v1/chamadas-lideranca")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(admin)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void pedeConfirmacaoAoAtualizarPresencaJaSalvaNoMesmoDia() throws Exception {
    Discipulado beta = criarBetaComMesmoLider();
    String token = token(admin);

    mvc.perform(
            put("/api/v1/chamadas-lideranca")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadPresenca(alpha.getId(), lider.getId(), "DISCIPULADOR", "PRESENTE")))
        .andExpect(status().isOk());

    JsonNode grade = consultar(token);
    JsonNode presencaNoBeta = presenca(discipuladoPorNome(grade, "Beta CL"), lider.getId());
    assertThat(presencaNoBeta.get("situacao").isNull()).isTrue();
    assertThat(presencaNoBeta.get("registroDoDia").get("discipuladoNome").asText())
        .isEqualTo("Alpha CL");
    assertThat(presencaNoBeta.get("registroDoDia").get("situacao").asText()).isEqualTo("PRESENTE");

    mvc.perform(
            put("/api/v1/chamadas-lideranca")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadPresenca(beta.getId(), lider.getId(), "DISCIPULADOR", "AUSENTE")))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail")
                .value(org.hamcrest.Matchers.containsString("já teve chamada salva")))
        .andExpect(jsonPath("$.conflitos[0].usuarioId").value(lider.getId()))
        .andExpect(jsonPath("$.conflitos[0].discipuladoNome").value("Alpha CL"))
        .andExpect(jsonPath("$.conflitos[0].situacao").value("PRESENTE"));

    JsonNode depoisDaRecusa = consultar(token);
    assertThat(situacao(discipuladoPorNome(depoisDaRecusa, "Alpha CL"), lider.getId()))
        .isEqualTo("PRESENTE");
    assertThat(situacao(discipuladoPorNome(depoisDaRecusa, "Beta CL"), lider.getId())).isNull();
  }

  @Test
  void outroAdminAtualizaPresencaAposConfirmacao() throws Exception {
    User outroAdmin = usuario("Admin 2", "admin2-cl-" + UUID.randomUUID(), Role.ADMIN);
    String tokenAdmin2 = token(outroAdmin);

    mvc.perform(
            put("/api/v1/chamadas-lideranca")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(admin)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadPresenca(alpha.getId(), lider.getId(), "DISCIPULADOR", "PRESENTE")))
        .andExpect(status().isOk());

    mvc.perform(
            put("/api/v1/chamadas-lideranca")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin2))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    payloadPresenca(
                        alpha.getId(), lider.getId(), "DISCIPULADOR", "AUSENTE", false)))
        .andExpect(status().isConflict());

    mvc.perform(
            put("/api/v1/chamadas-lideranca")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin2))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    payloadPresenca(alpha.getId(), lider.getId(), "DISCIPULADOR", "AUSENTE", true)))
        .andExpect(status().isOk());

    JsonNode depois = discipuladoPorNome(consultar(tokenAdmin2), "Alpha CL");
    assertThat(situacao(depois, lider.getId())).isEqualTo("AUSENTE");
  }

  @Test
  void confirmacaoMovePresencaParaOutroDiscipulado() throws Exception {
    Discipulado beta = criarBetaComMesmoLider();
    String token = token(admin);
    mvc.perform(
            put("/api/v1/chamadas-lideranca")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadPresenca(alpha.getId(), lider.getId(), "DISCIPULADOR", "PRESENTE")))
        .andExpect(status().isOk());

    mvc.perform(
            put("/api/v1/chamadas-lideranca")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    payloadPresenca(beta.getId(), lider.getId(), "DISCIPULADOR", "AUSENTE", true)))
        .andExpect(status().isOk());

    JsonNode grade = consultar(token);
    assertThat(situacao(discipuladoPorNome(grade, "Alpha CL"), lider.getId())).isNull();
    assertThat(situacao(discipuladoPorNome(grade, "Beta CL"), lider.getId())).isEqualTo("AUSENTE");
    assertThat(
            presenca(discipuladoPorNome(grade, "Alpha CL"), lider.getId())
                .get("registroDoDia")
                .get("discipuladoNome")
                .asText())
        .isEqualTo("Beta CL");
  }

  @Test
  void reenvioIdenticoNaoPedeConfirmacao() throws Exception {
    String token = token(admin);
    mvc.perform(
            put("/api/v1/chamadas-lideranca")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadPresenca(alpha.getId(), lider.getId(), "DISCIPULADOR", "PRESENTE")))
        .andExpect(status().isOk());

    mvc.perform(
            put("/api/v1/chamadas-lideranca")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadPresenca(alpha.getId(), lider.getId(), "DISCIPULADOR", "PRESENTE")))
        .andExpect(status().isOk());
  }

  @Test
  void rejeitaMesmaPessoaEmDoisDiscipuladosNoMesmoPayload() throws Exception {
    Discipulado beta = criarBetaComMesmoLider();
    String payload =
        """
        {
          "data": "%s",
          "observacaoGeral": null,
          "discipulados": [
            {
              "discipuladoId": %d,
              "observacao": null,
              "presencas": [
                {"usuarioId": %d, "papel": "DISCIPULADOR", "situacao": "PRESENTE"}
              ]
            },
            {
              "discipuladoId": %d,
              "observacao": null,
              "presencas": [
                {"usuarioId": %d, "papel": "DISCIPULADOR", "situacao": "AUSENTE"}
              ]
            }
          ]
        }
        """
            .formatted(DATA, alpha.getId(), lider.getId(), beta.getId(), lider.getId());

    mvc.perform(
            put("/api/v1/chamadas-lideranca")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(admin)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail")
                .value(org.hamcrest.Matchers.containsString("apenas um lançamento por pessoa")));
  }

  @Test
  void naoAdminRecebeForbidden() throws Exception {
    mvc.perform(
            get("/api/v1/chamadas-lideranca")
                .param("data", DATA.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(token(lider))))
        .andExpect(status().isForbidden());
  }

  private JsonNode consultar(String token) throws Exception {
    String body =
        mvc.perform(
                get("/api/v1/chamadas-lideranca")
                    .param("data", DATA.toString())
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(body);
  }

  private static JsonNode discipuladoPorNome(JsonNode root, String nome) {
    for (JsonNode discipulado : root.get("discipulados")) {
      if (nome.equals(discipulado.get("discipuladoNome").asText())) return discipulado;
    }
    throw new AssertionError("Discipulado não encontrado: " + nome);
  }

  private static String situacao(JsonNode discipulado, long usuarioId) {
    JsonNode valor = presenca(discipulado, usuarioId).get("situacao");
    return valor.isNull() ? null : valor.asText();
  }

  private static JsonNode presenca(JsonNode discipulado, long usuarioId) {
    for (JsonNode item : discipulado.get("presencas")) {
      if (item.get("usuarioId").asLong() == usuarioId) return item;
    }
    throw new AssertionError("Usuário não encontrado: " + usuarioId);
  }

  private Discipulado criarBetaComMesmoLider() {
    return discipulados.saveAndFlush(
        new Discipulado(
            "Beta CL", Sexo.FEMININO, FaixaEtaria.DE_15_MAIS, alpha.getGerencia(), lider));
  }

  private static String payloadPresenca(
      long discipuladoId, long usuarioId, String papel, String situacao) {
    return payloadPresenca(discipuladoId, usuarioId, papel, situacao, null);
  }

  private static String payloadPresenca(
      long discipuladoId,
      long usuarioId,
      String papel,
      String situacao,
      Boolean confirmarAtualizacao) {
    String confirmacao =
        confirmarAtualizacao == null
            ? ""
            : ",\n          \"confirmarAtualizacao\": " + confirmarAtualizacao;
    return """
        {
          "data": "%s",
          "observacaoGeral": null,
          "discipulados": [
            {
              "discipuladoId": %d,
              "observacao": null,
              "presencas": [
                {"usuarioId": %d, "papel": "%s", "situacao": "%s"}
              ]
            }
          ]%s
        }
        """
        .formatted(DATA, discipuladoId, usuarioId, papel, situacao, confirmacao);
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
