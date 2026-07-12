package br.com.sgd.organizacao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EstruturaPersistidaHttpTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    @WithMockUser(roles = "ADMIN")
    void listaUsuariosGerenciasEDiscipuladosJaPersistidos() throws Exception {
        String suffix = UUID.randomUUID().toString();
        long gerenteId = criarUsuario("Gerente persistido", "gerente-" + suffix + "@sgd.local", "GERENTE");
        long discipuladorId = criarUsuario("Discipulador persistido", "discipulador-" + suffix + "@sgd.local", "DISCIPULADOR");
        long coLiderId = criarUsuario("Co-líder persistido", "colider-" + suffix + "@sgd.local", "CO_LIDER");
        long gerenciaId = criar("/api/v1/gerencias", "{\"nome\":\"Gerência Persistida\",\"gerenteId\":" + gerenteId + "}");
        long discipuladoId = criar("/api/v1/discipulados", "{\"nome\":\"Discipulado Persistido\",\"sexo\":\"MASCULINO\",\"gerenciaId\":" + gerenciaId + ",\"discipuladorId\":" + discipuladorId + "}");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/discipulados/{id}/co-lideres", discipuladoId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioIds\":[" + coLiderId + "]}"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/v1/usuarios").param("page", "0").param("size", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.id == " + gerenteId + ")]").isNotEmpty());
        mvc.perform(get("/api/v1/gerencias").param("page", "0").param("size", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.id == " + gerenciaId + ")]").isNotEmpty());
        mvc.perform(get("/api/v1/discipulados").param("page", "0").param("size", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.id == " + discipuladoId + ")]").isNotEmpty())
            .andExpect(jsonPath("$.content[?(@.coLideres[0].id == " + coLiderId + ")]").isNotEmpty());
    }

    private long criarUsuario(String nome, String email, String perfil) throws Exception {
        return criar("/api/v1/usuarios", "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"senha-inicial-segura\",\"perfis\":[\"" + perfil + "\"]}");
    }

    private long criar(String path, String body) throws Exception {
        String response = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode value = json.readTree(response);
        return value.get("id").asLong();
    }
}
