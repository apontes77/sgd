package br.com.sgd.frequencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import br.com.sgd.adolescente.Adolescente;
import br.com.sgd.adolescente.EscopoOrganizacionalService;
import br.com.sgd.adolescente.VinculoAdolescenteDiscipulado;
import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;

@ExtendWith(MockitoExtension.class)
class ChamadaServiceTest {
  private static final Instant AGORA = Instant.parse("2026-07-17T12:00:00Z");
  @Mock EncontroService encontros;
  @Mock FrequenciaRepository frequencias;
  @Mock VisitanteRepository visitantes;
  @Mock EscopoOrganizacionalService escopo;
  @Mock Encontro encontro;
  @Mock Discipulado discipulado;
  private ChamadaService service;
  private User ator;

  @BeforeEach
  void setup() {
    service =
        new ChamadaService(
            encontros, frequencias, visitantes, escopo, Clock.fixed(AGORA, ZoneOffset.UTC));
    ator = new User("Ator", "ator@teste.local", "hash", Set.of(Role.DISCIPULADOR));
  }

  @Test
  void listarExigeLeituraDoDiscipulado() {
    when(encontros.encontro(1L)).thenReturn(encontro);
    when(encontro.getDiscipulado()).thenReturn(discipulado);
    when(frequencias.findAllByEncontroIdOrderByAdolescenteNome(1L)).thenReturn(List.of());

    assertThat(service.listar(ator, 1L)).isEmpty();
    verify(escopo).exigirLeitura(ator, discipulado);
  }

  @Test
  void listarVisitantesExigeLeituraERetornaQuantidadeOuZero() {
    when(encontros.encontro(1L)).thenReturn(encontro);
    when(encontro.getDiscipulado()).thenReturn(discipulado);
    when(visitantes.findByEncontroId(1L))
        .thenReturn(Optional.of(new Visitante(encontro, 4, AGORA)))
        .thenReturn(Optional.empty());

    assertThat(service.listarVisitantes(ator, 1L)).isEqualTo(4);
    assertThat(service.listarVisitantes(ator, 1L)).isZero();
    verify(escopo, org.mockito.Mockito.times(2)).exigirLeitura(ator, discipulado);
  }

  @Test
  void rejeitaChamadaNulaDuplicadaIncompletaOuComIdNulo() {
    prepararEncontro();
    Adolescente ana = adolescente(1L, "Ana");
    Adolescente bia = adolescente(2L, "Bia");
    when(encontros.participantesAtuais(encontro)).thenReturn(List.of(vinculo(ana), vinculo(bia)));
    when(frequencias.findAllByEncontroIdOrderByAdolescenteNome(1L)).thenReturn(List.of());

    assertThatThrownBy(() -> service.salvar(ator, 1L, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                service.salvar(
                    ator,
                    1L,
                    List.of(
                        item(1L, SituacaoFrequencia.PRESENTE),
                        item(1L, SituacaoFrequencia.AUSENTE))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("adolescentes ativos");
    assertThatThrownBy(
            () -> service.salvar(ator, 1L, List.of(item(1L, SituacaoFrequencia.PRESENTE))))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                service.salvar(
                    ator,
                    1L,
                    java.util.Arrays.asList(
                        item(1L, SituacaoFrequencia.PRESENTE),
                        item(null, SituacaoFrequencia.AUSENTE))))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void incluiRegistroHistoricoMesmoQuandoAdolescenteNaoEstaMaisAtivo() {
    prepararEncontro();
    Adolescente ana = adolescente(1L, "Ana");
    Adolescente exMembro = adolescente(2L, "Ex");
    Frequencia historica =
        new Frequencia(encontro, exMembro, SituacaoFrequencia.PRESENTE, AGORA.minusSeconds(60));
    when(encontros.participantesAtuais(encontro)).thenReturn(List.of(vinculo(ana)));
    when(frequencias.findAllByEncontroIdOrderByAdolescenteNome(1L)).thenReturn(List.of(historica));
    when(frequencias.save(any())).thenAnswer(i -> i.getArgument(0));

    List<Frequencia> salvas =
        service.salvar(
            ator,
            1L,
            List.of(item(1L, SituacaoFrequencia.PRESENTE), item(2L, SituacaoFrequencia.AUSENTE)));

    assertThat(salvas).hasSize(2);
    assertThat(historica.getSituacao()).isEqualTo(SituacaoFrequencia.AUSENTE);
    verify(encontros).auditar(eq(ator), eq("FREQUENCIA"), eq("SUBSTITUIR_CHAMADA"), any());
  }

  @Test
  void rejeitaSituacaoNulaSemPersistir() {
    prepararEncontro();
    Adolescente ana = adolescente(1L, "Ana");
    when(encontros.participantesAtuais(encontro)).thenReturn(List.of(vinculo(ana)));
    when(frequencias.findAllByEncontroIdOrderByAdolescenteNome(1L)).thenReturn(List.of());

    assertThatThrownBy(() -> service.salvar(ator, 1L, List.of(item(1L, null))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("situação");
    verify(frequencias, never()).save(any());
  }

  @Test
  void naoAuditaQuandoSituacaoPermaneceIgual() {
    prepararEncontro();
    Adolescente ana = adolescente(1L, "Ana");
    Frequencia existente =
        new Frequencia(encontro, ana, SituacaoFrequencia.PRESENTE, AGORA.minusSeconds(60));
    when(encontros.participantesAtuais(encontro)).thenReturn(List.of());
    when(frequencias.findAllByEncontroIdOrderByAdolescenteNome(1L)).thenReturn(List.of(existente));
    when(frequencias.save(any())).thenAnswer(i -> i.getArgument(0));

    service.salvar(ator, 1L, List.of(item(1L, SituacaoFrequencia.PRESENTE)));

    verify(encontros, never()).auditar(any(), any(), any(), any());
  }

  @Test
  void criaAtualizaEAuditaVisitantesSomenteQuandoQuantidadeMuda() {
    prepararEncontro();
    when(visitantes.findByEncontroId(1L)).thenReturn(Optional.empty());
    assertThat(service.salvarVisitantes(ator, 1L, 3)).isEqualTo(3);
    verify(visitantes).save(any(Visitante.class));
    verify(encontros).auditar(eq(ator), eq("VISITANTE"), eq("ALTERAR"), any());

    Visitante existente = new Visitante(encontro, 3, AGORA.minusSeconds(60));
    org.mockito.Mockito.clearInvocations(encontros, visitantes);
    when(encontros.encontro(1L)).thenReturn(encontro);
    when(visitantes.findByEncontroId(1L)).thenReturn(Optional.of(existente));
    service.salvarVisitantes(ator, 1L, 3);
    verify(encontros, never()).auditar(any(), any(), any(), any());
  }

  @Test
  void rejeitaQuantidadeNegativa() {
    prepararEncontro();
    assertThatThrownBy(() -> service.salvarVisitantes(ator, 1L, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("negativa");
    verify(visitantes, never()).save(any());
  }

  private void prepararEncontro() {
    when(encontros.encontro(1L)).thenReturn(encontro);
  }

  private VinculoAdolescenteDiscipulado vinculo(Adolescente adolescente) {
    return new VinculoAdolescenteDiscipulado(adolescente, discipulado, LocalDate.of(2026, 1, 1));
  }

  private static ChamadaService.ItemChamada item(Long id, SituacaoFrequencia situacao) {
    return new ChamadaService.ItemChamada(id, situacao);
  }

  private static Adolescente adolescente(long id, String nome) {
    Adolescente a = org.mockito.Mockito.mock(Adolescente.class);
    when(a.getId()).thenReturn(id);
    return a;
  }
}
