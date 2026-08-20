package br.com.sgd.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class UserTest {
  @Test
  void acumulaAdminEmDiscipuladorExistente() {
    User user = new User("João", "joao@sgd.local", "hash", Set.of(Role.DISCIPULADOR));

    user.update("João Costa", Set.of(Role.DISCIPULADOR, Role.ADMIN), true);

    assertThat(user.getNome()).isEqualTo("João Costa");
    assertThat(user.getPerfis()).containsExactlyInAnyOrder(Role.DISCIPULADOR, Role.ADMIN);
    assertThat(user.isAtivo()).isTrue();
  }
}
