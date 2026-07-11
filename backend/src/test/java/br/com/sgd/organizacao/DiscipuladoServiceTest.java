package br.com.sgd.organizacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiscipuladoServiceTest {

    @Mock private DiscipuladoRepository discipulados;
    @Mock private GerenciaRepository gerencias;
    @Mock private UserRepository usuarios;
    @Mock private Gerencia gerencia;
    @Mock private User discipulador;
    @Mock private User primeiroCoLider;
    @Mock private User segundoCoLider;

    private DiscipuladoService service;

    @BeforeEach
    void setUp() {
        service = new DiscipuladoService(discipulados, gerencias, usuarios);
    }

    @Test
    void criaDiscipuladoComExatamenteUmDiscipuladorAtivoComPerfilAdequado() {
        when(gerencia.isAtivo()).thenReturn(true);
        when(gerencias.findById(1L)).thenReturn(Optional.of(gerencia));
        when(discipulador.isAtivo()).thenReturn(true);
        when(discipulador.getPerfis()).thenReturn(Set.of(Role.DISCIPULADOR));
        when(usuarios.findById(2L)).thenReturn(Optional.of(discipulador));
        when(discipulados.save(any(Discipulado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Discipulado criado = service.create("  Discipulado Norte  ", Sexo.MASCULINO, 1L, 2L);

        assertThat(criado.getNome()).isEqualTo("Discipulado Norte");
        assertThat(criado.getGerencia()).isSameAs(gerencia);
        assertThat(criado.getDiscipulador()).isSameAs(discipulador);
        verify(discipulados).save(criado);
    }

    @Test
    void rejeitaDiscipuladorInativoOuSemPerfilDeDiscipulador() {
        when(gerencia.isAtivo()).thenReturn(true);
        when(gerencias.findById(1L)).thenReturn(Optional.of(gerencia));
        when(discipulador.isAtivo()).thenReturn(true);
        when(discipulador.getPerfis()).thenReturn(Set.of(Role.CO_LIDER));
        when(usuarios.findById(2L)).thenReturn(Optional.of(discipulador));

        assertThatThrownBy(() -> service.create("Discipulado Norte", Sexo.MASCULINO, 1L, 2L))
            .isInstanceOf(DiscipuladoService.DiscipuladorInvalidoException.class);

        verify(discipulados, never()).save(any());
    }

    @Test
    void mantemODiscipuladorAtualQuandoAtualizacaoParcialNaoInformaOutro() {
        Discipulado discipulado = discipuladoExistente();
        when(discipulados.findById(7L)).thenReturn(Optional.of(discipulado));

        Discipulado atualizado = service.update(7L, "Novo nome", null, null, null, null);

        assertThat(atualizado.getDiscipulador()).isSameAs(discipulador);
        assertThat(atualizado.getNome()).isEqualTo("Novo nome");
    }

    @Test
    void substituiCoLideresPorAteDoisUsuariosAtivosComPerfilAdequado() {
        Discipulado discipulado = discipuladoExistente();
        when(discipulados.findById(7L)).thenReturn(Optional.of(discipulado));
        when(discipulador.getId()).thenReturn(2L);
        configurarCoLider(primeiroCoLider, 3L);
        configurarCoLider(segundoCoLider, 4L);
        when(usuarios.findById(3L)).thenReturn(Optional.of(primeiroCoLider));
        when(usuarios.findById(4L)).thenReturn(Optional.of(segundoCoLider));

        Discipulado atualizado = service.replaceCoLideres(7L, new LinkedHashSet<>(Set.of(3L, 4L)));

        assertThat(atualizado.getCoLideres()).containsExactlyInAnyOrder(primeiroCoLider, segundoCoLider);
    }

    @Test
    void rejeitaMaisDeDoisCoLideresAntesDeModificarODiscipulado() {
        Set<Long> ids = new LinkedHashSet<>(Set.of(3L, 4L, 5L));

        assertThatThrownBy(() -> service.replaceCoLideres(7L, ids))
            .isInstanceOf(Discipulado.CoLiderLimitExceededException.class);

        verify(discipulados, never()).findById(any());
    }

    @Test
    void rejeitaCoLiderInativoSemPerfilOuIgualAoDiscipulador() {
        Discipulado discipulado = discipuladoExistente();
        when(discipulados.findById(7L)).thenReturn(Optional.of(discipulado));
        when(primeiroCoLider.isAtivo()).thenReturn(true);
        when(primeiroCoLider.getPerfis()).thenReturn(Set.of(Role.DISCIPULADOR));
        when(usuarios.findById(3L)).thenReturn(Optional.of(primeiroCoLider));

        assertThatThrownBy(() -> service.replaceCoLideres(7L, Set.of(3L)))
            .isInstanceOf(DiscipuladoService.CoLiderInvalidoException.class);

        assertThat(discipulado.getCoLideres()).isEmpty();
    }

    private Discipulado discipuladoExistente() {
        return new Discipulado("Discipulado Norte", Sexo.MASCULINO, gerencia, discipulador);
    }

    private void configurarCoLider(User coLider, long id) {
        when(coLider.getId()).thenReturn(id);
        when(coLider.isAtivo()).thenReturn(true);
        when(coLider.getPerfis()).thenReturn(Set.of(Role.CO_LIDER));
    }
}
