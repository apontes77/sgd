package br.com.sgd.organizacao;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;

@Service
@Transactional
public class DiscipuladoService {
  private final DiscipuladoRepository discipulados;
  private final GerenciaRepository gerencias;
  private final UserRepository usuarios;

  public DiscipuladoService(
      DiscipuladoRepository discipulados, GerenciaRepository gerencias, UserRepository usuarios) {
    this.discipulados = discipulados;
    this.gerencias = gerencias;
    this.usuarios = usuarios;
  }

  public Discipulado create(
      String nome, Sexo sexo, FaixaEtaria faixaEtaria, long gerenciaId, long discipuladorId) {
    return create(nome, sexo, faixaEtaria, gerenciaId, discipuladorId, false);
  }

  public Discipulado create(
      String nome,
      Sexo sexo,
      FaixaEtaria faixaEtaria,
      Long gerenciaId,
      long discipuladorId,
      boolean emFormacao) {
    User discipulador = discipuladorAtivo(discipuladorId);
    exigirLiderancaLivre(discipulador, emFormacao, null);
    if (emFormacao) {
      if (gerenciaId != null) throw new FormacaoInvalidaException();
      return discipulados.save(new Discipulado(nome, sexo, faixaEtaria, null, discipulador, true));
    }
    if (gerenciaId == null) throw new FormacaoInvalidaException();
    return discipulados.save(
        new Discipulado(nome, sexo, faixaEtaria, gerenciaAtiva(gerenciaId), discipulador, false));
  }

  public Discipulado update(
      long id,
      String nome,
      Sexo sexo,
      FaixaEtaria faixaEtaria,
      Long gerenciaId,
      Long discipuladorId,
      Boolean ativo) {
    Discipulado discipulado = findById(id);
    if (discipulado.isEmFormacao() && gerenciaId != null) throw new FormacaoInvalidaException();
    Gerencia gerencia =
        discipulado.isEmFormacao() || gerenciaId == null ? null : gerenciaAtiva(gerenciaId);
    User discipulador = discipuladorId == null ? null : discipuladorAtivo(discipuladorId);
    if (discipulador != null)
      exigirLiderancaLivre(discipulador, discipulado.isEmFormacao(), discipulado.getId());
    if (Boolean.TRUE.equals(ativo)
        && !discipulado.isEmFormacao()
        && gerencia == null
        && !discipulado.getGerencia().isAtivo()) {
      throw new GerenciaInativaException();
    }
    discipulado.update(nome, sexo, faixaEtaria, gerencia, discipulador, ativo);
    return discipulado;
  }

  public Discipulado replaceCoLideres(long discipuladoId, Collection<Long> usuarioIds) {
    if (usuarioIds == null)
      throw new IllegalArgumentException("A lista de co-líderes é obrigatória.");
    if (usuarioIds.size() > Discipulado.MAX_CO_LIDERES
        || new LinkedHashSet<>(usuarioIds).size() != usuarioIds.size()) {
      throw new Discipulado.CoLiderLimitExceededException();
    }
    Discipulado discipulado = findById(discipuladoId);
    if (discipulado.isEmFormacao()) throw new Discipulado.FormacaoNaoPermiteCoLiderException();
    Set<User> coLideres = new LinkedHashSet<>();
    for (Long usuarioId : usuarioIds) {
      if (usuarioId == null) throw new CoLiderInvalidoException();
      User coLider =
          usuarios.findById(usuarioId).orElseThrow(UsuarioOrganizacionalNotFoundException::new);
      if (!coLider.isAtivo() || !coLider.getPerfis().contains(Role.CO_LIDER))
        throw new CoLiderInvalidoException();
      if (coLider.getId().equals(discipulado.getDiscipulador().getId()))
        throw new CoLiderInvalidoException();
      exigirLiderancaLivre(coLider, false, discipulado.getId());
      coLideres.add(coLider);
    }
    discipulado.replaceCoLideres(coLideres);
    return discipulado;
  }

  @Transactional(readOnly = true)
  public Page<Discipulado> list(
      User usuario, Long gerenciaId, Boolean ativo, Boolean emFormacao, Pageable pageable) {
    Specification<Discipulado> filtro =
        Specification.where(
            gerenciaId == null
                ? null
                : (root, query, cb) -> cb.equal(root.get("gerencia").get("id"), gerenciaId));
    if (ativo != null) filtro = filtro.and((root, query, cb) -> cb.equal(root.get("ativo"), ativo));
    if (emFormacao != null)
      filtro = filtro.and((root, query, cb) -> cb.equal(root.get("emFormacao"), emFormacao));
    filtro = filtro.and(noEscopo(usuario));
    Page<Discipulado> result = discipulados.findAll(filtro, pageable);
    result.forEach(discipulado -> discipulado.getCoLideres().forEach(User::getPerfis));
    return result;
  }

  private Specification<Discipulado> noEscopo(User usuario) {
    if (usuario.getPerfis().contains(Role.ADMIN)) return null;
    return (root, query, cb) -> {
      query.distinct(true);
      var acessos = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
      if (usuario.getPerfis().contains(Role.GERENTE))
        acessos.add(cb.equal(root.get("gerencia").get("gerente").get("id"), usuario.getId()));
      if (usuario.getPerfis().contains(Role.DISCIPULADOR))
        acessos.add(cb.equal(root.get("discipulador").get("id"), usuario.getId()));
      if (usuario.getPerfis().contains(Role.CO_LIDER))
        acessos.add(
            cb.equal(
                root.join("coLideres", jakarta.persistence.criteria.JoinType.LEFT).get("id"),
                usuario.getId()));
      return acessos.isEmpty()
          ? cb.disjunction()
          : cb.or(acessos.toArray(jakarta.persistence.criteria.Predicate[]::new));
    };
  }

  @Transactional(readOnly = true)
  public List<Discipulado> listLiderados(User usuario, Boolean ativo) {
    return discipulados.findAllByLiderancaUsuarioId(usuario.getId()).stream()
        .filter(discipulado -> ativo == null || discipulado.isAtivo() == ativo)
        .peek(discipulado -> discipulado.getCoLideres().forEach(User::getPerfis))
        .sorted(Comparator.comparing(Discipulado::getNome, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  @Transactional(readOnly = true)
  public Discipulado findById(long id) {
    return discipulados.findById(id).orElseThrow(DiscipuladoNotFoundException::new);
  }

  private void exigirLiderancaLivre(User usuario, boolean emFormacao, Long ignorarDiscipuladoId) {
    if (usuario.getId() == null) return;
    boolean ocupado =
        discipulados.findAllByLiderancaUsuarioId(usuario.getId()).stream()
            .anyMatch(
                atual ->
                    atual.isEmFormacao() == emFormacao
                        && (ignorarDiscipuladoId == null
                            || !ignorarDiscipuladoId.equals(atual.getId())));
    if (ocupado) throw new LiderancaDuplicadaException();
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

  public static class DiscipuladoNotFoundException extends RuntimeException {}

  public static class GerenciaNotFoundException extends RuntimeException {}

  public static class GerenciaInativaException extends RuntimeException {}

  public static class UsuarioOrganizacionalNotFoundException extends RuntimeException {}

  public static class DiscipuladorInvalidoException extends RuntimeException {}

  public static class CoLiderInvalidoException extends RuntimeException {}

  public static class FormacaoInvalidaException extends RuntimeException {}

  public static class LiderancaDuplicadaException extends RuntimeException {}
}
