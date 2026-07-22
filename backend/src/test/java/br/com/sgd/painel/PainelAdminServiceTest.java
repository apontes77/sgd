package br.com.sgd.painel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PainelAdminServiceTest {
    private PainelAdminRepository repository;
    private PainelAdminService service;
    private final LocalDate inicio = LocalDate.of(2026, 1, 1);
    private final LocalDate fim = LocalDate.of(2026, 6, 30);

    @BeforeEach void setup() {
        repository = mock(PainelAdminRepository.class);
        service = new PainelAdminService(repository);
        when(repository.frequenciasMensais(inicio, fim)).thenReturn(List.of());
        when(repository.visitantesMensais(inicio, fim)).thenReturn(List.of());
        when(repository.porGerencia(inicio, fim)).thenReturn(List.of());
        when(repository.porGerenciaMensal(inicio, fim)).thenReturn(List.of());
        when(repository.porSexo(inicio, fim)).thenReturn(List.of());
        when(repository.encontrosNaoRealizados(inicio, fim)).thenReturn(0L);
    }

    @Test void retornaZerosESexosQuandoNaoHaRegistros() {
        var resposta = service.consultar(inicio, fim);
        assertThat(resposta.resumo().presentes()).isZero();
        assertThat(resposta.resumo().percentualPresenca()).isZero();
        assertThat(resposta.evolucao()).isEmpty();
        assertThat(resposta.encontrosNaoRealizados()).isZero();
        assertThat(resposta.gerenciasMensal()).isEmpty();
        assertThat(resposta.sexos()).extracting(PainelAdminService.SexoIndicador::sexo).containsExactly("MASCULINO", "FEMININO");
    }

    @Test void agregaMesesESeparaVisitantesDoPercentual() {
        var frequencia = mock(PainelAdminRepository.ContagemMensal.class);
        when(frequencia.getReferencia()).thenReturn("2026-03");
        when(frequencia.getPresentes()).thenReturn(3L);
        when(frequencia.getAusentes()).thenReturn(1L);
        var visitante = mock(PainelAdminRepository.VisitantesMensais.class);
        when(visitante.getReferencia()).thenReturn("2026-03");
        when(visitante.getVisitantes()).thenReturn(20L);
        when(repository.frequenciasMensais(inicio, fim)).thenReturn(List.of(frequencia));
        when(repository.visitantesMensais(inicio, fim)).thenReturn(List.of(visitante));
        when(repository.encontrosRealizados(inicio, fim)).thenReturn(2L);

        var resposta = service.consultar(inicio, fim);
        assertThat(resposta.resumo().visitantes()).isEqualTo(20);
        assertThat(resposta.resumo().percentualPresenca()).isEqualByComparingTo("75.00");
        assertThat(resposta.evolucao().getFirst().percentualPresenca()).isEqualByComparingTo("75.00");
    }

    @Test void ordenaGerenciasPorPercentual() {
        var menor = gerencia(1L, "Menor", 1L, 3L);
        var maior = gerencia(2L, "Maior", 9L, 1L);
        when(repository.porGerencia(inicio, fim)).thenReturn(List.of(menor, maior));
        assertThat(service.consultar(inicio, fim).gerencias()).extracting(PainelAdminService.GerenciaIndicador::nome).containsExactly("Maior", "Menor");
    }

    @Test void incluiNaoRealizadosESerieMensalPorGerencia() {
        when(repository.encontrosNaoRealizados(inicio, fim)).thenReturn(3L);
        var mensal = mock(PainelAdminRepository.ContagemGerenciaMensal.class);
        when(mensal.getGerenciaId()).thenReturn(10L);
        when(mensal.getGerenciaNome()).thenReturn("Norte");
        when(mensal.getReferencia()).thenReturn("2026-02");
        when(mensal.getPresentes()).thenReturn(4L);
        when(mensal.getAusentes()).thenReturn(1L);
        when(repository.porGerenciaMensal(inicio, fim)).thenReturn(List.of(mensal));

        var resposta = service.consultar(inicio, fim);
        assertThat(resposta.encontrosNaoRealizados()).isEqualTo(3);
        assertThat(resposta.gerenciasMensal()).hasSize(1);
        assertThat(resposta.gerenciasMensal().getFirst().gerenciaNome()).isEqualTo("Norte");
        assertThat(resposta.gerenciasMensal().getFirst().referencia()).isEqualTo("2026-02");
        assertThat(resposta.gerenciasMensal().getFirst().percentualPresenca()).isEqualByComparingTo("80.00");
    }

    @Test void rejeitaPeriodoInvertidoOuSuperiorAVinteEQuatroMeses() {
        assertThatThrownBy(() -> service.consultar(fim, inicio)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.consultar(inicio, inicio.plusMonths(24).plusDays(1))).isInstanceOf(ResponseStatusException.class);
    }

    private PainelAdminRepository.ContagemGerencia gerencia(Long id, String nome, Long presentes, Long ausentes) {
        var item = mock(PainelAdminRepository.ContagemGerencia.class);
        when(item.getId()).thenReturn(id); when(item.getNome()).thenReturn(nome); when(item.getPresentes()).thenReturn(presentes); when(item.getAusentes()).thenReturn(ausentes);
        return item;
    }
}
