package br.com.sgd.observability;

import java.util.UUID;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

public final class TraceIds {
  private TraceIds() {}

  public static String currentOrRandom() {
    SpanContext context = Span.current().getSpanContext();
    return context.isValid() ? context.getTraceId() : UUID.randomUUID().toString();
  }
}
