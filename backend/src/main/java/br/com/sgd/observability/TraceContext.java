package br.com.sgd.observability;

import java.util.UUID;
import org.slf4j.MDC;

public final class TraceContext {
    public static final String MDC_KEY = "traceId";
    public static final String RESPONSE_HEADER = "X-Trace-Id";

    private TraceContext() { }

    public static String currentTraceId() {
        String traceId = MDC.get(MDC_KEY);
        return traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId;
    }
}
