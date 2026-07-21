package br.com.sgd.adolescente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.organizacao.Gerencia;
import br.com.sgd.user.User;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdolescenteServiceTest {
    @Mock AdolescenteRepository adolescentes;
    @Mock VinculoAdolescenteRepository vinculos;
    @Mock DiscipuladoRepository discipulados;
    @Mock EscopoOrganizacionalService escopo;
    @Mock User usuario;
    @Mock Discipulado origem;
    @Mock Discipulado destino;
    @Mock Gerencia gerencia;
    private AdolescenteService service;

    @BeforeEach void setup() { service = new AdolescenteService(adolescentes, vinculos, discipulados, escopo); }

    @Test void criaAdolescenteEVinculoInicialNoMesmoFluxo() {
        configurarAtivo(origem);
        when(discipulados.findById(10L)).thenReturn(Optional.of(origem));
        when(adolescentes.save(any())).thenAnswer(i -> i.getArgument(0));
        when(vinculos.save(any())).thenAnswer(i -> i.getArgument(0));
        var dados = new AdolescenteService.DadosAdolescente(" Ana ", LocalDate.of(2010, 3, 2), null, "@ana", 10L, true, null);

        Adolescente criado = service.criar(usuario, dados);

        assertThat(criado.getNome()).isEqualTo("Ana");
        var captor = ArgumentCaptor.forClass(VinculoAdolescenteDiscipulado.class);
        verify(vinculos).save(captor.capture());
        assertThat(captor.getValue().getAdolescente()).isSameAs(criado);
        assertThat(captor.getValue().getDiscipulado()).isSameAs(origem);
        verify(escopo).exigirAlteracao(usuario, origem);
    }

    @Test void usaDataInicioInformadaAoRegistrarVisitanteEmDataAnterior() {
        configurarAtivo(origem);
        when(discipulados.findById(10L)).thenReturn(Optional.of(origem));
        when(adolescentes.save(any())).thenAnswer(i -> i.getArgument(0));
        when(vinculos.save(any())).thenAnswer(i -> i.getArgument(0));
        var dados = new AdolescenteService.DadosAdolescente("Bia", LocalDate.of(2011, 5, 4), null, null, 10L, true, LocalDate.of(2026, 3, 1));

        service.criar(usuario, dados);

        var captor = ArgumentCaptor.forClass(VinculoAdolescenteDiscipulado.class);
        verify(vinculos).save(captor.capture());
        assertThat(captor.getValue().getDataInicio()).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test void transfereEncerrandoAnteriorSemApagarHistorico() {
        Adolescente adolescente = new Adolescente("Ana", LocalDate.of(2010, 3, 2), null, null);
        var anterior = new VinculoAdolescenteDiscipulado(adolescente, origem, LocalDate.of(2026, 1, 1));
        configurarAtivo(destino);
        when(adolescentes.findById(1L)).thenReturn(Optional.of(adolescente));
        when(vinculos.findByAdolescenteIdAndAtivoTrue(1L)).thenReturn(Optional.of(anterior));
        when(discipulados.findById(20L)).thenReturn(Optional.of(destino));
        when(origem.getId()).thenReturn(10L);
        when(vinculos.save(any())).thenAnswer(i -> i.getArgument(0));

        var novo = service.transferir(usuario, 1L, 20L, LocalDate.of(2026, 2, 1));

        assertThat(anterior.isAtivo()).isFalse();
        assertThat(anterior.getDataFim()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(novo.getDataInicio()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(novo.getDiscipulado()).isSameAs(destino);
        verify(escopo).exigirAlteracao(usuario, origem);
        verify(escopo).exigirAlteracao(usuario, destino);
    }

    @Test void rejeitaTransferenciaNaDataInicialOuParaOMesmoDiscipulado() {
        Adolescente adolescente = new Adolescente("Ana", LocalDate.of(2010, 3, 2), null, null);
        var anterior = new VinculoAdolescenteDiscipulado(adolescente, origem, LocalDate.of(2026, 1, 1));
        configurarAtivo(destino);
        when(adolescentes.findById(1L)).thenReturn(Optional.of(adolescente));
        when(vinculos.findByAdolescenteIdAndAtivoTrue(1L)).thenReturn(Optional.of(anterior));
        when(discipulados.findById(20L)).thenReturn(Optional.of(destino));
        when(origem.getId()).thenReturn(10L);

        assertThatThrownBy(() -> service.transferir(usuario, 1L, 20L, LocalDate.of(2026, 1, 1)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("após o início");
        assertThat(anterior.isAtivo()).isTrue();
    }

    @Test void atualizarNaoPermiteTrocarDiscipuladoSemEndpointDeTransferencia() {
        Adolescente adolescente = new Adolescente("Ana", LocalDate.of(2010, 3, 2), null, null);
        var atual = new VinculoAdolescenteDiscipulado(adolescente, origem, LocalDate.of(2026, 1, 1));
        when(adolescentes.findById(1L)).thenReturn(Optional.of(adolescente));
        when(vinculos.findByAdolescenteIdAndAtivoTrue(1L)).thenReturn(Optional.of(atual));
        when(origem.getId()).thenReturn(10L);
        var dados = new AdolescenteService.DadosAdolescente("Ana", LocalDate.of(2010, 3, 2), null, null, 20L, true, null);

        assertThatThrownBy(() -> service.atualizar(usuario, 1L, dados))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("endpoint de vínculos");
    }

    private void configurarAtivo(Discipulado d) {
        when(d.isAtivo()).thenReturn(true); when(d.getGerencia()).thenReturn(gerencia); when(gerencia.isAtivo()).thenReturn(true);
    }
}
