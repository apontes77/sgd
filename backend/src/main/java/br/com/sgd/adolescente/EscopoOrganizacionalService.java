package br.com.sgd.adolescente;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.Gerencia;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;

@Service
public class EscopoOrganizacionalService {
  public boolean podeLer(User usuario, Discipulado discipulado) {
    if (usuario.getPerfis().contains(Role.ADMIN)) return true;
    if (gerenteDoDiscipulado(usuario, discipulado)) return true;
    return liderDoDiscipulado(usuario, discipulado);
  }

  public boolean podeAlterar(User usuario, Discipulado discipulado) {
    if (usuario.getPerfis().contains(Role.ADMIN)) return true;
    if (gerenteDoDiscipulado(usuario, discipulado)) return true;
    return liderDoDiscipulado(usuario, discipulado);
  }

  /** Frequência/encontros: somente ADMIN ou liderança do discipulado (sem gerente). */
  public boolean podeRegistrarFrequencia(User usuario, Discipulado discipulado) {
    return usuario.getPerfis().contains(Role.ADMIN) || liderDoDiscipulado(usuario, discipulado);
  }

  public void exigirLeitura(User usuario, Discipulado discipulado) {
    if (!podeLer(usuario, discipulado))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "O usuário não possui acesso a este discipulado.");
  }

  public void exigirAlteracao(User usuario, Discipulado discipulado) {
    if (!podeAlterar(usuario, discipulado))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "O usuário não pode alterar dados deste discipulado.");
  }

  public void exigirRegistroFrequencia(User usuario, Discipulado discipulado) {
    if (!podeRegistrarFrequencia(usuario, discipulado))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "O usuário não pode registrar frequência neste discipulado.");
  }

  private boolean gerenteDoDiscipulado(User usuario, Discipulado discipulado) {
    Gerencia gerencia = discipulado.getGerencia();
    return usuario.getPerfis().contains(Role.GERENTE)
        && gerencia != null
        && gerencia.getGerente().getId().equals(usuario.getId());
  }

  private boolean liderDoDiscipulado(User usuario, Discipulado discipulado) {
    if (usuario.getPerfis().contains(Role.DISCIPULADOR)
        && discipulado.getDiscipulador().getId().equals(usuario.getId())) return true;
    return usuario.getPerfis().contains(Role.CO_LIDER)
        && discipulado.getCoLideres().stream().anyMatch(u -> u.getId().equals(usuario.getId()));
  }
}
