package br.com.sgd.painel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.organizacao.Sexo;
import br.com.sgd.user.User;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PainelLiderServiceTest {
    private PainelLiderRepository painel;
    private DiscipuladoRepository discipulados;
    private PainelLiderService service;
    private User lider;
    private final LocalDate inicio = LocalDate.of(2026, 1, 1);
    private final LocalDate fim = LocalDate.of(2026, 6, 30);

    @BeforeEach void setup() {
        painel = mock(PainelLiderRepository.class);
        discipulados = mock(DiscipuladoRepository.class);
        service = new PainelLiderService(painel, discipulados);
        lider = mock(User.class);
        when(lider.getId()).thenReturn(7L);
    }

    @Test void retorna404SemAssociacaoE409ComMultiplas() {
        when(discipulados.findAllByLiderancaUsuarioId(7L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.consultar(lider, inicio, fim)).isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode().value()).isEqualTo(404));
        when(discipulados.findAllByLiderancaUsuarioId(7L)).thenReturn(List.of(mock(Discipulado.class), mock(Discipulado.class)));
        assertThatThrownBy(() -> service.consultar(lider, inicio, fim)).isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode().value()).isEqualTo(409));
    }

    @Test void agregaSomenteODiscipuladoLiderado() {
        Discipulado discipulado = mock(Discipulado.class);
        when(discipulado.getId()).thenReturn(11L); when(discipulado.getNome()).thenReturn("Esperança");
        when(discipulado.getSexo()).thenReturn(Sexo.FEMININO); when(discipulado.isAtivo()).thenReturn(true);
        when(discipulados.findAllByLiderancaUsuarioId(7L)).thenReturn(List.of(discipulado));
        var frequencias = mock(PainelLiderRepository.ContagemMensal.class);
        when(frequencias.getReferencia()).thenReturn("2026-03"); when(frequencias.getPresentes()).thenReturn(3L); when(frequencias.getAusentes()).thenReturn(1L);
        var visitantes = mock(PainelLiderRepository.VisitantesMensais.class);
        when(visitantes.getReferencia()).thenReturn("2026-03"); when(visitantes.getVisitantes()).thenReturn(2L);
        when(painel.frequenciasMensais(11L, inicio, fim)).thenReturn(List.of(frequencias));
        when(painel.visitantesMensais(11L, inicio, fim)).thenReturn(List.of(visitantes));
        when(painel.encontrosRealizados(11L, inicio, fim)).thenReturn(4L);
        var ana = mock(PainelLiderRepository.DiscipuloPeriodo.class);
        when(ana.getAdolescenteId()).thenReturn(21L); when(ana.getNome()).thenReturn("Ana");
        var frequenciaAna = mock(PainelLiderRepository.FrequenciaIndividualMensal.class);
        when(frequenciaAna.getAdolescenteId()).thenReturn(21L); when(frequenciaAna.getNome()).thenReturn("Ana");
        when(frequenciaAna.getReferencia()).thenReturn("2026-03"); when(frequenciaAna.getPresentes()).thenReturn(3L);
        when(frequenciaAna.getAusentes()).thenReturn(1L);
        when(painel.discipulosNoPeriodo(11L, inicio, fim)).thenReturn(List.of(ana));
        when(painel.frequenciasIndividuaisMensais(11L, inicio, fim)).thenReturn(List.of(frequenciaAna));

        var resposta = service.consultar(lider, inicio, fim);
        assertThat(resposta.discipulado().nome()).isEqualTo("Esperança");
        assertThat(resposta.resumo().encontrosRealizados()).isEqualTo(4);
        assertThat(resposta.resumo().visitantes()).isEqualTo(2);
        assertThat(resposta.resumo().percentualPresenca()).isEqualByComparingTo("75.00");
        assertThat(resposta.discipulos()).singleElement().satisfies(item -> {
            assertThat(item.nome()).isEqualTo("Ana");
            assertThat(item.percentualPresenca()).isEqualByComparingTo("75.00");
            assertThat(item.evolucao()).singleElement().extracting(PainelLiderService.EvolucaoIndividual::referencia).isEqualTo("2026-03");
        });
    }

    @Test void retornaPercentualNuloParaDiscipuloSemRegistros() {
        Discipulado discipulado = mock(Discipulado.class);
        when(discipulados.findAllByLiderancaUsuarioId(7L)).thenReturn(List.of(discipulado));
        when(discipulado.getId()).thenReturn(11L); when(discipulado.getNome()).thenReturn("Esperança");
        when(discipulado.getSexo()).thenReturn(Sexo.FEMININO); when(discipulado.isAtivo()).thenReturn(true);
        var ana = mock(PainelLiderRepository.DiscipuloPeriodo.class);
        when(ana.getAdolescenteId()).thenReturn(21L); when(ana.getNome()).thenReturn("Ana");
        LocalDate inicioSemRegistros = LocalDate.of(2026, 1, 1);
        LocalDate fimSemRegistros = LocalDate.of(2026, 1, 31);
        when(painel.discipulosNoPeriodo(11L, inicioSemRegistros, fimSemRegistros)).thenReturn(List.of(ana));

        var resposta = service.consultar(lider, inicioSemRegistros, fimSemRegistros);

        assertThat(resposta.discipulos()).singleElement().extracting(PainelLiderService.Discipulo::percentualPresenca).isNull();
    }

    @Test void rejeitaPeriodoInvertidoOuSuperiorAVinteEQuatroMeses() {
        assertThatThrownBy(() -> service.consultar(lider, fim, inicio)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.consultar(lider, inicio, inicio.plusMonths(24).plusDays(1))).isInstanceOf(ResponseStatusException.class);
    }
}
