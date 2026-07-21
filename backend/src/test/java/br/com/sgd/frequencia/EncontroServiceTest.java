package br.com.sgd.frequencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sgd.adolescente.EscopoOrganizacionalService;
import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@ExtendWith(MockitoExtension.class)
class EncontroServiceTest {
    private static final Instant AGORA = Instant.parse("2026-07-17T12:00:00Z");
    @Mock EncontroRepository encontros;
    @Mock FrequenciaRepository frequencias;
    @Mock VisitanteRepository visitantes;
    @Mock VinculoHistoricoRepository vinculos;
    @Mock DiscipuladoRepository discipulados;
    @Mock EscopoOrganizacionalService escopo;
    @Mock AuditLogRepository auditoria;
    @Mock ObjectMapper json;
    @Mock Discipulado discipulado;
    private EncontroService service;

    @BeforeEach void setup() {
        service = new EncontroService(encontros, frequencias, visitantes, vinculos, discipulados, escopo,
                auditoria, json, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test void criaEncontroAtivoERegistraAuditoria() throws Exception {
        User ator = usuario(Role.DISCIPULADOR);
        when(discipulados.findById(10L)).thenReturn(Optional.of(discipulado));
        when(discipulado.isAtivo()).thenReturn(true);
        when(encontros.save(any())).thenAnswer(i -> withId(i.getArgument(0), 1L));
        when(json.writeValueAsString(any())).thenReturn("{}");

        Encontro criado = service.criar(ator, 10L, LocalDate.of(2026, 7, 17), SituacaoEncontro.REALIZADO);

        assertThat(criado.getCriadoEm()).isEqualTo(AGORA);
        verify(escopo).exigirAlteracao(ator, discipulado);
        verify(auditoria).save(any());
    }

    @Test void rejeitaCriacaoEmDiscipuladoInativo() {
        when(discipulados.findById(10L)).thenReturn(Optional.of(discipulado));
        when(discipulado.isAtivo()).thenReturn(false);

        assertThatThrownBy(() -> service.criar(usuario(Role.ADMIN), 10L, LocalDate.now(), SituacaoEncontro.REALIZADO))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("inativo");
        verify(encontros, never()).save(any());
    }

    @Test void impedeCancelarQuandoHaFrequenciaOuVisitante() {
        Encontro encontro = encontro(AGORA.minusSeconds(60));
        when(encontros.findById(1L)).thenReturn(Optional.of(encontro));
        when(frequencias.existsByEncontroId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(usuario(Role.ADMIN), 1L, null, SituacaoEncontro.NAO_REALIZADO, "Líder indisponível"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("situação alterada");

        when(frequencias.existsByEncontroId(1L)).thenReturn(false);
        when(visitantes.findByEncontroId(1L)).thenReturn(Optional.of(new Visitante(encontro, 2, AGORA)));
        assertThatThrownBy(() -> service.atualizar(usuario(Role.ADMIN), 1L, null, SituacaoEncontro.NAO_REALIZADO, "Líder indisponível"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("situação alterada");
    }

    @Test void permiteCancelarQuandoVisitantesSaoZero() throws Exception {
        Encontro encontro = encontro(AGORA.minusSeconds(60));
        when(encontros.findById(1L)).thenReturn(Optional.of(encontro));
        when(visitantes.findByEncontroId(1L)).thenReturn(Optional.of(new Visitante(encontro, 0, AGORA)));
        when(json.writeValueAsString(any())).thenReturn("{}");

        service.atualizar(usuario(Role.ADMIN), 1L, null, SituacaoEncontro.NAO_REALIZADO, "  Líder indisponível  ");

        assertThat(encontro.getSituacao()).isEqualTo(SituacaoEncontro.NAO_REALIZADO);
        assertThat(encontro.getJustificativa()).isEqualTo("Líder indisponível");
        verify(auditoria).save(any());
    }

    @Test void administradorEDiscipuladorPodemMarcarNaoRealizadoEJustificativaEhObrigatoria() {
        Encontro encontro = encontro(AGORA.minusSeconds(60));
        when(encontros.findById(1L)).thenReturn(Optional.of(encontro));

        service.atualizar(usuario(Role.DISCIPULADOR), 1L, null, SituacaoEncontro.NAO_REALIZADO, "Imprevisto");
        assertThat(encontro.getSituacao()).isEqualTo(SituacaoEncontro.NAO_REALIZADO);
        assertThat(encontro.getJustificativa()).isEqualTo("Imprevisto");

        assertThatThrownBy(() -> service.atualizar(usuario(Role.CO_LIDER), 1L, null,
                SituacaoEncontro.NAO_REALIZADO, "Imprevisto"))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(403));

        assertThatThrownBy(() -> service.atualizar(usuario(Role.ADMIN), 1L, null,
                SituacaoEncontro.NAO_REALIZADO, "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("justificativa");
        verify(encontros, never()).save(any());
    }

    @Test void rejeitaSegundoEncontroDoMesmoDiscipuladoNaMesmaData() {
        LocalDate data = LocalDate.of(2026, 7, 17);
        when(discipulados.findById(10L)).thenReturn(Optional.of(discipulado));
        when(discipulado.isAtivo()).thenReturn(true);
        when(encontros.existsByDiscipuladoIdAndData(10L, data)).thenReturn(true);

        assertThatThrownBy(() -> service.criar(usuario(Role.ADMIN), 10L, data, SituacaoEncontro.REALIZADO))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Já existe um encontro");
        verify(encontros, never()).save(any());
    }

    @Test void somenteAdministradorPodeReverterNaoRealizacao() throws Exception {
        Encontro cancelado = new Encontro(discipulado, LocalDate.of(2026, 7, 17),
                SituacaoEncontro.NAO_REALIZADO, "Imprevisto", AGORA);
        when(encontros.findById(1L)).thenReturn(Optional.of(cancelado));

        assertThatThrownBy(() -> service.atualizar(usuario(Role.DISCIPULADOR), 1L, null,
                SituacaoEncontro.REALIZADO, null))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(403));

        when(json.writeValueAsString(any())).thenReturn("{}");
        service.atualizar(usuario(Role.ADMIN), 1L, null, SituacaoEncontro.REALIZADO, null);

        assertThat(cancelado.getSituacao()).isEqualTo(SituacaoEncontro.REALIZADO);
        assertThat(cancelado.getJustificativa()).isNull();
    }

    @Test void rejeitaMoverEncontroParaDataJaOcupada() {
        Encontro encontro = encontro(AGORA.minusSeconds(60));
        when(encontros.findById(1L)).thenReturn(Optional.of(encontro));
        when(encontros.existsByDiscipuladoIdAndDataAndIdNot(0L, LocalDate.of(2026, 7, 18), 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(usuario(Role.ADMIN), 1L, LocalDate.of(2026, 7, 18), null, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Já existe um encontro");
    }
    @Test void aplicaJanelaDeTresHorasSomenteParaNaoAdministrador() {
        Encontro antigo = encontro(AGORA.minusSeconds(3 * 3600 + 1));
        assertThatThrownBy(() -> service.exigirEditavel(usuario(Role.DISCIPULADOR), antigo))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("três horas");

        service.exigirEditavel(usuario(Role.ADMIN), antigo);
    }

    @Test void rejeitaEdicaoDeEncontroCanceladoAntesDeAvaliarJanela() {
        Encontro cancelado = new Encontro(discipulado, LocalDate.now(), SituacaoEncontro.NAO_REALIZADO, AGORA);
        assertThatThrownBy(() -> service.exigirEditavel(usuario(Role.ADMIN), cancelado))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("não realizado");
    }

    @Test void listarUsaLimitesPadraoEExigeLeitura() {
        User ator = usuario(Role.GERENTE);
        when(discipulados.findById(10L)).thenReturn(Optional.of(discipulado));
        when(encontros.findAllByDiscipuladoIdAndDataBetweenOrderByDataDesc(10L,
                LocalDate.of(1900, 1, 1), LocalDate.of(2999, 12, 31))).thenReturn(List.of());

        assertThat(service.listar(ator, 10L, null, null)).isEmpty();
        verify(escopo).exigirLeitura(ator, discipulado);
    }

    @Test void converteFalhaDeSerializacaoDaAuditoria() throws Exception {
        when(json.writeValueAsString(any())).thenThrow(new JsonProcessingException("erro") { });
        assertThatThrownBy(() -> service.auditar(usuario(Role.ADMIN), "E", "A", java.util.Map.of()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("auditoria");
    }

    private Encontro encontro(Instant criadoEm) {
        return new Encontro(discipulado, LocalDate.of(2026, 7, 17), SituacaoEncontro.REALIZADO, criadoEm);
    }

    private static Encontro withId(Encontro encontro, long id) {
        try {
            var field = Encontro.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(encontro, id);
            return encontro;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
    private static User usuario(Role role) { return new User("Ator", "ator@teste.local", "hash", Set.of(role)); }
}
