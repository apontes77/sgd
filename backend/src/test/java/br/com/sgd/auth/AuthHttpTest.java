package br.com.sgd.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "app.security.admin-email=bootstrap-admin@sgd.local")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthHttpTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwords;
    @MockitoBean private PasswordResetNotifier notifier;

    @Test
    void administradorBootstrapAguardaDefinicaoDeSenha() throws Exception {
        org.assertj.core.api.Assertions.assertThat(users.findByEmailIgnoreCase("bootstrap-admin@sgd.local"))
                .get().extracting(User::isSenhaDefinida).isEqualTo(false);
        mvc.perform(post("/api/v1/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"bootstrap-admin@sgd.local\",\"senha\":\"senha-bootstrap-segura\"}"))
            .andExpect(status().isUnauthorized());
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
        String accessToken = session.get("accessToken").asText();
        String refreshToken = session.get("refreshToken").asText();

        mvc.perform(post("/api/v1/autenticacao/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/autenticacao/atualizar-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointEuExigeAutenticacao() throws Exception {
        mvc.perform(get("/api/v1/autenticacao/eu")).andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroSemSenhaEnviaSetupQuePermiteDefinirSenhaLoginEImpedeReuso() throws Exception {
        clearInvocations(notifier);
        String adminEmail = "admin-flow-" + UUID.randomUUID() + "@sgd.local";
        String adminPassword = "senha-admin-segura";
        users.saveAndFlush(new User("Admin", adminEmail, passwords.encode(adminPassword), Set.of(Role.ADMIN)));
        String adminLogin = mvc.perform(post("/api/v1/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + adminEmail + "\",\"senha\":\"" + adminPassword + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String accessToken = json.readTree(adminLogin).get("accessToken").asText();
        String email = "pending-" + UUID.randomUUID() + "@sgd.local";

        mvc.perform(post("/api/v1/usuarios")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Usuário Pendente\",\"email\":\"" + email + "\",\"perfis\":[\"GERENTE\"]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.senhaDefinida").value(false));

        ArgumentCaptor<String> rawToken = ArgumentCaptor.forClass(String.class);
        verify(notifier).notify(any(User.class), rawToken.capture(),
                eq(PasswordResetToken.Type.DEFINICAO_INICIAL), eq(1440L));
        String resetJson = "{\"token\":\"" + rawToken.getValue() + "\",\"novaSenha\":\"nova-senha-muito-segura\"}";
        mvc.perform(post("/api/v1/autenticacao/redefinir-senha")
                .contentType(MediaType.APPLICATION_JSON).content(resetJson))
            .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"nova-senha-muito-segura\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuario.senhaDefinida").value(true));
        mvc.perform(post("/api/v1/autenticacao/redefinir-senha")
                .contentType(MediaType.APPLICATION_JSON).content(resetJson))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void limitaEndpointsDeSenhaPorIpAtrasDeProxyConfiavel() throws Exception {
        for (int attempt = 0; attempt < PasswordEndpointRateLimitFilter.MAX_REQUESTS; attempt++) {
            mvc.perform(post("/api/v1/autenticacao/esqueci-a-senha")
                    .with(request -> { request.setRemoteAddr("127.0.0.1"); return request; })
                    .header("X-Forwarded-For", "198.51.100.20")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"unknown@example.com\"}"))
                .andExpect(status().isNoContent());
        }
        mvc.perform(post("/api/v1/autenticacao/redefinir-senha")
                .with(request -> { request.setRemoteAddr("127.0.0.1"); return request; })
                .header("X-Forwarded-For", "198.51.100.20")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"invalid\",\"novaSenha\":\"senha-muito-segura\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists("Retry-After"));

        mvc.perform(post("/api/v1/autenticacao/esqueci-a-senha")
                .with(request -> { request.setRemoteAddr("127.0.0.1"); return request; })
                .header("X-Forwarded-For", "198.51.100.21")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unknown@example.com\"}"))
            .andExpect(status().isNoContent());
    }

    @Test
    void rejeitaNomeAntigoPasswordParaEvitarDivergenciaDoContrato() throws Exception {
        var result = mvc.perform(post("/api/v1/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@sgd.local\",\"password\":\"qualquer-senha\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists("X-Trace-Id"))
            .andReturn();
        String traceHeader = result.getResponse().getHeader("X-Trace-Id");
        org.assertj.core.api.Assertions.assertThat(json.readTree(result.getResponse().getContentAsString()).get("traceId").asText())
                .isEqualTo(traceHeader);
    }
}
