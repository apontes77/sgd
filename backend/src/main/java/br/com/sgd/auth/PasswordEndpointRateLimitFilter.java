package br.com.sgd.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.sgd.observability.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Single-instance IP limiter for the two unauthenticated password endpoints. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class PasswordEndpointRateLimitFilter extends OncePerRequestFilter {
    static final int MAX_REQUESTS = 10;
    static final long WINDOW_SECONDS = 60;
    private static final String FORGOT = "/api/v1/autenticacao/esqueci-a-senha";
    private static final String RESET = "/api/v1/autenticacao/redefinir-senha";

    private final ObjectMapper json;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public PasswordEndpointRateLimitFilter(ObjectMapper json) { this.json = json; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI();
        return !FORGOT.equals(path) && !RESET.equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = Instant.now().getEpochSecond();
        Decision decision = consume(clientIp(request), now);
        if (decision.allowed()) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getOutputStream(), Map.of(
                "type", "about:blank", "title", "Too Many Requests", "status", 429,
                "detail", "Muitas tentativas. Tente novamente mais tarde.",
                "traceId", TraceContext.currentTraceId()));
    }

    private String clientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        try {
            InetAddress proxy = InetAddress.getByName(remote);
            if (proxy.isLoopbackAddress() || proxy.isSiteLocalAddress()) {
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null) {
                    String[] hops = forwarded.split(",");
                    String candidate = hops[hops.length - 1].trim();
                    if (!candidate.isBlank() && candidate.length() <= 45 && candidate.matches("[0-9a-fA-F:.]+"))
                        return candidate;
                }
            }
        } catch (Exception ignored) {
            // Servlet containers normally provide an IP literal. Fall back safely if they do not.
        }
        return remote;
    }

    private Decision consume(String ip, long now) {
        Decision[] result = new Decision[1];
        windows.compute(ip, (key, current) -> {
            if (current == null || now >= current.startedAt() + WINDOW_SECONDS) {
                result[0] = new Decision(true, 0);
                return new Window(now, 1);
            }
            if (current.count() >= MAX_REQUESTS) {
                result[0] = new Decision(false, Math.max(1, current.startedAt() + WINDOW_SECONDS - now));
                return current;
            }
            result[0] = new Decision(true, 0);
            return new Window(current.startedAt(), current.count() + 1);
        });
        if (windows.size() > 10_000) windows.entrySet().removeIf(entry -> now >= entry.getValue().startedAt() + WINDOW_SECONDS);
        return result[0];
    }

    private record Window(long startedAt, int count) { }
    private record Decision(boolean allowed, long retryAfterSeconds) { }
}
