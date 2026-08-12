package br.com.sgd.relatorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.sgd.adolescente.Adolescente;
import br.com.sgd.adolescente.AdolescenteRepository;
import br.com.sgd.adolescente.CategoriaAdolescente;
import br.com.sgd.adolescente.ContatosAdolescente;
import br.com.sgd.adolescente.DadosCadastroAdolescente;
import br.com.sgd.adolescente.VinculoAdolescenteDiscipulado;
import br.com.sgd.adolescente.VinculoAdolescenteRepository;
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
class RelatorioAdolescentesHttpTest {
  private static final String SENHA = "senha-inicial-segura";

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository usuarios;
  @Autowired GerenciaRepository gerencias;
  @Autowired DiscipuladoRepository discipulados;
  @Autowired AdolescenteRepository adolescentes;
  @Autowired VinculoAdolescenteRepository vinculos;
  @Autowired PasswordEncoder passwords;

  private User admin;
  private User lider;
  private Discipulado alpha;
  private Discipulado beta;

  @BeforeEach
  void prepararDados() {
    String sufixo = UUID.randomUUID().toString();
    admin = usuario("Admin", "admin-exp-" + sufixo, Role.ADMIN);
    lider = usuario("Líder Alpha", "lider-exp-" + sufixo, Role.DISCIPULADOR);
    User liderBeta = usuario("Líder Beta", "lider-beta-exp-" + sufixo, Role.DISCIPULADOR);
    User gerente = usuario("Gerente", "gerente-exp-" + sufixo, Role.GERENTE);

    Gerencia centro =
        gerencias.saveAndFlush(
            new Gerencia("Centro Export", Sexo.MASCULINO, Set.of(FaixaEtaria.DE_15_MAIS), gerente));
    alpha =
        discipulados.saveAndFlush(
            new Discipulado("Alpha Export", Sexo.MASCULINO, FaixaEtaria.DE_15_MAIS, centro, lider));
    beta =
        discipulados.saveAndFlush(
            new Discipulado(
                "Beta Export", Sexo.FEMININO, FaixaEtaria.DE_15_MAIS, centro, liderBeta));

    Adolescente bia =
        adolescentes.saveAndFlush(adolescente("Bia Export", LocalDate.of(2010, 2, 1), true));
    Adolescente ana =
        adolescentes.saveAndFlush(adolescente("Ana Export", LocalDate.of(2010, 1, 1), false));
    Adolescente carla =
        adolescentes.saveAndFlush(adolescente("Carla Export", LocalDate.of(2011, 3, 1), true));

    vinculos.saveAndFlush(new VinculoAdolescenteDiscipulado(bia, alpha, LocalDate.of(2026, 1, 1)));
    vinculos.saveAndFlush(new VinculoAdolescenteDiscipulado(ana, alpha, LocalDate.of(2026, 1, 1)));
    vinculos.saveAndFlush(new VinculoAdolescenteDiscipulado(carla, beta, LocalDate.of(2026, 1, 1)));
  }

  @Test
  void adminExportaCsvComCabecalhoSemIdsEFiltros() throws Exception {
    String token = token(admin);

    MvcResult todosAtivos =
        mvc.perform(
                get("/api/v1/relatorios/adolescentes/export")
                    .param("ativo", "true")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("adolescentes-")))
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString("text/csv")))
            .andReturn();

    String csvAtivos = csvBody(todosAtivos);
    assertThat(csvAtivos).contains("Nome,Data de nascimento,Idade");
    assertThat(csvAtivos).doesNotContain("Anonimizado");
    assertThat(csvAtivos).doesNotContain("Consentimento em");
    assertThat(csvAtivos).contains("Bia Export");
    assertThat(csvAtivos).contains("Carla Export");
    assertThat(csvAtivos).doesNotContain("Ana Export");
    assertThat(csvAtivos).doesNotContain(",id,");
    assertThat(csvAtivos).contains("Alpha Export");
    assertThat(csvAtivos).contains("Centro Export");
    assertThat(csvAtivos).contains("Líder Alpha");

    MvcResult soAlpha =
        mvc.perform(
                get("/api/v1/relatorios/adolescentes/export")
                    .param("discipuladoId", String.valueOf(alpha.getId()))
                    .param("ativo", "true")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    String csvAlpha = csvBody(soAlpha);
    assertThat(csvAlpha).contains("Bia Export");
    assertThat(csvAlpha).doesNotContain("Carla Export");

    MvcResult inativos =
        mvc.perform(
                get("/api/v1/relatorios/adolescentes/export")
                    .param("ativo", "false")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    String csvInativos = csvBody(inativos);
    assertThat(csvInativos).contains("Ana Export");
    assertThat(csvInativos).doesNotContain("Bia Export");
  }

  @Test
  void naoAdminRecebeForbidden() throws Exception {
    mvc.perform(
            get("/api/v1/relatorios/adolescentes/export")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(lider))))
        .andExpect(status().isForbidden());
  }

  private static String csvBody(MvcResult result) throws Exception {
    byte[] bytes = result.getResponse().getContentAsByteArray();
    String texto = new String(bytes, StandardCharsets.UTF_8);
    if (texto.startsWith("\uFEFF")) texto = texto.substring(1);
    return texto;
  }

  private static Adolescente adolescente(String nome, LocalDate nascimento, boolean ativo) {
    return new Adolescente(
        new DadosCadastroAdolescente(
            nome,
            nascimento,
            "(11) 98888-0000",
            null,
            LocalDate.of(2026, 1, 1),
            CategoriaAdolescente.DISCIPULO,
            null,
            null,
            ContatosAdolescente.de(null, null, null, null, "Responsável", "(11) 90000-0000"),
            false,
            false),
        ativo);
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
