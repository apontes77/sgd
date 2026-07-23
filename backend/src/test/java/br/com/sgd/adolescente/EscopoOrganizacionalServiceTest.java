package br.com.sgd.adolescente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.Gerencia;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;

@ExtendWith(MockitoExtension.class)
class EscopoOrganizacionalServiceTest {
  @Mock Discipulado discipulado;
  @Mock Gerencia gerencia;
  private final EscopoOrganizacionalService service = new EscopoOrganizacionalService();
  private User gerente;
  private User discipulador;
  private User coLider;

  @BeforeEach
  void setup() {
    gerente = usuario(1L, Role.GERENTE);
    discipulador = usuario(2L, Role.DISCIPULADOR);
    coLider = usuario(3L, Role.CO_LIDER);
    lenient().when(discipulado.getGerencia()).thenReturn(gerencia);
    lenient().when(gerencia.getGerente()).thenReturn(gerente);
    lenient().when(discipulado.getDiscipulador()).thenReturn(discipulador);
    lenient().when(discipulado.getCoLideres()).thenReturn(Set.of(coLider));
  }

  @Test
  void adminPodeLerEAlterar() {
    User admin = usuario(9L, Role.ADMIN);
    assertThat(service.podeLer(admin, discipulado)).isTrue();
    assertThat(service.podeAlterar(admin, discipulado)).isTrue();
  }

  @Test
  void gerenteDaGerenciaPodeLerMasNaoAlterar() {
    assertThat(service.podeLer(gerente, discipulado)).isTrue();
    assertThat(service.podeAlterar(gerente, discipulado)).isFalse();
  }

  @Test
  void discipuladorECoLiderPodemLerEAlterar() {
    assertThat(service.podeLer(discipulador, discipulado)).isTrue();
    assertThat(service.podeAlterar(discipulador, discipulado)).isTrue();
    assertThat(service.podeLer(coLider, discipulado)).isTrue();
    assertThat(service.podeAlterar(coLider, discipulado)).isTrue();
  }

  @Test
  void usuarioForaDoEscopoRecebeForbidden() {
    User outro = usuario(8L, Role.CO_LIDER);
    assertThat(service.podeLer(outro, discipulado)).isFalse();
    assertThat(service.podeAlterar(outro, discipulado)).isFalse();
    assertThatThrownBy(() -> service.exigirLeitura(outro, discipulado))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("não possui acesso");
    assertThatThrownBy(() -> service.exigirAlteracao(outro, discipulado))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("não pode alterar");
  }

  private static User usuario(long id, Role role) {
    User user = new User("U" + id, "u" + id + "@teste.local", "hash", Set.of(role));
    try {
      Field f = User.class.getDeclaredField("id");
      f.setAccessible(true);
      f.set(user, id);
      return user;
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }
}
