package br.com.sgd.frequencia;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
class FrequenciaHttpTest {
  private static final String SENHA = "senha-inicial-segura";
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository usuarios;
  @Autowired GerenciaRepository gerencias;
  @Autowired DiscipuladoRepository discipulados;
  @Autowired VisitanteRepository visitantes;
  @Autowired EncontroRepository encontros;
  @Autowired PasswordEncoder passwords;

  private User admin;
  private User discipulador;
  private User outroDiscipulador;
  private User coLider;
  private User gerente;
  private Discipulado proprio;
  private Discipulado alheio;

  @BeforeEach
  void prepararEstrutura() {
    String sufixo = UUID.randomUUID().toString();
    admin = usuario("Admin", "admin-" + sufixo, Role.ADMIN);
    discipulador = usuario("Discipulador", "lider-" + sufixo, Role.DISCIPULADOR);
    outroDiscipulador = usuario("Outro", "outro-" + sufixo, Role.DISCIPULADOR);
    coLider = usuario("Co-líder", "colider-" + sufixo, Role.CO_LIDER);
    gerente = usuario("Gerente", "gerente-" + sufixo, Role.GERENTE);
    Gerencia gerencia =
        gerencias.saveAndFlush(
            new Gerencia("Gerência", Sexo.MASCULINO, Set.of(FaixaEtaria.DE_15_MAIS), gerente));
    proprio =
        new Discipulado("Próprio", Sexo.MASCULINO, FaixaEtaria.DE_15_MAIS, gerencia, discipulador);
    proprio.replaceCoLideres(Set.of(coLider));
    proprio = discipulados.saveAndFlush(proprio);
    alheio =
        discipulados.saveAndFlush(
            new Discipulado(
                "Alheio", Sexo.FEMININO, FaixaEtaria.DE_15_MAIS, gerencia, outroDiscipulador));
  }

  @Test
  void restringeLideresAoProprioDiscipuladoEPermiteAdministradorEmTodos() throws Exception {
    String tokenDiscipulador = token(discipulador);
    String tokenCoLider = token(coLider);
    String tokenAdmin = token(admin);
    String tokenGerente = token(gerente);

    long encontroId = criarEncontro(tokenDiscipulador, proprio.getId(), "2026-07-13", 201);
    mvc.perform(
            get("/api/v1/encontros/{id}/frequencias", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador)))
        .andExpect(status().isOk());
    criarEncontro(tokenDiscipulador, alheio.getId(), "2026-07-13", 403);
    criarEncontro(tokenCoLider, proprio.getId(), "2026-07-14", 201);
    criarEncontro(tokenCoLider, alheio.getId(), "2026-07-14", 403);
    criarEncontro(tokenGerente, proprio.getId(), "2026-07-15", 403);
    criarEncontro(tokenAdmin, proprio.getId(), "2026-07-16", 201);
    criarEncontro(tokenAdmin, alheio.getId(), "2026-07-16", 201);

    mvc.perform(
            get("/api/v1/discipulados/liderados")
                .param("ativo", "true")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(proprio.getId()));
    mvc.perform(
            get("/api/v1/discipulados/liderados")
                .param("ativo", "true")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenCoLider)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(proprio.getId()));
  }

  @Test
  void adminEDiscipuladorRegistramNaoRealizadoComJustificativaNoProprioEscopo() throws Exception {
    String tokenAdmin = token(admin);
    String tokenDiscipulador = token(discipulador);
    String tokenCoLider = token(coLider);

    mvc.perform(
            post("/api/v1/encontros")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"discipuladoId\":"
                        + proprio.getId()
                        + ",\"data\":\"2026-07-18\",\"situacao\":\"NAO_REALIZADO\"}"))
        .andExpect(status().isBadRequest());

    String response =
        mvc.perform(
                post("/api/v1/encontros")
                    .header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"discipuladoId\":"
                            + proprio.getId()
                            + ",\"data\":\"2026-07-18\",\"situacao\":\"NAO_REALIZADO\",\"justificativa\":\"  Líder doente  \"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.situacao").value("NAO_REALIZADO"))
            .andExpect(jsonPath("$.justificativa").value("Líder doente"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long encontroId = json.readTree(response).get("id").asLong();

    mvc.perform(
            post("/api/v1/encontros")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenCoLider))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"discipuladoId\":"
                        + proprio.getId()
                        + ",\"data\":\"2026-07-19\",\"situacao\":\"NAO_REALIZADO\",\"justificativa\":\"Imprevisto\"}"))
        .andExpect(status().isForbidden());

    mvc.perform(
            post("/api/v1/encontros")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"discipuladoId\":"
                        + alheio.getId()
                        + ",\"data\":\"2026-07-19\",\"situacao\":\"NAO_REALIZADO\",\"justificativa\":\"Imprevisto\"}"))
        .andExpect(status().isForbidden());

    mvc.perform(
            patch("/api/v1/encontros/{id}", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"situacao\":\"NAO_REALIZADO\",\"justificativa\":\"Imprevisto resolvido\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.justificativa").value("Imprevisto resolvido"));

    mvc.perform(
            get("/api/v1/encontros")
                .param("discipuladoId", proprio.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].justificativa").value("Imprevisto resolvido"));

    mvc.perform(
            patch("/api/v1/encontros/{id}", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"situacao\":\"REALIZADO\"}"))
        .andExpect(status().isForbidden());

    mvc.perform(
            patch("/api/v1/encontros/{id}", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"situacao\":\"REALIZADO\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.justificativa").doesNotExist());

    mvc.perform(
            post("/api/v1/encontros")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"discipuladoId\":"
                        + alheio.getId()
                        + ",\"data\":\"2026-07-20\",\"situacao\":\"NAO_REALIZADO\",\"justificativa\":\"Ausência justificada\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  void permiteObservacaoEmEncontroRealizado() throws Exception {
    String token = token(discipulador);
    long encontroId = criarEncontro(token, proprio.getId(), "2026-07-21", 201);

    mvc.perform(
            patch("/api/v1/encontros/{id}", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"observacao\":\"  Foi colocada a frequência, mas o menino só chegou na hora do culto  \"}"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.observacao")
                .value("Foi colocada a frequência, mas o menino só chegou na hora do culto"));

    mvc.perform(
            get("/api/v1/encontros")
                .param("discipuladoId", proprio.getId().toString())
                .param("dataInicio", "2026-07-21")
                .param("dataFim", "2026-07-21")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[0].observacao")
                .value("Foi colocada a frequência, mas o menino só chegou na hora do culto"));
  }

  @Test
  void discipuloGoeEVisitanteEntramNaChamadaSomenteComoPresentesOptIn() throws Exception {
    String token = token(discipulador);
    long anaId = criarAdolescente(token, "Ana");
    long goeId = criarDiscipuloGoe(token, "Goe");
    long encontroId = criarEncontro(token, proprio.getId(), "2026-06-01", 201);

    salvarChamada(
        token,
        encontroId,
        "{\"frequencias\":[{\"adolescenteId\":" + anaId + ",\"situacao\":\"PRESENTE\"}]}",
        200);
    salvarChamada(
        token,
        encontroId,
        "{\"frequencias\":[{\"adolescenteId\":"
            + anaId
            + ",\"situacao\":\"PRESENTE\"},{\"adolescenteId\":"
            + goeId
            + ",\"situacao\":\"AUSENTE\"}]}",
        400);
    salvarChamada(
        token,
        encontroId,
        "{\"frequencias\":[{\"adolescenteId\":"
            + anaId
            + ",\"situacao\":\"PRESENTE\"},{\"adolescenteId\":"
            + goeId
            + ",\"situacao\":\"PRESENTE\"}]}",
        200);

    mvc.perform(
            get("/api/v1/encontros/{id}/frequencias", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[1].adolescenteNome").value("Goe"))
        .andExpect(jsonPath("$[1].categoria").value("DISCIPULO_GOE"))
        .andExpect(jsonPath("$[1].situacao").value("PRESENTE"));

    salvarChamada(
        token,
        encontroId,
        "{\"frequencias\":[{\"adolescenteId\":" + anaId + ",\"situacao\":\"PRESENTE\"}]}",
        200);
    mvc.perform(
            get("/api/v1/encontros/{id}/frequencias", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].adolescenteId").value(anaId));
  }

  @Test
  void usaVinculosAtuaisEmEncontroPassadoEPreservaFrequenciaAnterior() throws Exception {
    String token = token(discipulador);
    long anaId = criarAdolescente(token, "Ana");
    long biaId = criarAdolescente(token, "Bia");
    long encontroId = criarEncontro(token, proprio.getId(), "2026-06-01", 201);

    salvarChamada(token, encontroId, chamada(anaId, biaId), 200);
    mvc.perform(
            patch("/api/v1/adolescentes/{id}", biaId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adolescente("Bia", false)))
        .andExpect(status().isOk());

    salvarChamada(token, encontroId, chamada(anaId, biaId), 200);
    salvarChamada(
        token,
        encontroId,
        "{\"frequencias\":[{\"adolescenteId\":" + anaId + ",\"situacao\":\"PRESENTE\"}]}",
        409);
    salvarChamada(
        token,
        encontroId,
        "{\"frequencias\":[{\"adolescenteId\":"
            + anaId
            + ",\"situacao\":\"PRESENTE\"},{\"adolescenteId\":999999,\"situacao\":\"AUSENTE\"}]}",
        409);
    mvc.perform(
            get("/api/v1/encontros/{id}/frequencias", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void discipuladorECoLiderRegistramVisitanteComoAdolescenteNaFrequencia() throws Exception {
    String tokenDiscipulador = token(discipulador);
    String tokenCoLider = token(coLider);

    // Discipulador: encontro de hoje, visitante com dataInicio na data do encontro, chamada salva
    // com o visitante.
    String hoje = java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).toString();
    long encontroId = criarEncontro(tokenDiscipulador, proprio.getId(), hoje, 201);
    long visitanteId = criarVisitante(tokenDiscipulador, "Visitante Um", hoje);
    salvarChamada(
        tokenDiscipulador,
        encontroId,
        "{\"frequencias\":[{\"adolescenteId\":" + visitanteId + ",\"situacao\":\"PRESENTE\"}]}",
        200);
    mvc.perform(
            get("/api/v1/encontros/{id}/frequencias", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].adolescenteId").value(visitanteId))
        .andExpect(jsonPath("$[0].situacao").value("PRESENTE"));

    // Co-líder: mesmo fluxo em uma data futura (encontro agendado), incluindo o adolescente já
    // vinculado.
    String dataFutura = java.time.LocalDate.now().plusDays(7).toString();
    long outroEncontroId = criarEncontro(tokenCoLider, proprio.getId(), dataFutura, 201);
    long segundoVisitanteId = criarVisitante(tokenCoLider, "Visitante Dois", dataFutura);
    salvarChamada(
        tokenCoLider,
        outroEncontroId,
        "{\"frequencias\":[{\"adolescenteId\":"
            + segundoVisitanteId
            + ",\"situacao\":\"PRESENTE\"}]}",
        200);
    mvc.perform(
            get("/api/v1/encontros/{id}/frequencias", outroEncontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenCoLider)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].adolescenteId").value(segundoVisitanteId))
        .andExpect(jsonPath("$[0].categoria").value("VISITANTE"));
  }

  @Test
  void consultaVisitantesRespeitaEscopoERetornaZeroQuandoNaoHaRegistro() throws Exception {
    String tokenDiscipulador = token(discipulador);
    String tokenOutro = token(outroDiscipulador);
    String tokenGerente = token(gerente);
    long encontroId = criarEncontro(tokenDiscipulador, proprio.getId(), "2026-07-21", 201);

    mvc.perform(
            get("/api/v1/encontros/{id}/visitantes", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantidade").value(0));

    visitantes.saveAndFlush(
        new Visitante(encontros.findById(encontroId).orElseThrow(), 5, java.time.Instant.now()));

    mvc.perform(
            get("/api/v1/encontros/{id}/visitantes", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantidade").value(5));
    mvc.perform(
            get("/api/v1/encontros/{id}/visitantes", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantidade").value(5));
    mvc.perform(
            get("/api/v1/encontros/{id}/visitantes", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenOutro)))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/encontros/{id}/visitantes", 999999L)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador)))
        .andExpect(status().isNotFound());
  }

  @Test
  void adminReverteFechamentoAutomaticoMantemFlagEPermiteChamada() throws Exception {
    Encontro fechado =
        new Encontro(
            proprio,
            LocalDate.of(2026, 7, 17),
            SituacaoEncontro.NAO_REALIZADO,
            PrazoLancamentoFrequencia.JUSTIFICATIVA_AUTOMATICA,
            Instant.parse("2026-07-20T06:20:00Z"));
    fechado.marcarFechamentoAutomatico();
    fechado = encontros.saveAndFlush(fechado);
    String tokenAdmin = token(admin);

    mvc.perform(
            patch("/api/v1/encontros/{id}", fechado.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"situacao\":\"REALIZADO\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.situacao").value("REALIZADO"))
        .andExpect(jsonPath("$.fechamentoAutomatico").value(true))
        .andExpect(jsonPath("$.justificativa").doesNotExist());

    long adolescenteId = criarAdolescente(tokenAdmin, "Ana");
    salvarChamada(
        tokenAdmin,
        fechado.getId(),
        "{\"frequencias\":[{\"adolescenteId\":" + adolescenteId + ",\"situacao\":\"PRESENTE\"}]}",
        200);

    mvc.perform(
            get("/api/v1/encontros")
                .param("discipuladoId", String.valueOf(proprio.getId()))
                .param("dataInicio", "2026-07-17")
                .param("dataFim", "2026-07-17")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].fechamentoAutomatico").value(true))
        .andExpect(jsonPath("$[0].situacao").value("REALIZADO"));
  }

  @Test
  void adminExcluiEncontroELiberaData_outrosPerfisRecebem403() throws Exception {
    String tokenAdmin = token(admin);
    String tokenDiscipulador = token(discipulador);
    String tokenGerente = token(gerente);
    long encontroId = criarEncontro(tokenAdmin, proprio.getId(), "2026-07-10", 201);
    long adolescenteId = criarAdolescente(tokenAdmin, "Bruno");
    salvarChamada(
        tokenAdmin,
        encontroId,
        "{\"frequencias\":[{\"adolescenteId\":" + adolescenteId + ",\"situacao\":\"PRESENTE\"}]}",
        200);

    mvc.perform(
            delete("/api/v1/encontros/{id}", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador)))
        .andExpect(status().isForbidden());
    mvc.perform(
            delete("/api/v1/encontros/{id}", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente)))
        .andExpect(status().isForbidden());

    mvc.perform(
            delete("/api/v1/encontros/{id}", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
        .andExpect(status().isNoContent());

    mvc.perform(
            get("/api/v1/encontros/{id}/frequencias", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
        .andExpect(status().isNotFound());

    criarEncontro(tokenAdmin, proprio.getId(), "2026-07-10", 201);
  }

  private User usuario(String nome, String prefixo, Role role) {
    return usuarios.saveAndFlush(
        new User(nome, prefixo + "@sgd.local", passwords.encode(SENHA), Set.of(role)));
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

  private long criarEncontro(String token, long discipuladoId, String data, int statusEsperado)
      throws Exception {
    String response =
        mvc.perform(
                post("/api/v1/encontros")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"discipuladoId\":"
                            + discipuladoId
                            + ",\"data\":\""
                            + data
                            + "\",\"situacao\":\"REALIZADO\"}"))
            .andExpect(status().is(statusEsperado))
            .andReturn()
            .getResponse()
            .getContentAsString();
    if (statusEsperado != 201) return 0;
    return json.readTree(response).get("id").asLong();
  }

  private long criarVisitante(String token, String nome, String dataInicio) throws Exception {
    String response =
        mvc.perform(
                post("/api/v1/adolescentes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"nome\":\""
                            + nome
                            + "\",\"dataNascimento\":\"2011-05-04\",\"categoria\":\"VISITANTE\",\"consentimentoEm\":\"2026-01-01\",\"naoPossuiTelefone\":true,\"discipuladoId\":"
                            + proprio.getId()
                            + ",\"ativo\":true,\"dataInicio\":\""
                            + dataInicio
                            + "\",\"familia\":"
                            + familiaJson()
                            + "}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(response).get("id").asLong();
  }

  private long criarAdolescente(String token, String nome) throws Exception {
    String response =
        mvc.perform(
                post("/api/v1/adolescentes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(adolescente(nome, true)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(response).get("id").asLong();
  }

  private long criarDiscipuloGoe(String token, String nome) throws Exception {
    String response =
        mvc.perform(
                post("/api/v1/adolescentes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"nome\":\""
                            + nome
                            + "\",\"dataNascimento\":\"2010-01-01\",\"telefone\":\"(11) 91234-5678\",\"categoria\":\"DISCIPULO_GOE\",\"motivoAfastamento\":\"Afastou-se\",\"consentimentoEm\":\"2026-01-01\",\"discipuladoId\":"
                            + proprio.getId()
                            + ",\"ativo\":true,\"familia\":"
                            + familiaJson()
                            + "}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(response).get("id").asLong();
  }

  private void salvarChamada(String token, long encontroId, String body, int statusEsperado)
      throws Exception {
    mvc.perform(
            put("/api/v1/encontros/{id}/frequencias", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().is(statusEsperado));
  }

  private String adolescente(String nome, boolean ativo) {
    return "{\"nome\":\""
        + nome
        + "\",\"dataNascimento\":\"2010-01-01\",\"categoria\":\"DISCIPULO\",\"consentimentoEm\":\"2026-01-01\",\"naoPossuiTelefone\":true,\"discipuladoId\":"
        + proprio.getId()
        + ",\"ativo\":"
        + ativo
        + ",\"familia\":"
        + familiaJson()
        + "}";
  }

  private static String familiaJson() {
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

  private static String chamada(long primeiro, long segundo) {
    return "{\"frequencias\":[{\"adolescenteId\":"
        + primeiro
        + ",\"situacao\":\"PRESENTE\"},{\"adolescenteId\":"
        + segundo
        + ",\"situacao\":\"AUSENTE\"}]}";
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }
}
