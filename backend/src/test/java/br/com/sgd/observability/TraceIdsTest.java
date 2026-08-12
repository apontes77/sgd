package br.com.sgd.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class TraceIdsTest {
  @Test
  void returnsUuid() {
    String value = TraceIds.currentOrRandom();
    assertThat(UUID.fromString(value).toString()).isEqualTo(value);
  }
}
