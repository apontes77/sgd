package br.com.sgd.frequencia;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.organizacao.Gerencia;
import br.com.sgd.organizacao.GerenciaRepository;
import br.com.sgd.organizacao.Sexo;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        Gerencia gerencia = gerencias.saveAndFlush(new Gerencia("Gerência", gerente));
        proprio = new Discipulado("Próprio", Sexo.MASCULINO, gerencia, discipulador);
        proprio.replaceCoLideres(Set.of(coLider));
        proprio = discipulados.saveAndFlush(proprio);
        alheio = discipulados.saveAndFlush(new Discipulado("Alheio", Sexo.FEMININO, gerencia, outroDiscipulador));
    }

    @Test
    void restringeLideresAoProprioDiscipuladoEPermiteAdministradorEmTodos() throws Exception {
        String tokenDiscipulador = token(discipulador);
        String tokenCoLider = token(coLider);
        String tokenAdmin = token(admin);
        String tokenGerente = token(gerente);

        long encontroId = criarEncontro(tokenDiscipulador, proprio.getId(), "2026-07-13", 201);
        mvc.perform(get("/api/v1/encontros/{id}/frequencias", encontroId).header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador)))
            .andExpect(status().isOk());
        criarEncontro(tokenDiscipulador, alheio.getId(), "2026-07-13", 403);
        criarEncontro(tokenCoLider, proprio.getId(), "2026-07-14", 201);
        criarEncontro(tokenCoLider, alheio.getId(), "2026-07-14", 403);
        criarEncontro(tokenGerente, proprio.getId(), "2026-07-15", 403);
        criarEncontro(tokenAdmin, proprio.getId(), "2026-07-16", 201);
        criarEncontro(tokenAdmin, alheio.getId(), "2026-07-16", 201);

        mvc.perform(get("/api/v1/discipulados/liderados").param("ativo", "true").header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(proprio.getId()));
        mvc.perform(get("/api/v1/discipulados/liderados").param("ativo", "true").header(HttpHeaders.AUTHORIZATION, bearer(tokenCoLider)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(proprio.getId()));
    }

    @Test
    void adminEDiscipuladorRegistramNaoRealizadoComJustificativaNoProprioEscopo() throws Exception {
        String tokenAdmin = token(admin);
        String tokenDiscipulador = token(discipulador);
        String tokenCoLider = token(coLider);

        mvc.perform(post("/api/v1/encontros").header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"discipuladoId\":" + proprio.getId() + ",\"data\":\"2026-07-18\",\"situacao\":\"CANCELADO\"}"))
            .andExpect(status().isBadRequest());

        String response = mvc.perform(post("/api/v1/encontros").header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"discipuladoId\":" + proprio.getId() + ",\"data\":\"2026-07-18\",\"situacao\":\"CANCELADO\",\"justificativa\":\"  Líder doente  \"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.situacao").value("CANCELADO"))
            .andExpect(jsonPath("$.justificativa").value("Líder doente"))
            .andReturn().getResponse().getContentAsString();
        long encontroId = json.readTree(response).get("id").asLong();

        mvc.perform(post("/api/v1/encontros").header(HttpHeaders.AUTHORIZATION, bearer(tokenCoLider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"discipuladoId\":" + proprio.getId() + ",\"data\":\"2026-07-19\",\"situacao\":\"CANCELADO\",\"justificativa\":\"Imprevisto\"}"))
            .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/encontros").header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"discipuladoId\":" + alheio.getId() + ",\"data\":\"2026-07-19\",\"situacao\":\"CANCELADO\",\"justificativa\":\"Imprevisto\"}"))
            .andExpect(status().isForbidden());

        mvc.perform(patch("/api/v1/encontros/{id}", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"situacao\":\"CANCELADO\",\"justificativa\":\"Imprevisto resolvido\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.justificativa").value("Imprevisto resolvido"));

        mvc.perform(get("/api/v1/encontros").param("discipuladoId", proprio.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].justificativa").value("Imprevisto resolvido"));

        mvc.perform(patch("/api/v1/encontros/{id}", encontroId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenDiscipulador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"situacao\":\"REALIZADO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.justificativa").doesNotExist());

        mvc.perform(post("/api/v1/encontros").header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"discipuladoId\":" + alheio.getId() + ",\"data\":\"2026-07-20\",\"situacao\":\"CANCELADO\",\"justificativa\":\"Ausência justificada\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void usaVinculosAtuaisEmEncontroPassadoEPreservaFrequenciaAnterior() throws Exception {
        String token = token(discipulador);
        long anaId = criarAdolescente(token, "Ana");
        long biaId = criarAdolescente(token, "Bia");
        long encontroId = criarEncontro(token, proprio.getId(), "2026-06-01", 201);

        salvarChamada(token, encontroId, chamada(anaId, biaId), 200);
        mvc.perform(patch("/api/v1/adolescentes/{id}", biaId).header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(adolescente("Bia", false)))
            .andExpect(status().isOk());

        salvarChamada(token, encontroId, chamada(anaId, biaId), 200);
        salvarChamada(token, encontroId, "{\"frequencias\":[{\"adolescenteId\":" + anaId + ",\"situacao\":\"PRESENTE\"}]}", 409);
        salvarChamada(token, encontroId, "{\"frequencias\":[{\"adolescenteId\":" + anaId + ",\"situacao\":\"PRESENTE\"},{\"adolescenteId\":999999,\"situacao\":\"AUSENTE\"}]}", 409);
        mvc.perform(get("/api/v1/encontros/{id}/frequencias", encontroId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    private User usuario(String nome, String prefixo, Role role) {
        return usuarios.saveAndFlush(new User(nome, prefixo + "@sgd.local", passwords.encode(SENHA), Set.of(role)));
    }

    private String token(User usuario) throws Exception {
        String response = mvc.perform(post("/api/v1/autenticacao/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + usuario.getEmail() + "\",\"senha\":\"" + SENHA + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("accessToken").asText();
    }

    private long criarEncontro(String token, long discipuladoId, String data, int statusEsperado) throws Exception {
        String response = mvc.perform(post("/api/v1/encontros").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"discipuladoId\":" + discipuladoId + ",\"data\":\"" + data + "\",\"situacao\":\"REALIZADO\"}"))
            .andExpect(status().is(statusEsperado)).andReturn().getResponse().getContentAsString();
        if (statusEsperado != 201) return 0;
        return json.readTree(response).get("id").asLong();
    }

    private long criarAdolescente(String token, String nome) throws Exception {
        String response = mvc.perform(post("/api/v1/adolescentes").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(adolescente(nome, true)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    private void salvarChamada(String token, long encontroId, String body, int statusEsperado) throws Exception {
        mvc.perform(put("/api/v1/encontros/{id}/frequencias", encontroId).header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().is(statusEsperado));
    }

    private String adolescente(String nome, boolean ativo) {
        return "{\"nome\":\"" + nome + "\",\"dataNascimento\":\"2010-01-01\",\"discipuladoId\":" + proprio.getId() + ",\"ativo\":" + ativo + "}";
    }

    private static String chamada(long primeiro, long segundo) {
        return "{\"frequencias\":[{\"adolescenteId\":" + primeiro + ",\"situacao\":\"PRESENTE\"},{\"adolescenteId\":" + segundo + ",\"situacao\":\"AUSENTE\"}]}";
    }

    private static String bearer(String token) { return "Bearer " + token; }
}
