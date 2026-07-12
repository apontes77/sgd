package br.com.sgd.organizacao;

import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DiscipuladoService {
    private final DiscipuladoRepository discipulados;
    private final GerenciaRepository gerencias;
    private final UserRepository usuarios;

    public DiscipuladoService(DiscipuladoRepository discipulados, GerenciaRepository gerencias, UserRepository usuarios) {
        this.discipulados = discipulados;
        this.gerencias = gerencias;
        this.usuarios = usuarios;
    }

    public Discipulado create(String nome, Sexo sexo, long gerenciaId, long discipuladorId) {
        return discipulados.save(new Discipulado(nome, sexo, gerenciaAtiva(gerenciaId), discipuladorAtivo(discipuladorId)));
    }

    public Discipulado update(long id, String nome, Sexo sexo, Long gerenciaId, Long discipuladorId, Boolean ativo) {
        Discipulado discipulado = findById(id);
        Gerencia gerencia = gerenciaId == null ? null : gerenciaAtiva(gerenciaId);
        User discipulador = discipuladorId == null ? null : discipuladorAtivo(discipuladorId);
        if (Boolean.TRUE.equals(ativo) && gerencia == null && !discipulado.getGerencia().isAtivo()) {
            throw new GerenciaInativaException();
        }
        discipulado.update(nome, sexo, gerencia, discipulador, ativo);
        return discipulado;
    }

    public Discipulado replaceCoLideres(long discipuladoId, Collection<Long> usuarioIds) {
        if (usuarioIds == null) throw new IllegalArgumentException("A lista de co-líderes é obrigatória.");
        if (usuarioIds.size() > Discipulado.MAX_CO_LIDERES || new LinkedHashSet<>(usuarioIds).size() != usuarioIds.size()) {
            throw new Discipulado.CoLiderLimitExceededException();
        }
        Discipulado discipulado = findById(discipuladoId);
        Set<User> coLideres = new LinkedHashSet<>();
        for (Long usuarioId : usuarioIds) {
            if (usuarioId == null) throw new CoLiderInvalidoException();
            User coLider = usuarios.findById(usuarioId).orElseThrow(UsuarioOrganizacionalNotFoundException::new);
            if (!coLider.isAtivo() || !coLider.getPerfis().contains(Role.CO_LIDER)) throw new CoLiderInvalidoException();
            if (coLider.getId().equals(discipulado.getDiscipulador().getId())) throw new CoLiderInvalidoException();
            coLideres.add(coLider);
        }
        discipulado.replaceCoLideres(coLideres);
        return discipulado;
    }

    @Transactional(readOnly = true)
    public Page<Discipulado> list(Long gerenciaId, Boolean ativo, Pageable pageable) {
        if (gerenciaId != null && ativo != null) return discipulados.findAllByGerenciaIdAndAtivo(gerenciaId, ativo, pageable);
        if (gerenciaId != null) return discipulados.findAllByGerenciaId(gerenciaId, pageable);
        if (ativo != null) return discipulados.findAllByAtivo(ativo, pageable);
        return discipulados.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Discipulado findById(long id) {
        return discipulados.findById(id).orElseThrow(DiscipuladoNotFoundException::new);
    }

    private Gerencia gerenciaAtiva(long id) {
        Gerencia gerencia = gerencias.findById(id).orElseThrow(GerenciaNotFoundException::new);
        if (!gerencia.isAtivo()) throw new GerenciaInativaException();
        return gerencia;
    }

    private User discipuladorAtivo(long id) {
        User usuario = usuarios.findById(id).orElseThrow(UsuarioOrganizacionalNotFoundException::new);
        if (!usuario.isAtivo() || !usuario.getPerfis().contains(Role.DISCIPULADOR)) {
            throw new DiscipuladorInvalidoException();
        }
        return usuario;
    }

    public static class DiscipuladoNotFoundException extends RuntimeException { }
    public static class GerenciaNotFoundException extends RuntimeException { }
    public static class GerenciaInativaException extends RuntimeException { }
    public static class UsuarioOrganizacionalNotFoundException extends RuntimeException { }
    public static class DiscipuladorInvalidoException extends RuntimeException { }
    public static class CoLiderInvalidoException extends RuntimeException { }
}
