package br.com.sgd.observability;

import java.util.UUID;

public final class TraceIds {
  private TraceIds() {}

  public static String currentOrRandom() {
    return UUID.randomUUID().toString();
  }
}
