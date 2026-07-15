package br.com.sgd.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestTraceFilterTest {
    @Test
    void exposesTraceIdAndClearsMdcAfterRequest() throws Exception {
        RequestTraceFilter filter = new RequestTraceFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/usuarios");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceDuringRequest = new AtomicReference<>();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            traceDuringRequest.set(MDC.get(TraceContext.MDC_KEY));
            response.setStatus(201);
        });

        assertThat(traceDuringRequest.get()).isNotBlank();
        assertThat(response.getHeader(TraceContext.RESPONSE_HEADER)).isEqualTo(traceDuringRequest.get());
        assertThat(MDC.get(TraceContext.MDC_KEY)).isNull();
    }
}
