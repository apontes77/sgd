package br.com.sgd.organizacao;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    mvc.perform(
            post("/api/v1/gerencias")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nome\":\"Central\",\"sexo\":\"MASCULINO\",\"faixasEtarias\":[\"DE_15_MAIS\"],\"gerenteId\":1}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "DISCIPULADOR")
  void exigeAdministradorParaAlterarEstrutura() throws Exception {
    mvc.perform(
            post("/api/v1/gerencias")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nome\":\"Central\",\"sexo\":\"MASCULINO\",\"faixasEtarias\":[\"DE_15_MAIS\"],\"gerenteId\":1}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void cadastraGerenteESuaGerencia() throws Exception {
    long gerenteId = criarUsuario("Gerente", "gerente@sgd.local", "GERENTE");

    mvc.perform(
            post("/api/v1/gerencias")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nome\":\"Gerência Central\",\"sexo\":\"MASCULINO\",\"faixasEtarias\":[\"DE_15_MAIS\",\"DE_13_A_15\"],\"gerenteId\":"
                        + gerenteId
                        + "}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nome").value("Gerência Central"))
        .andExpect(jsonPath("$.sexo").value("MASCULINO"))
        .andExpect(jsonPath("$.faixasEtarias.length()").value(2))
        .andExpect(jsonPath("$.gerenteId").value(gerenteId));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void cadastraDiscipuladorDiscipuladoECoLider() throws Exception {
    long gerenteId = criarUsuario("Gerente", "gerente2@sgd.local", "GERENTE");
    long gerenciaId =
        idDaResposta(
            post("/api/v1/gerencias"),
            "{\"nome\":\"Gerência Norte\",\"sexo\":\"FEMININO\",\"faixasEtarias\":[\"DE_11_A_13\"],\"gerenteId\":"
                + gerenteId
                + "}");
    long discipuladorId = criarUsuario("Discipulador", "discipulador@sgd.local", "DISCIPULADOR");
    long coLiderId = criarUsuario("Co-líder", "colider@sgd.local", "CO_LIDER");
    long discipuladoId =
        idDaResposta(
            post("/api/v1/discipulados"),
            "{\"nome\":\"Discipulado Norte\",\"sexo\":\"MASCULINO\",\"faixaEtaria\":\"DE_09_A_11\",\"gerenciaId\":"
                + gerenciaId
                + ",\"discipuladorId\":"
                + discipuladorId
                + "}");

    mvc.perform(
            put("/api/v1/discipulados/{id}/co-lideres", discipuladoId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioIds\":[" + coLiderId + "]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gerenciaId").value(gerenciaId))
        .andExpect(jsonPath("$.faixaEtaria").value("DE_09_A_11"))
        .andExpect(jsonPath("$.discipuladorId").value(discipuladorId))
        .andExpect(jsonPath("$.discipuladorNome").value("Discipulador"))
        .andExpect(jsonPath("$.coLideres[0].id").value(coLiderId));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void substituiERemoveCoLideresDeDiscipuladoExistente() throws Exception {
    String sufixo = java.util.UUID.randomUUID().toString();
    long gerenteId = criarUsuario("Gerente", "gerente-colider-" + sufixo + "@sgd.local", "GERENTE");
    long gerenciaId =
        idDaResposta(
            post("/api/v1/gerencias"),
            "{\"nome\":\"Gerência Co-líder\",\"sexo\":\"MASCULINO\",\"faixasEtarias\":[\"DE_15_MAIS\"],\"gerenteId\":"
                + gerenteId
                + "}");
    long discipuladorId =
        criarUsuario(
            "Discipulador", "discipulador-colider-" + sufixo + "@sgd.local", "DISCIPULADOR");
    long primeiroCoLiderId =
        criarUsuario("Co-líder A", "colider-a-" + sufixo + "@sgd.local", "CO_LIDER");
    long segundoCoLiderId =
        criarUsuario("Co-líder B", "colider-b-" + sufixo + "@sgd.local", "CO_LIDER");
    long discipuladoId =
        idDaResposta(
            post("/api/v1/discipulados"),
            "{\"nome\":\"Discipulado Co-líder\",\"sexo\":\"MASCULINO\",\"faixaEtaria\":\"DE_15_MAIS\",\"gerenciaId\":"
                + gerenciaId
                + ",\"discipuladorId\":"
                + discipuladorId
                + "}");

    mvc.perform(
            put("/api/v1/discipulados/{id}/co-lideres", discipuladoId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioIds\":[" + primeiroCoLiderId + "]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.coLideres.length()").value(1))
        .andExpect(jsonPath("$.coLideres[0].id").value(primeiroCoLiderId));

    mvc.perform(
            put("/api/v1/discipulados/{id}/co-lideres", discipuladoId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioIds\":[" + segundoCoLiderId + "]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.coLideres.length()").value(1))
        .andExpect(jsonPath("$.coLideres[0].id").value(segundoCoLiderId));

    mvc.perform(get("/api/v1/discipulados").param("page", "0").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.content[?(@.coLideres[0].id == " + segundoCoLiderId + ")]").isNotEmpty());

    mvc.perform(
            put("/api/v1/discipulados/{id}/co-lideres", discipuladoId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioIds\":[]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.coLideres.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void rejeitaPerfilIncorretoComConflitoProblemDetails() throws Exception {
    long usuarioId = criarUsuario("Usuário", "sem-perfil@sgd.local", "CO_LIDER");

    mvc.perform(
            post("/api/v1/gerencias")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nome\":\"Central\",\"sexo\":\"MASCULINO\",\"faixasEtarias\":[\"DE_15_MAIS\"],\"gerenteId\":"
                        + usuarioId
                        + "}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.detail").isNotEmpty())
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void atualizaGerenciaEDiscipuladoParcialmente() throws Exception {
    String sufixo = java.util.UUID.randomUUID().toString();
    long gerenteId = criarUsuario("Gerente", "gerente-patch-" + sufixo + "@sgd.local", "GERENTE");
    long gerenciaId =
        idDaResposta(
            post("/api/v1/gerencias"),
            "{\"nome\":\"Gerencia original\",\"sexo\":\"MASCULINO\",\"faixasEtarias\":[\"DE_15_MAIS\"],\"gerenteId\":"
                + gerenteId
                + "}");
    long discipuladorId =
        criarUsuario("Discipulador", "discipulador-patch-" + sufixo + "@sgd.local", "DISCIPULADOR");
    long discipuladoId =
        idDaResposta(
            post("/api/v1/discipulados"),
            "{\"nome\":\"Discipulado original\",\"sexo\":\"MASCULINO\",\"faixaEtaria\":\"DE_15_MAIS\",\"gerenciaId\":"
                + gerenciaId
                + ",\"discipuladorId\":"
                + discipuladorId
                + "}");

    mvc.perform(
            patch("/api/v1/gerencias/{id}", gerenciaId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nome\":\"Gerencia atualizada\",\"sexo\":\"FEMININO\",\"faixasEtarias\":[\"DE_11_A_13\",\"DE_13_A_15\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Gerencia atualizada"))
        .andExpect(jsonPath("$.sexo").value("FEMININO"))
        .andExpect(jsonPath("$.faixasEtarias.length()").value(2))
        .andExpect(jsonPath("$.gerenteId").value(gerenteId));

    mvc.perform(
            patch("/api/v1/discipulados/{id}", discipuladoId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nome\":\"Discipulado atualizado\",\"sexo\":\"FEMININO\",\"faixaEtaria\":\"DE_09_A_11\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Discipulado atualizado"))
        .andExpect(jsonPath("$.sexo").value("FEMININO"))
        .andExpect(jsonPath("$.faixaEtaria").value("DE_09_A_11"))
        .andExpect(jsonPath("$.gerenciaId").value(gerenciaId));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void filtraGerenciasPorSexoEFaixaEtaria() throws Exception {
    String sufixo = java.util.UUID.randomUUID().toString();
    long gerenteMasculino =
        criarUsuario("Gerente M", "gerente-m-" + sufixo + "@sgd.local", "GERENTE");
    long gerenteFeminino =
        criarUsuario("Gerente F", "gerente-f-" + sufixo + "@sgd.local", "GERENTE");
    long gerenciaMasculina =
        idDaResposta(
            post("/api/v1/gerencias"),
            "{\"nome\":\"Gerencia M\",\"sexo\":\"MASCULINO\",\"faixasEtarias\":[\"DE_09_A_11\",\"DE_11_A_13\"],\"gerenteId\":"
                + gerenteMasculino
                + "}");
    idDaResposta(
        post("/api/v1/gerencias"),
        "{\"nome\":\"Gerencia F\",\"sexo\":\"FEMININO\",\"faixasEtarias\":[\"DE_15_MAIS\"],\"gerenteId\":"
            + gerenteFeminino
            + "}");

    mvc.perform(
            get("/api/v1/gerencias").param("sexo", "MASCULINO").param("faixaEtaria", "DE_11_A_13"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.id == " + gerenciaMasculina + ")]").isNotEmpty())
        .andExpect(jsonPath("$.content[?(@.sexo == 'FEMININO')]").isEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void excluiGerenciaSemDiscipulados() throws Exception {
    String sufixo = java.util.UUID.randomUUID().toString();
    long gerenteId = criarUsuario("Gerente", "gerente-delete-" + sufixo + "@sgd.local", "GERENTE");
    long gerenciaId =
        idDaResposta(
            post("/api/v1/gerencias"),
            "{\"nome\":\"Gerencia vazia\",\"sexo\":\"MASCULINO\",\"faixasEtarias\":[\"DE_15_MAIS\"],\"gerenteId\":"
                + gerenteId
                + "}");

    mvc.perform(delete("/api/v1/gerencias/{id}", gerenciaId).with(csrf()))
        .andExpect(status().isNoContent());

    mvc.perform(get("/api/v1/gerencias").param("page", "0").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.id == " + gerenciaId + ")]").isEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void rejeitaExclusaoDeGerenciaComDiscipulados() throws Exception {
    String sufixo = java.util.UUID.randomUUID().toString();
    long gerenteId =
        criarUsuario("Gerente", "gerente-delete-disc-" + sufixo + "@sgd.local", "GERENTE");
    long gerenciaId =
        idDaResposta(
            post("/api/v1/gerencias"),
            "{\"nome\":\"Gerencia com discipulado\",\"sexo\":\"MASCULINO\",\"faixasEtarias\":[\"DE_15_MAIS\"],\"gerenteId\":"
                + gerenteId
                + "}");
    long discipuladorId =
        criarUsuario(
            "Discipulador", "discipulador-delete-" + sufixo + "@sgd.local", "DISCIPULADOR");
    idDaResposta(
        post("/api/v1/discipulados"),
        "{\"nome\":\"Discipulado vinculado\",\"sexo\":\"MASCULINO\",\"faixaEtaria\":\"DE_15_MAIS\",\"gerenciaId\":"
            + gerenciaId
            + ",\"discipuladorId\":"
            + discipuladorId
            + "}");

    mvc.perform(delete("/api/v1/gerencias/{id}", gerenciaId).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "Os discipulados associados a esta gerência precisam ser realocados ou desativados antes de excluir a gerência."));
  }

  @Test
  @WithMockUser(roles = "DISCIPULADOR")
  void exigeAdministradorParaExcluirGerencia() throws Exception {
    mvc.perform(delete("/api/v1/gerencias/{id}", 1).with(csrf())).andExpect(status().isForbidden());
  }

  private long criarUsuario(String nome, String email, String perfil) throws Exception {
    return idDaResposta(
        post("/api/v1/usuarios"),
        "{\"nome\":\""
            + nome
            + "\",\"email\":\""
            + email
            + "\",\"senha\":\"senha-inicial-segura\",\"perfis\":[\""
            + perfil
            + "\"]}");
  }

  private long idDaResposta(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
      String body)
      throws Exception {
    String response =
        mvc.perform(request.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode value = json.readTree(response);
    return value.get("id").asLong();
  }
}
