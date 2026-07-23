package br.com.sgd.organizacao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  public Gerencia create(String nome, long gerenteId) {
    return gerencias.save(new Gerencia(nome, gerenteAtivo(gerenteId)));
  }

  public Gerencia update(long id, String nome, Long gerenteId, Boolean ativo) {
    Gerencia gerencia = findById(id);
    User gerente = gerenteId == null ? null : gerenteAtivo(gerenteId);
    boolean deveInativarDiscipulados = Boolean.FALSE.equals(ativo) && gerencia.isAtivo();
    gerencia.update(nome, gerente, ativo);
    if (deveInativarDiscipulados) {
      List<Discipulado> discipuladosAtivos = discipulados.findAllByGerenciaIdAndAtivoTrue(id);
      discipuladosAtivos.forEach(discipulado -> discipulado.update(null, null, null, null, false));
    }
    return gerencia;
  }

  @Transactional(readOnly = true)
  public Page<Gerencia> list(Pageable pageable) {
    return gerencias.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Gerencia findById(long id) {
    return gerencias.findById(id).orElseThrow(GerenciaNotFoundException::new);
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
}
