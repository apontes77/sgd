package br.com.sgd.organizacao;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EstruturaOrganizacionalHttpTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    @WithMockUser(roles = "ADMIN")
    void listaEstruturaComPaginacaoEstavelEValidaLimites() throws Exception {
        mvc.perform(get("/api/v1/gerencias?page=0&size=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").isNumber())
            .andExpect(jsonPath("$.totalPages").isNumber());

        mvc.perform(get("/api/v1/discipulados?page=-1&size=101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void exigeAutenticacaoParaCriarGerencia() throws Exception {
        mvc.perform(post("/api/v1/gerencias").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Central\",\"gerenteId\":1}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DISCIPULADOR")
    void exigeAdministradorParaAlterarEstrutura() throws Exception {
        mvc.perform(post("/api/v1/gerencias").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Central\",\"gerenteId\":1}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cadastraGerenteESuaGerencia() throws Exception {
        long gerenteId = criarUsuario("Gerente", "gerente@sgd.local", "GERENTE");

        mvc.perform(post("/api/v1/gerencias").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Gerência Central\",\"gerenteId\":" + gerenteId + "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nome").value("Gerência Central"))
            .andExpect(jsonPath("$.gerenteId").value(gerenteId));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cadastraDiscipuladorDiscipuladoECoLider() throws Exception {
        long gerenteId = criarUsuario("Gerente", "gerente2@sgd.local", "GERENTE");
        long gerenciaId = idDaResposta(post("/api/v1/gerencias"),
            "{\"nome\":\"Gerência Norte\",\"gerenteId\":" + gerenteId + "}");
        long discipuladorId = criarUsuario("Discipulador", "discipulador@sgd.local", "DISCIPULADOR");
        long coLiderId = criarUsuario("Co-líder", "colider@sgd.local", "CO_LIDER");
        long discipuladoId = idDaResposta(post("/api/v1/discipulados"),
            "{\"nome\":\"Discipulado Norte\",\"sexo\":\"MASCULINO\",\"gerenciaId\":" + gerenciaId
                + ",\"discipuladorId\":" + discipuladorId + "}");

        mvc.perform(put("/api/v1/discipulados/{id}/co-lideres", discipuladoId).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioIds\":[" + coLiderId + "]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gerenciaId").value(gerenciaId))
            .andExpect(jsonPath("$.discipuladorId").value(discipuladorId))
            .andExpect(jsonPath("$.coLideres[0].id").value(coLiderId));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejeitaPerfilIncorretoComConflitoProblemDetails() throws Exception {
        long usuarioId = criarUsuario("Usuário", "sem-perfil@sgd.local", "CO_LIDER");

        mvc.perform(post("/api/v1/gerencias").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Central\",\"gerenteId\":" + usuarioId + "}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private long criarUsuario(String nome, String email, String perfil) throws Exception {
        return idDaResposta(post("/api/v1/usuarios"), "{\"nome\":\"" + nome + "\",\"email\":\"" + email
            + "\",\"senha\":\"senha-inicial-segura\",\"perfis\":[\"" + perfil + "\"]}");
    }

    private long idDaResposta(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String body) throws Exception {
        String response = mvc.perform(request.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode value = json.readTree(response);
        return value.get("id").asLong();
    }
}
