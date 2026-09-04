package br.com.sgd.organizacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class GerenciaServiceTest {

  @Mock private GerenciaRepository gerencias;
  @Mock private DiscipuladoRepository discipulados;
  @Mock private UserRepository usuarios;
  @Mock private User gerente;

  private GerenciaService service;

  @BeforeEach
  void setUp() {
    service = new GerenciaService(gerencias, discipulados, usuarios);
  }

  @Test
  void criaGerenciaComExatamenteUmGerenteAtivoComPerfilGerente() {
    when(gerente.isAtivo()).thenReturn(true);
    when(gerente.getPerfis()).thenReturn(Set.of(Role.GERENTE));
    when(usuarios.findById(10L)).thenReturn(Optional.of(gerente));
    when(gerencias.save(any(Gerencia.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Gerencia criada =
        service.create(
            "  Gerência Central  ",
            Sexo.MASCULINO,
            Set.of(FaixaEtaria.DE_15_MAIS, FaixaEtaria.DE_13_A_15),
            10L);

    ArgumentCaptor<Gerencia> captor = ArgumentCaptor.forClass(Gerencia.class);
    verify(gerencias).save(captor.capture());
    assertThat(criada).isSameAs(captor.getValue());
    assertThat(criada.getNome()).isEqualTo("Gerência Central");
    assertThat(criada.getSexo()).isEqualTo(Sexo.MASCULINO);
    assertThat(criada.getFaixasEtarias())
        .containsExactlyInAnyOrder(FaixaEtaria.DE_15_MAIS, FaixaEtaria.DE_13_A_15);
    assertThat(criada.getGerente()).isSameAs(gerente);
  }

  @Test
  void rejeitaGerenteInativoOuSemPerfilGerente() {
    when(gerente.isAtivo()).thenReturn(false);
    when(usuarios.findById(10L)).thenReturn(Optional.of(gerente));

    assertThatThrownBy(
            () ->
                service.create(
                    "Gerência Central", Sexo.MASCULINO, Set.of(FaixaEtaria.DE_15_MAIS), 10L))
        .isInstanceOf(GerenciaService.GerenteInvalidoException.class);

    verify(gerencias, never()).save(any());
  }

  @Test
  void rejeitaGerenteInexistente() {
    when(usuarios.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.create(
                    "Gerência Central", Sexo.MASCULINO, Set.of(FaixaEtaria.DE_15_MAIS), 99L))
        .isInstanceOf(GerenciaService.UsuarioOrganizacionalNotFoundException.class);

    verify(gerencias, never()).save(any());
  }

  @Test
  void mantemOGerenteAtualQuandoAtualizacaoParcialNaoInformaOutro() {
    Gerencia existente =
        new Gerencia("Gerência Central", Sexo.MASCULINO, Set.of(FaixaEtaria.DE_15_MAIS), gerente);
    when(gerencias.findById(1L)).thenReturn(Optional.of(existente));

    Gerencia atualizada = service.update(1L, "Novo nome", null, null, null, null);

    assertThat(atualizada.getGerente()).isSameAs(gerente);
    assertThat(atualizada.getNome()).isEqualTo("Novo nome");
    assertThat(atualizada.getSexo()).isEqualTo(Sexo.MASCULINO);
    assertThat(atualizada.getFaixasEtarias()).containsExactly(FaixaEtaria.DE_15_MAIS);
  }

  @Test
  void excluiGerenciaSemDiscipuladosAssociados() {
    Gerencia existente =
        new Gerencia("Gerência Central", Sexo.MASCULINO, Set.of(FaixaEtaria.DE_15_MAIS), gerente);
    when(gerencias.findById(1L)).thenReturn(Optional.of(existente));
    when(discipulados.existsByGerenciaId(1L)).thenReturn(false);

    service.delete(1L);

    verify(gerencias).delete(existente);
  }

  @Test
  void rejeitaExclusaoQuandoAindaHaDiscipuladosAssociados() {
    Gerencia existente =
        new Gerencia("Gerência Central", Sexo.MASCULINO, Set.of(FaixaEtaria.DE_15_MAIS), gerente);
    when(gerencias.findById(1L)).thenReturn(Optional.of(existente));
    when(discipulados.existsByGerenciaId(1L)).thenReturn(true);

    assertThatThrownBy(() -> service.delete(1L))
        .isInstanceOf(GerenciaService.GerenciaComDiscipuladosException.class);

    verify(gerencias, never()).delete(any(Gerencia.class));
  }

  @Test
  void rejeitaExclusaoDeGerenciaInexistente() {
    when(gerencias.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(99L))
        .isInstanceOf(GerenciaService.GerenciaNotFoundException.class);

    verify(gerencias, never()).delete(any(Gerencia.class));
  }
}
