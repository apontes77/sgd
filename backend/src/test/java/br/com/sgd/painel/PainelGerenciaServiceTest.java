package br.com.sgd.painel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.organizacao.Gerencia;
import br.com.sgd.organizacao.GerenciaRepository;
import br.com.sgd.organizacao.Sexo;
import br.com.sgd.user.User;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PainelGerenciaServiceTest {
    private PainelGerenciaRepository painel;
    private GerenciaRepository gerencias;
    private DiscipuladoRepository discipulados;
    private PainelGerenciaService service;
    private User gerente;
    private final LocalDate inicio = LocalDate.of(2026, 1, 1);
    private final LocalDate fim = LocalDate.of(2026, 6, 30);

    @BeforeEach void setup() {
        painel = mock(PainelGerenciaRepository.class); gerencias = mock(GerenciaRepository.class); discipulados = mock(DiscipuladoRepository.class);
        service = new PainelGerenciaService(painel, gerencias, discipulados);
        gerente = mock(User.class); when(gerente.getId()).thenReturn(7L);
    }

    @Test void retorna404SemGerenciaE409ComMultiplas() {
        when(gerencias.findAllByGerenteIdAndAtivoTrue(7L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.consultar(gerente, inicio, fim)).isInstanceOf(ResponseStatusException.class).satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode().value()).isEqualTo(404));
        when(gerencias.findAllByGerenteIdAndAtivoTrue(7L)).thenReturn(List.of(mock(Gerencia.class), mock(Gerencia.class)));
        assertThatThrownBy(() -> service.consultar(gerente, inicio, fim)).isInstanceOf(ResponseStatusException.class).satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode().value()).isEqualTo(409));
    }

    @Test void agregaGerenciaOrdenaDiscipuladosEIncluiInativoComHistorico() {
        Gerencia gerencia = mock(Gerencia.class); when(gerencia.getId()).thenReturn(10L); when(gerencia.getNome()).thenReturn("Centro");
        when(gerencias.findAllByGerenteIdAndAtivoTrue(7L)).thenReturn(List.of(gerencia));
        var mensal = mock(PainelGerenciaRepository.ContagemMensal.class); when(mensal.getReferencia()).thenReturn("2026-03"); when(mensal.getPresentes()).thenReturn(3L); when(mensal.getAusentes()).thenReturn(1L);
        var visitantes = mock(PainelGerenciaRepository.VisitantesMensais.class); when(visitantes.getReferencia()).thenReturn("2026-03"); when(visitantes.getVisitantes()).thenReturn(12L);
        when(painel.frequenciasMensais(10L, inicio, fim)).thenReturn(List.of(mensal)); when(painel.visitantesMensais(10L, inicio, fim)).thenReturn(List.of(visitantes)); when(painel.encontrosRealizados(10L, inicio, fim)).thenReturn(2L);
        var naoRealizado = mock(PainelGerenciaRepository.EncontroNaoRealizado.class);
        when(naoRealizado.getEncontroId()).thenReturn(20L); when(naoRealizado.getDiscipuladoId()).thenReturn(1L);
        when(naoRealizado.getDiscipuladoNome()).thenReturn("Ativo"); when(naoRealizado.getData()).thenReturn(LocalDate.of(2026, 3, 10));
        when(naoRealizado.getJustificativa()).thenReturn("Líder doente");
        when(painel.encontrosNaoRealizados(10L, inicio, fim)).thenReturn(List.of(naoRealizado));


        Discipulado ativo = discipulado(1L, "Ativo", true); Discipulado inativo = discipulado(2L, "Antigo", false); Discipulado oculto = discipulado(3L, "Sem histórico", false);
        when(discipulados.findAllByGerenciaIdOrderByNomeAsc(10L)).thenReturn(List.of(ativo, inativo, oculto));
        var contagem = mock(PainelGerenciaRepository.ContagemDiscipulado.class); when(contagem.getDiscipuladoId()).thenReturn(2L); when(contagem.getPresentes()).thenReturn(4L); when(contagem.getAusentes()).thenReturn(1L); when(contagem.getEncontrosRealizados()).thenReturn(1L);
        when(painel.frequenciasPorDiscipulado(10L, inicio, fim)).thenReturn(List.of(contagem));

        var resposta = service.consultar(gerente, inicio, fim);
        assertThat(resposta.resumo().visitantes()).isEqualTo(12); assertThat(resposta.resumo().percentualPresenca()).isEqualByComparingTo("75.00");
        assertThat(resposta.discipulados()).extracting(PainelGerenciaService.DiscipuladoIndicador::nome).containsExactly("Antigo", "Ativo");
        assertThat(resposta.discipulados().getFirst().ativo()).isFalse();
        assertThat(resposta.encontrosNaoRealizados()).singleElement().satisfies(item ->
            assertThat(item.justificativa()).isEqualTo("Líder doente"));
    }

    @Test void rejeitaPeriodoInvalido() {
        assertThatThrownBy(() -> service.consultar(gerente, fim, inicio)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.consultar(gerente, inicio, inicio.plusMonths(24).plusDays(1))).isInstanceOf(ResponseStatusException.class);
    }

    private Discipulado discipulado(long id, String nome, boolean ativo) {
        Discipulado item = mock(Discipulado.class); when(item.getId()).thenReturn(id); when(item.getNome()).thenReturn(nome); when(item.isAtivo()).thenReturn(ativo); when(item.getSexo()).thenReturn(Sexo.MASCULINO); return item;
    }
}
