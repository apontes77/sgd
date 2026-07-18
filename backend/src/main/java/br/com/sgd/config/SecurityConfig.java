package br.com.sgd.config;

import br.com.sgd.auth.AuthService;
import br.com.sgd.auth.JwtService;
import br.com.sgd.observability.TraceIds;
import br.com.sgd.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean ApplicationRunner adminBootstrap(AuthService authService) { return arguments -> authService.bootstrapAdmin(); }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter, ObjectMapper json) throws Exception {
        return http.csrf(csrf -> csrf.disable()).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) ->
                        writeProblem(response, json, HttpServletResponse.SC_UNAUTHORIZED, "Não autorizado", "A autenticação é obrigatória ou expirou."))
                        .accessDeniedHandler((request, response, exception) ->
                        writeProblem(response, json, HttpServletResponse.SC_FORBIDDEN, "Acesso proibido", "Você não possui permissão para realizar esta operação.")))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/autenticacao/**", "/api/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/usuarios/**").hasRole("ADMIN").anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
    }

    private static void writeProblem(HttpServletResponse response, ObjectMapper json, int status, String title, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getOutputStream(), Map.of("type", "about:blank", "title", title, "status", status,
                "detail", detail, "traceId", TraceIds.currentOrRandom()));
    }

    @Component
    static class JwtAuthenticationFilter extends OncePerRequestFilter {
        private final JwtService jwt; private final UserRepository users;
        JwtAuthenticationFilter(JwtService jwt, UserRepository users) { this.jwt = jwt; this.users = users; }
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwt.isValid(token)) users.findByEmailIgnoreCase(jwt.subject(token)).filter(br.com.sgd.user.User::isAtivo).ifPresent(user -> {
                    var authorities = user.getPerfis().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.name())).toList();
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
            chain.doFilter(request, response);
        }
    }
}
