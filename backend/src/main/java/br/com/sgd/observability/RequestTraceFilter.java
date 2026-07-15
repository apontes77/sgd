package br.com.sgd.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTraceFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") || "/api/health".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        MDC.put(TraceContext.MDC_KEY, traceId);
        response.setHeader(TraceContext.RESPONSE_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            LOGGER.info("HTTP request method={} path={} status={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            MDC.remove(TraceContext.MDC_KEY);
        }
    }
}
