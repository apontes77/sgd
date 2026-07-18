package br.com.sgd.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import java.util.UUID;

public final class TraceIds {
    private TraceIds() {
    }

    public static String currentOrRandom() {
        SpanContext context = Span.current().getSpanContext();
        return context.isValid() ? context.getTraceId() : UUID.randomUUID().toString();
    }
}
