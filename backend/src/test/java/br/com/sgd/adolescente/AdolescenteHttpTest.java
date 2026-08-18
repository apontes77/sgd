package br.com.sgd.adolescente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class AdolescenteHttpTest {
  private static final String SENHA = "senha-inicial-segura";

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository usuarios;
  @Autowired GerenciaRepository gerencias;
  @Autowired DiscipuladoRepository discipulados;
  @Autowired PasswordEncoder passwords;

  private User admin;
  private User gerente;
  private User outroGerente;
  private User lider;
  private Discipulado alpha;
  private Discipulado betaOutraGerencia;

  @BeforeEach
  void preparar() {
    String s = UUID.randomUUID().toString();
    admin = usuario("Admin", "admin-ad-" + s, Role.ADMIN);
    gerente = usuario("Gerente", "gerente-ad-" + s, Role.GERENTE);
    outroGerente = usuario("Outro Gerente", "outro-gerente-ad-" + s, Role.GERENTE);
    lider = usuario("Líder", "lider-ad-" + s, Role.DISCIPULADOR);
    User liderBeta = usuario("Líder Beta", "lider-beta-ad-" + s, Role.DISCIPULADOR);

    Gerencia centro =
        gerencias.saveAndFlush(
            new Gerencia("Centro AD", Sexo.MASCULINO, Set.of(FaixaEtaria.DE_15_MAIS), gerente));
    Gerencia outra =
        gerencias.saveAndFlush(
            new Gerencia("Outra AD", Sexo.FEMININO, Set.of(FaixaEtaria.DE_15_MAIS), outroGerente));
    alpha =
        discipulados.saveAndFlush(
            new Discipulado("Alpha AD", Sexo.MASCULINO, FaixaEtaria.DE_15_MAIS, centro, lider));
    betaOutraGerencia =
        discipulados.saveAndFlush(
            new Discipulado("Beta AD", Sexo.FEMININO, FaixaEtaria.DE_15_MAIS, outra, liderBeta));
  }

  @Test
  void gerenteCriaAdolescenteNaPropriaGerencia() throws Exception {
    mvc.perform(
            post("/api/v1/adolescentes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(gerente)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadCriacao(alpha.getId(), "Ana Gerente")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nome").value("Ana Gerente"))
        .andExpect(jsonPath("$.discipuladoId").value(alpha.getId()));
  }

  @Test
  void gerenteRecebeForbiddenEmOutraGerencia() throws Exception {
    mvc.perform(
            post("/api/v1/adolescentes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(gerente)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadCriacao(betaOutraGerencia.getId(), "Fora")))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCriaEmQualquerGerencia() throws Exception {
    mvc.perform(
            post("/api/v1/adolescentes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(admin)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadCriacao(betaOutraGerencia.getId(), "Admin Global")))
        .andExpect(status().isCreated());
  }

  @Test
  void gerenteEditaETransfereDentroDaGerenciaMasNaoAnonimiza() throws Exception {
    String criado =
        mvc.perform(
                post("/api/v1/adolescentes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token(gerente)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payloadCriacao(alpha.getId(), "Bia")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = json.readTree(criado).get("id").asLong();

    Discipulado gamma =
        discipulados.saveAndFlush(
            new Discipulado(
                "Gamma AD", Sexo.MASCULINO, FaixaEtaria.DE_15_MAIS, alpha.getGerencia(), lider));

    mvc.perform(
            patch("/api/v1/adolescentes/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer(token(gerente)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadAtualizacao(alpha.getId(), "Bia Editada")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Bia Editada"));

    mvc.perform(
            post("/api/v1/adolescentes/" + id + "/vinculos")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(gerente)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"discipuladoId\":" + gamma.getId() + ",\"dataInicio\":\"2026-06-01\"}"))
        .andExpect(status().isCreated());

    mvc.perform(
            post("/api/v1/adolescentes/" + id + "/vinculos")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(gerente)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"discipuladoId\":"
                        + betaOutraGerencia.getId()
                        + ",\"dataInicio\":\"2026-07-01\"}"))
        .andExpect(status().isForbidden());

    mvc.perform(
            delete("/api/v1/adolescentes/" + id + "/dados-pessoais")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(gerente))))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminAnonimiza() throws Exception {
    String criado =
        mvc.perform(
                post("/api/v1/adolescentes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token(admin)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payloadCriacao(alpha.getId(), "Carla")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = json.readTree(criado).get("id").asLong();

    mvc.perform(
            delete("/api/v1/adolescentes/" + id + "/dados-pessoais")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(admin))))
        .andExpect(status().isNoContent());

    JsonNode lista =
        json.readTree(
            mvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                            "/api/v1/adolescentes")
                        .param("discipuladoId", String.valueOf(alpha.getId()))
                        .param("ativo", "false")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(admin))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(lista.get("content").toString()).contains("Adolescente anonimizado");
  }

  private static String payloadCriacao(long discipuladoId, String nome) {
    return """
        {
          "nome": "%s",
          "dataNascimento": "2010-01-15",
          "consentimentoEm": "2026-01-01",
          "categoria": "DISCIPULO",
          "discipuladoId": %d,
          "ativo": true,
          "dataInicio": "2026-01-01",
          "naoPossuiTelefone": true,
          "familia": %s
        }
        """
        .formatted(nome, discipuladoId, familiaNaoConstaJson());
  }

  private static String payloadAtualizacao(long discipuladoId, String nome) {
    return """
        {
          "nome": "%s",
          "dataNascimento": "2010-01-15",
          "consentimentoEm": "2026-01-01",
          "categoria": "DISCIPULO",
          "discipuladoId": %d,
          "ativo": true,
          "naoPossuiTelefone": true
        }
        """
        .formatted(nome, discipuladoId);
  }

  private static String familiaNaoConstaJson() {
    return """
        {
          "cep": "Não consta",
          "rua": "Não consta",
          "numero": "Não consta",
          "complemento": "Não consta",
          "bairro": "Não consta",
          "cidade": "Não consta",
          "situacaoIgreja": "NAO_CONSTA",
          "atuaOnde": "Não consta",
          "situacaoPais": "NAO_CONSTA",
          "descricao": "Não consta",
          "desafioFinanceiro": false,
          "desafioEmocional": false,
          "desafioEspiritual": false,
          "desafiosDescricao": "Não consta",
          "atividadesJuntas": "Não consta",
          "rotinaSemana": "Não consta",
          "irmaoDokmos": "Não consta",
          "pedidoOracao": "Não consta",
          "intervencao": "Não consta",
          "observacaoDiscipulador": "Não consta",
          "observacaoGerente": "Não consta",
          "responsavel1": {
            "nome": "Não consta",
            "parentesco": "Não consta",
            "estadoCivil": "Não consta",
            "profissao": "Não consta",
            "telefone": "Não consta",
            "email": "Não consta",
            "interessePessoal": "Não consta"
          },
          "responsavel2": {
            "nome": "Não consta",
            "parentesco": "Não consta",
            "estadoCivil": "Não consta",
            "profissao": "Não consta",
            "telefone": "Não consta",
            "email": "Não consta",
            "interessePessoal": "Não consta"
          }
        }
        """;
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
