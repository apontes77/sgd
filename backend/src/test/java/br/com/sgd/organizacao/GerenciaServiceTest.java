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
import java.util.Set;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        Gerencia criada = service.create("  Gerência Central  ", 10L);

        ArgumentCaptor<Gerencia> captor = ArgumentCaptor.forClass(Gerencia.class);
        verify(gerencias).save(captor.capture());
        assertThat(criada).isSameAs(captor.getValue());
        assertThat(criada.getNome()).isEqualTo("Gerência Central");
        assertThat(criada.getGerente()).isSameAs(gerente);
    }

    @Test
    void rejeitaGerenteInativoOuSemPerfilGerente() {
        when(gerente.isAtivo()).thenReturn(false);
        when(usuarios.findById(10L)).thenReturn(Optional.of(gerente));

        assertThatThrownBy(() -> service.create("Gerência Central", 10L))
            .isInstanceOf(GerenciaService.GerenteInvalidoException.class);

        verify(gerencias, never()).save(any());
    }

    @Test
    void rejeitaGerenteInexistente() {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("Gerência Central", 99L))
            .isInstanceOf(GerenciaService.UsuarioOrganizacionalNotFoundException.class);

        verify(gerencias, never()).save(any());
    }

    @Test
    void mantemOGerenteAtualQuandoAtualizacaoParcialNaoInformaOutro() {
        Gerencia existente = new Gerencia("Gerência Central", gerente);
        when(gerencias.findById(1L)).thenReturn(Optional.of(existente));

        Gerencia atualizada = service.update(1L, "Novo nome", null, null);

        assertThat(atualizada.getGerente()).isSameAs(gerente);
        assertThat(atualizada.getNome()).isEqualTo("Novo nome");
    }
}
