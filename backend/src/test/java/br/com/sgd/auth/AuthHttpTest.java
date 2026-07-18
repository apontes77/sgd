package br.com.sgd.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
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

@SpringBootTest(properties = {
    "app.security.admin-email=bootstrap-admin@sgd.local",
    "app.security.admin-password=senha-bootstrap-segura"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthHttpTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwords;

    @Test
    void administradorConfiguradoConsegueEntrarComCredenciaisIniciais() throws Exception {
        mvc.perform(post("/api/v1/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"bootstrap-admin@sgd.local\",\"senha\":\"senha-bootstrap-segura\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuario.email").value("bootstrap-admin@sgd.local"))
            .andExpect(jsonPath("$.usuario.perfis[0]").value("ADMIN"));
    }

    @Test
    void loginUsaCampoSenhaELogoutRevogaRefreshToken() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@sgd.local";
        String password = "senha-inicial-segura";
        users.saveAndFlush(new User("Administrador", email, passwords.encode(password), Set.of(Role.ADMIN)));

        String loginBody = mvc.perform(post("/api/v1/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.usuario.email").value(email))
            .andReturn().getResponse().getContentAsString();
        JsonNode session = json.readTree(loginBody);
        String refreshToken = session.get("refreshToken").asText();

        mvc.perform(post("/api/v1/autenticacao/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/autenticacao/atualizar-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejeitaNomeAntigoPasswordParaEvitarDivergenciaDoContrato() throws Exception {
        mvc.perform(post("/api/v1/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@sgd.local\",\"password\":\"qualquer-senha\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void exigeAutenticacaoParaConsultarUsuarioAtual() throws Exception {
        mvc.perform(get("/api/v1/autenticacao/eu"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void rejeitaTokenInvalidoAoConsultarUsuarioAtual() throws Exception {
        mvc.perform(get("/api/v1/autenticacao/eu")
                .header("Authorization", "Bearer token-invalido"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void permitePreflightDoFrontendConfiguradoParaLogin() throws Exception {
        mvc.perform(options("/api/v1/autenticacao/login")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }
}
