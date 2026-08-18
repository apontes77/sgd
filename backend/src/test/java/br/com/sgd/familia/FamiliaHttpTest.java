package br.com.sgd.familia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class FamiliaHttpTest {
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
  private User liderBeta;
  private Discipulado alpha;
  private Discipulado betaOutraGerencia;

  @BeforeEach
  void preparar() {
    String s = UUID.randomUUID().toString();
    admin = usuario("Admin", "admin-fam-" + s, Role.ADMIN);
    gerente = usuario("Gerente", "gerente-fam-" + s, Role.GERENTE);
    outroGerente = usuario("Outro Gerente", "outro-gerente-fam-" + s, Role.GERENTE);
    lider = usuario("Líder", "lider-fam-" + s, Role.DISCIPULADOR);
    liderBeta = usuario("Líder Beta", "lider-beta-fam-" + s, Role.DISCIPULADOR);

    Gerencia centro =
        gerencias.saveAndFlush(
            new Gerencia("Centro Fam", Sexo.MASCULINO, Set.of(FaixaEtaria.DE_15_MAIS), gerente));
    Gerencia outra =
        gerencias.saveAndFlush(
            new Gerencia("Outra Fam", Sexo.FEMININO, Set.of(FaixaEtaria.DE_15_MAIS), outroGerente));
    alpha =
        discipulados.saveAndFlush(
            new Discipulado("Alpha Fam", Sexo.MASCULINO, FaixaEtaria.DE_15_MAIS, centro, lider));
    betaOutraGerencia =
        discipulados.saveAndFlush(
            new Discipulado("Beta Fam", Sexo.FEMININO, FaixaEtaria.DE_15_MAIS, outra, liderBeta));
  }

  @Test
  void postSemFamiliaRetornaBadRequest() throws Exception {
    String semFamilia =
        """
        {
          "nome": "Sem Ficha",
          "dataNascimento": "2010-01-15",
          "consentimentoEm": "2026-01-01",
          "categoria": "DISCIPULO",
          "discipuladoId": %d,
          "ativo": true,
          "dataInicio": "2026-01-01",
          "naoPossuiTelefone": true
        }
        """
            .formatted(alpha.getId());

    mvc.perform(
            post("/api/v1/adolescentes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(admin)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(semFamilia))
        .andExpect(status().isBadRequest());
  }

  @Test
  void liderConsultaEAtualizaFamiliaDoProprioDiscipulado() throws Exception {
    long adolescenteId = criarAdolescente(lider, alpha.getId(), "Ana Familia");

    mvc.perform(
            get("/api/v1/adolescentes/{id}/familia", adolescenteId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token(lider))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.adolescenteId").value(adolescenteId));

    mvc.perform(
            put("/api/v1/adolescentes/{id}/familia", adolescenteId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token(lider)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(familiaPreenchidaJson("Maria Mãe")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.responsavel1.nome").value("Maria Mãe"))
        .andExpect(jsonPath("$.situacaoFicha").value("PREENCHIDA"));
  }

  @Test
  void liderRecebeForbiddenForaDoDiscipulado() throws Exception {
    long adolescenteId = criarAdolescente(admin, betaOutraGerencia.getId(), "Fora Escopo");

    mvc.perform(
            get("/api/v1/adolescentes/{id}/familia", adolescenteId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token(lider))))
        .andExpect(status().isForbidden());

    mvc.perform(
            put("/api/v1/adolescentes/{id}/familia", adolescenteId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token(lider)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(familiaNaoConstaJson()))
        .andExpect(status().isForbidden());
  }

  @Test
  void gerenteConsultaEAtualizaNaGerenciaEForbiddenFora() throws Exception {
    long naGerencia = criarAdolescente(gerente, alpha.getId(), "Na Gerencia");
    long fora = criarAdolescente(admin, betaOutraGerencia.getId(), "Outra Gerencia");

    mvc.perform(
            get("/api/v1/adolescentes/{id}/familia", naGerencia)
                .header(HttpHeaders.AUTHORIZATION, bearer(token(gerente))))
        .andExpect(status().isOk());

    mvc.perform(
            put("/api/v1/adolescentes/{id}/familia", naGerencia)
                .header(HttpHeaders.AUTHORIZATION, bearer(token(gerente)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(familiaPreenchidaJson("Pai Gerente")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.responsavel1.nome").value("Pai Gerente"));

    mvc.perform(
            get("/api/v1/adolescentes/{id}/familia", fora)
                .header(HttpHeaders.AUTHORIZATION, bearer(token(gerente))))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminAcessaFamiliaEmQualquerGerencia() throws Exception {
    long fora = criarAdolescente(admin, betaOutraGerencia.getId(), "Admin Global Fam");

    mvc.perform(
            get("/api/v1/adolescentes/{id}/familia", fora)
                .header(HttpHeaders.AUTHORIZATION, bearer(token(admin))))
        .andExpect(status().isOk());

    mvc.perform(
            put("/api/v1/adolescentes/{id}/familia", fora)
                .header(HttpHeaders.AUTHORIZATION, bearer(token(admin)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(familiaPreenchidaJson("Resp Admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.responsavel1.nome").value("Resp Admin"));
  }

  @Test
  void listagemFamiliasSomenteAdminEGerenteNoEscopo() throws Exception {
    criarAdolescente(gerente, alpha.getId(), "Lista Alpha");
    criarAdolescente(admin, betaOutraGerencia.getId(), "Lista Beta");

    JsonNode adminLista =
        json.readTree(
            mvc.perform(
                    get("/api/v1/familias")
                        .param("page", "0")
                        .param("size", "50")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(admin))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(adminLista.get("content").toString()).contains("Lista Alpha");
    assertThat(adminLista.get("content").toString()).contains("Lista Beta");

    JsonNode gerenteLista =
        json.readTree(
            mvc.perform(
                    get("/api/v1/familias")
                        .param("page", "0")
                        .param("size", "50")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(gerente))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(gerenteLista.get("content").toString()).contains("Lista Alpha");
    assertThat(gerenteLista.get("content").toString()).doesNotContain("Lista Beta");

    mvc.perform(get("/api/v1/familias").header(HttpHeaders.AUTHORIZATION, bearer(token(lider))))
        .andExpect(status().isForbidden());
  }

  private long criarAdolescente(User ator, long discipuladoId, String nome) throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/adolescentes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token(ator)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payloadCriacao(discipuladoId, nome)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(body).get("id").asLong();
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

  private static String familiaPreenchidaJson(String nomeResponsavel1) {
    return """
        {
          "cep": "74000000",
          "rua": "Rua Teste",
          "numero": "10",
          "complemento": "Casa",
          "bairro": "Centro",
          "cidade": "Goiânia",
          "situacaoIgreja": "NAO_CONSTA",
          "atuaOnde": "Não consta",
          "situacaoPais": "CASADOS",
          "descricao": "Família acolhedora",
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
            "nome": "%s",
            "parentesco": "Mãe",
            "estadoCivil": "Casada",
            "profissao": "Professora",
            "telefone": "62999998888",
            "email": "mae@exemplo.com",
            "interessePessoal": "Música"
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
        """
        .formatted(nomeResponsavel1);
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
