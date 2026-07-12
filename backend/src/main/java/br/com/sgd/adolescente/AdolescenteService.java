package br.com.sgd.adolescente;

import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AdolescenteService {
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Sao_Paulo");
    private final AdolescenteRepository adolescentes;
    private final VinculoAdolescenteRepository vinculos;
    private final DiscipuladoRepository discipulados;
    private final EscopoOrganizacionalService escopo;

    public AdolescenteService(AdolescenteRepository adolescentes, VinculoAdolescenteRepository vinculos,
            DiscipuladoRepository discipulados, EscopoOrganizacionalService escopo) {
        this.adolescentes = adolescentes; this.vinculos = vinculos; this.discipulados = discipulados; this.escopo = escopo;
    }

    public Adolescente criar(User usuario, DadosAdolescente dados) {
        Discipulado discipulado = discipuladoAtivo(dados.discipuladoId());
        escopo.exigirAlteracao(usuario, discipulado);
        Adolescente adolescente = new Adolescente(dados.nome(), dados.dataNascimento(), dados.telefone(), dados.instagram());
        if (Boolean.FALSE.equals(dados.ativo())) adolescente.atualizar(dados.nome(), dados.dataNascimento(), dados.telefone(), dados.instagram(), false);
        adolescente = adolescentes.save(adolescente);
        vinculos.save(new VinculoAdolescenteDiscipulado(adolescente, discipulado, LocalDate.now(ZONA_NEGOCIO)));
        return adolescente;
    }

    public Adolescente atualizar(User usuario, long id, DadosAdolescente dados) {
        Adolescente adolescente = buscar(id);
        VinculoAdolescenteDiscipulado atual = vinculoAtivo(id);
        escopo.exigirAlteracao(usuario, atual.getDiscipulado());
        if (!atual.getDiscipulado().getId().equals(dados.discipuladoId())) {
            throw conflito("Use o endpoint de vínculos para transferir o adolescente.");
        }
        adolescente.atualizar(dados.nome(), dados.dataNascimento(), dados.telefone(), dados.instagram(), dados.ativo());
        return adolescente;
    }

    public VinculoAdolescenteDiscipulado transferir(User usuario, long adolescenteId, long discipuladoId, LocalDate dataInicio) {
        Adolescente adolescente = buscar(adolescenteId);
        VinculoAdolescenteDiscipulado atual = vinculoAtivo(adolescenteId);
        Discipulado destino = discipuladoAtivo(discipuladoId);
        escopo.exigirAlteracao(usuario, atual.getDiscipulado());
        escopo.exigirAlteracao(usuario, destino);
        if (atual.getDiscipulado().getId().equals(discipuladoId)) throw conflito("O adolescente já pertence ao discipulado informado.");
        if (dataInicio == null || !dataInicio.isAfter(atual.getDataInicio())) throw conflito("A transferência deve ocorrer após o início do vínculo atual.");
        atual.encerrar(dataInicio.minusDays(1));
        return vinculos.save(new VinculoAdolescenteDiscipulado(adolescente, destino, dataInicio));
    }

    @Transactional(readOnly = true)
    public Page<Adolescente> listar(User usuario, Long discipuladoId, Boolean ativo, Pageable pageable) {
        if (discipuladoId != null) escopo.exigirLeitura(usuario, discipuladoAtivoOuInativo(discipuladoId));
        Specification<Adolescente> filtro = Specification.where(ativo == null ? null : (root, query, cb) -> cb.equal(root.get("ativo"), ativo));
        if (discipuladoId != null) filtro = filtro.and(noDiscipulado(discipuladoId));
        filtro = filtro.and(noEscopo(usuario));
        return adolescentes.findAll(filtro, pageable);
    }

    @Transactional(readOnly = true)
    public VinculoAdolescenteDiscipulado vinculoAtual(long adolescenteId) { return vinculos.findFirstByAdolescenteIdAndAtivoTrue(adolescenteId).orElseThrow(() -> conflito("O adolescente não possui vínculo ativo.")); }

    private Specification<Adolescente> noDiscipulado(long discipuladoId) {
        return (root, query, cb) -> {
            var sub = query.subquery(Long.class);
            var v = sub.from(VinculoAdolescenteDiscipulado.class);
            sub.select(v.get("adolescente").get("id")).where(cb.isTrue(v.get("ativo")), cb.equal(v.get("discipulado").get("id"), discipuladoId));
            return root.get("id").in(sub);
        };
    }

    private Specification<Adolescente> noEscopo(User usuario) {
        if (usuario.getPerfis().contains(Role.ADMIN)) return null;
        return (root, query, cb) -> {
            var sub = query.subquery(Long.class);
            var v = sub.from(VinculoAdolescenteDiscipulado.class);
            var d = v.join("discipulado", JoinType.INNER);
            List<jakarta.persistence.criteria.Predicate> acessos = new ArrayList<>();
            if (usuario.getPerfis().contains(Role.GERENTE)) acessos.add(cb.equal(d.get("gerencia").get("gerente").get("id"), usuario.getId()));
            if (usuario.getPerfis().contains(Role.DISCIPULADOR)) acessos.add(cb.equal(d.get("discipulador").get("id"), usuario.getId()));
            if (usuario.getPerfis().contains(Role.CO_LIDER)) {
                var coLider = d.join("coLideres", JoinType.LEFT);
                acessos.add(cb.equal(coLider.get("id"), usuario.getId()));
            }
            if (acessos.isEmpty()) return cb.disjunction();
            sub.select(v.get("adolescente").get("id")).where(cb.isTrue(v.get("ativo")), cb.or(acessos.toArray(jakarta.persistence.criteria.Predicate[]::new)));
            return root.get("id").in(sub);
        };
    }

    private Adolescente buscar(long id) { return adolescentes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adolescente não encontrado.")); }
    private VinculoAdolescenteDiscipulado vinculoAtivo(long id) { return vinculos.findByAdolescenteIdAndAtivoTrue(id).orElseThrow(() -> conflito("O adolescente não possui vínculo ativo.")); }
    private Discipulado discipuladoAtivo(long id) {
        Discipulado d = discipuladoAtivoOuInativo(id);
        if (!d.isAtivo() || !d.getGerencia().isAtivo()) throw conflito("O discipulado informado está inativo.");
        return d;
    }
    private Discipulado discipuladoAtivoOuInativo(long id) { return discipulados.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discipulado não encontrado.")); }
    private static ResponseStatusException conflito(String mensagem) { return new ResponseStatusException(HttpStatus.CONFLICT, mensagem); }

    public record DadosAdolescente(String nome, LocalDate dataNascimento, String telefone, String instagram, Long discipuladoId, Boolean ativo) { }
}
