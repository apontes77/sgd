package br.com.sgd.organizacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;

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
    lenient().when(discipulados.findAllByLiderancaUsuarioId(any())).thenReturn(List.of());
  }

  @Test
  void criaDiscipuladoComExatamenteUmDiscipuladorAtivoComPerfilAdequado() {
    when(gerencia.isAtivo()).thenReturn(true);
    when(gerencias.findById(1L)).thenReturn(Optional.of(gerencia));
    when(discipulador.isAtivo()).thenReturn(true);
    when(discipulador.getPerfis()).thenReturn(Set.of(Role.DISCIPULADOR));
    when(usuarios.findById(2L)).thenReturn(Optional.of(discipulador));
    when(discipulados.save(any(Discipulado.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Discipulado criado =
        service.create("  Discipulado Norte  ", Sexo.MASCULINO, FaixaEtaria.DE_11_A_13, 1L, 2L);

    assertThat(criado.getNome()).isEqualTo("Discipulado Norte");
    assertThat(criado.getFaixaEtaria()).isEqualTo(FaixaEtaria.DE_11_A_13);
    assertThat(criado.getGerencia()).isSameAs(gerencia);
    assertThat(criado.getDiscipulador()).isSameAs(discipulador);
    verify(discipulados).save(criado);
  }

  @Test
  void criaDiscipuladoDeFormacaoSemGerencia() {
    when(discipulador.isAtivo()).thenReturn(true);
    when(discipulador.getPerfis()).thenReturn(Set.of(Role.DISCIPULADOR));
    when(usuarios.findById(2L)).thenReturn(Optional.of(discipulador));
    when(discipulados.save(any(Discipulado.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Discipulado criado =
        service.create("Formação Norte", Sexo.FEMININO, FaixaEtaria.DE_15_MAIS, null, 2L, true);

    assertThat(criado.isEmFormacao()).isTrue();
    assertThat(criado.getGerencia()).isNull();
    verify(gerencias, never()).findById(any());
  }

  @Test
  void rejeitaFormacaoComGerenciaEPadraoSemGerencia() {
    when(discipulador.isAtivo()).thenReturn(true);
    when(discipulador.getPerfis()).thenReturn(Set.of(Role.DISCIPULADOR));
    when(usuarios.findById(2L)).thenReturn(Optional.of(discipulador));

    assertThatThrownBy(
            () -> service.create("Formação", Sexo.MASCULINO, FaixaEtaria.DE_15_MAIS, 1L, 2L, true))
        .isInstanceOf(DiscipuladoService.FormacaoInvalidaException.class);
    assertThatThrownBy(
            () -> service.create("Padrão", Sexo.MASCULINO, FaixaEtaria.DE_15_MAIS, null, 2L, false))
        .isInstanceOf(DiscipuladoService.FormacaoInvalidaException.class);
  }

  @Test
  void permiteAcumularDiscipuladoPadraoEDeFormacao() {
    when(discipulador.isAtivo()).thenReturn(true);
    when(discipulador.getId()).thenReturn(2L);
    when(discipulador.getPerfis()).thenReturn(Set.of(Role.DISCIPULADOR));
    when(usuarios.findById(2L)).thenReturn(Optional.of(discipulador));
    Discipulado padrao = discipuladoExistente();
    when(discipulados.findAllByLiderancaUsuarioId(2L)).thenReturn(List.of(padrao));
    when(discipulados.save(any(Discipulado.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Discipulado formacao =
        service.create("Formação", Sexo.FEMININO, FaixaEtaria.DE_15_MAIS, null, 2L, true);

    assertThat(formacao.isEmFormacao()).isTrue();
  }

  @Test
  void rejeitaDoisDiscipuladosDoMesmoTipo() {
    when(discipulador.isAtivo()).thenReturn(true);
    when(discipulador.getId()).thenReturn(2L);
    when(discipulador.getPerfis()).thenReturn(Set.of(Role.DISCIPULADOR));
    when(usuarios.findById(2L)).thenReturn(Optional.of(discipulador));
    Discipulado existente =
        new Discipulado(
            "Formação A", Sexo.FEMININO, FaixaEtaria.DE_15_MAIS, null, discipulador, true);
    when(discipulados.findAllByLiderancaUsuarioId(2L)).thenReturn(List.of(existente));

    assertThatThrownBy(
            () ->
                service.create("Formação B", Sexo.FEMININO, FaixaEtaria.DE_15_MAIS, null, 2L, true))
        .isInstanceOf(DiscipuladoService.LiderancaDuplicadaException.class);
    verify(discipulados, never()).save(any());
  }

  @Test
  void rejeitaCoLiderEmDiscipuladoDeFormacao() {
    Discipulado formacao =
        new Discipulado(
            "Formação", Sexo.FEMININO, FaixaEtaria.DE_15_MAIS, null, discipulador, true);
    when(discipulados.findById(7L)).thenReturn(Optional.of(formacao));

    assertThatThrownBy(() -> service.replaceCoLideres(7L, List.of(3L)))
        .isInstanceOf(Discipulado.FormacaoNaoPermiteCoLiderException.class);
  }

  @Test
  void rejeitaDiscipuladorInativoOuSemPerfilDeDiscipulador() {
    when(discipulador.isAtivo()).thenReturn(true);
    when(discipulador.getPerfis()).thenReturn(Set.of(Role.CO_LIDER));
    when(usuarios.findById(2L)).thenReturn(Optional.of(discipulador));

    assertThatThrownBy(
            () ->
                service.create("Discipulado Norte", Sexo.MASCULINO, FaixaEtaria.DE_11_A_13, 1L, 2L))
        .isInstanceOf(DiscipuladoService.DiscipuladorInvalidoException.class);

    verify(discipulados, never()).save(any());
  }

  @Test
  void mantemODiscipuladorAtualQuandoAtualizacaoParcialNaoInformaOutro() {
    Discipulado discipulado = discipuladoExistente();
    when(discipulados.findById(7L)).thenReturn(Optional.of(discipulado));

    Discipulado atualizado = service.update(7L, "Novo nome", null, null, null, null, null);

    assertThat(atualizado.getDiscipulador()).isSameAs(discipulador);
    assertThat(atualizado.getNome()).isEqualTo("Novo nome");
    assertThat(atualizado.getFaixaEtaria()).isEqualTo(FaixaEtaria.DE_11_A_13);
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

    assertThat(atualizado.getCoLideres())
        .containsExactlyInAnyOrder(primeiroCoLider, segundoCoLider);
  }

  @Test
  void substituiCoLideresJaAssociadosMantendoNoMaximoDois() {
    Discipulado discipulado = discipuladoExistente();
    when(discipulados.findById(7L)).thenReturn(Optional.of(discipulado));
    when(discipulador.getId()).thenReturn(2L);
    configurarCoLider(primeiroCoLider, 3L);
    configurarCoLider(segundoCoLider, 4L);
    when(usuarios.findById(3L)).thenReturn(Optional.of(primeiroCoLider));
    when(usuarios.findById(4L)).thenReturn(Optional.of(segundoCoLider));

    service.replaceCoLideres(7L, List.of(3L));
    Discipulado atualizado = service.replaceCoLideres(7L, List.of(4L));

    assertThat(atualizado.getCoLideres()).containsExactly(segundoCoLider);
  }

  @Test
  void rejeitaMaisDeDoisCoLideresAntesDeModificarODiscipulado() {
    Set<Long> ids = new LinkedHashSet<>(Set.of(3L, 4L, 5L));

    assertThatThrownBy(() -> service.replaceCoLideres(7L, ids))
        .isInstanceOf(Discipulado.CoLiderLimitExceededException.class);

    verify(discipulados, never()).findById(any());
  }

  @Test
  void rejeitaCoLiderDuplicadoAntesDeModificarODiscipulado() {
    assertThatThrownBy(() -> service.replaceCoLideres(7L, List.of(3L, 3L)))
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
    return new Discipulado(
        "Discipulado Norte", Sexo.MASCULINO, FaixaEtaria.DE_11_A_13, gerencia, discipulador);
  }

  private void configurarCoLider(User coLider, long id) {
    when(coLider.getId()).thenReturn(id);
    when(coLider.isAtivo()).thenReturn(true);
    when(coLider.getPerfis()).thenReturn(Set.of(Role.CO_LIDER));
  }
}
