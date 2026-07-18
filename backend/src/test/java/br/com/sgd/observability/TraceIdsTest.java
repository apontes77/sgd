package br.com.sgd.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TraceIdsTest {
    @Test
    void returnsCurrentOpenTelemetryTraceId() {
        String traceId = "0123456789abcdef0123456789abcdef";
        SpanContext context = SpanContext.create(
                traceId,
                "0123456789abcdef",
                TraceFlags.getSampled(),
                TraceState.getDefault());

        try (Scope ignored = Span.wrap(context).makeCurrent()) {
            assertThat(TraceIds.currentOrRandom()).isEqualTo(traceId);
        }
    }

    @Test
    void fallsBackToUuidOutsideATrace() {
        assertThatCodeIsUuid(TraceIds.currentOrRandom());
    }

    private void assertThatCodeIsUuid(String value) {
        assertThat(UUID.fromString(value).toString()).isEqualTo(value);
    }
}
