package br.com.sgd.organizacao;

import java.util.Collection;
import java.util.List;

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
public class GerenciaService {
  private final GerenciaRepository gerencias;
  private final DiscipuladoRepository discipulados;
  private final UserRepository usuarios;

  public GerenciaService(
      GerenciaRepository gerencias, DiscipuladoRepository discipulados, UserRepository usuarios) {
    this.gerencias = gerencias;
    this.discipulados = discipulados;
    this.usuarios = usuarios;
  }

  public Gerencia create(
      String nome, Sexo sexo, Collection<FaixaEtaria> faixasEtarias, long gerenteId) {
    return gerencias.save(new Gerencia(nome, sexo, faixasEtarias, gerenteAtivo(gerenteId)));
  }

  public Gerencia update(
      long id,
      String nome,
      Sexo sexo,
      Collection<FaixaEtaria> faixasEtarias,
      Long gerenteId,
      Boolean ativo) {
    Gerencia gerencia = findById(id);
    User gerente = gerenteId == null ? null : gerenteAtivo(gerenteId);
    boolean deveInativarDiscipulados = Boolean.FALSE.equals(ativo) && gerencia.isAtivo();
    gerencia.update(nome, sexo, faixasEtarias, gerente, ativo);
    if (deveInativarDiscipulados) {
      List<Discipulado> discipuladosAtivos = discipulados.findAllByGerenciaIdAndAtivoTrue(id);
      discipuladosAtivos.forEach(
          discipulado -> discipulado.update(null, null, null, null, null, false));
    }
    return gerencia;
  }

  @Transactional(readOnly = true)
  public Page<Gerencia> list(Sexo sexo, FaixaEtaria faixaEtaria, Pageable pageable) {
    Specification<Gerencia> filtro = Specification.where(null);
    if (sexo != null) filtro = filtro.and((root, query, cb) -> cb.equal(root.get("sexo"), sexo));
    if (faixaEtaria != null) {
      filtro =
          filtro.and(
              (root, query, cb) -> {
                query.distinct(true);
                return cb.isMember(faixaEtaria, root.get("faixasEtarias"));
              });
    }
    return gerencias.findAll(filtro, pageable);
  }

  @Transactional(readOnly = true)
  public Gerencia findById(long id) {
    return gerencias.findById(id).orElseThrow(GerenciaNotFoundException::new);
  }

  public void delete(long id) {
    Gerencia gerencia = findById(id);
    if (discipulados.existsByGerenciaId(id)) {
      throw new GerenciaComDiscipuladosException();
    }
    gerencias.delete(gerencia);
  }

  private User gerenteAtivo(long id) {
    User usuario = usuarios.findById(id).orElseThrow(UsuarioOrganizacionalNotFoundException::new);
    if (!usuario.isAtivo() || !usuario.getPerfis().contains(Role.GERENTE)) {
      throw new GerenteInvalidoException();
    }
    return usuario;
  }

  public static class GerenciaNotFoundException extends RuntimeException {}

  public static class UsuarioOrganizacionalNotFoundException extends RuntimeException {}

  public static class GerenteInvalidoException extends RuntimeException {}

  public static class GerenciaComDiscipuladosException extends RuntimeException {}
}
