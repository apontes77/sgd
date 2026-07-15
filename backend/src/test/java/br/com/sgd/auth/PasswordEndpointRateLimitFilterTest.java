package br.com.sgd.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PasswordEndpointRateLimitFilterTest {
    @Test
    void allowsTenThenReturns429AndRetryAfter() throws Exception {
        PasswordEndpointRateLimitFilter filter = new PasswordEndpointRateLimitFilter(new ObjectMapper());
        FilterChain chain = mock(FilterChain.class);

        for (int attempt = 0; attempt < PasswordEndpointRateLimitFilter.MAX_REQUESTS; attempt++) {
            MockHttpServletResponse response = invoke(filter, chain, "203.0.113.10", null);
            assertThat(response.getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse limited = invoke(filter, chain, "203.0.113.10", null);

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getHeader("Retry-After")).isNotBlank();
        verify(chain, times(PasswordEndpointRateLimitFilter.MAX_REQUESTS)).doFilter(any(), any());
    }

    @Test
    void trustsForwardedAddressOnlyBehindPrivateProxy() throws Exception {
        PasswordEndpointRateLimitFilter proxyFilter = new PasswordEndpointRateLimitFilter(new ObjectMapper());
        FilterChain proxyChain = mock(FilterChain.class);
        for (int attempt = 0; attempt < PasswordEndpointRateLimitFilter.MAX_REQUESTS; attempt++)
            invoke(proxyFilter, proxyChain, "10.0.0.2", "192.0.2.99, 198.51.100.10");
        assertThat(invoke(proxyFilter, proxyChain, "10.0.0.2", "192.0.2.99, 198.51.100.10").getStatus()).isEqualTo(429);
        assertThat(invoke(proxyFilter, proxyChain, "10.0.0.2", "192.0.2.99, 198.51.100.11").getStatus()).isEqualTo(200);

        PasswordEndpointRateLimitFilter directFilter = new PasswordEndpointRateLimitFilter(new ObjectMapper());
        FilterChain directChain = mock(FilterChain.class);
        for (int attempt = 0; attempt < PasswordEndpointRateLimitFilter.MAX_REQUESTS; attempt++)
            invoke(directFilter, directChain, "203.0.113.20", "198.51.100.10");
        assertThat(invoke(directFilter, directChain, "203.0.113.20", "198.51.100.11").getStatus()).isEqualTo(429);
    }

    private MockHttpServletResponse invoke(PasswordEndpointRateLimitFilter filter, FilterChain chain,
                                           String remoteAddress, String forwardedFor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/autenticacao/esqueci-a-senha");
        request.setRemoteAddr(remoteAddress);
        if (forwardedFor != null) request.addHeader("X-Forwarded-For", forwardedFor);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }
}
