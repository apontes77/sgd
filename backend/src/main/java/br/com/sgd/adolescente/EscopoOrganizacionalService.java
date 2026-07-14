package br.com.sgd.adolescente;

import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EscopoOrganizacionalService {
    public boolean podeLer(User usuario, Discipulado discipulado) {
        if (usuario.getPerfis().contains(Role.ADMIN)) return true;
        if (usuario.getPerfis().contains(Role.GERENTE) && discipulado.getGerencia().getGerente().getId().equals(usuario.getId())) return true;
        return liderDoDiscipulado(usuario, discipulado);
    }

    public boolean podeAlterar(User usuario, Discipulado discipulado) {
        return usuario.getPerfis().contains(Role.ADMIN) || liderDoDiscipulado(usuario, discipulado);
    }

    public void exigirLeitura(User usuario, Discipulado discipulado) {
        if (!podeLer(usuario, discipulado)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O usuário não possui acesso a este discipulado.");
    }

    public void exigirAlteracao(User usuario, Discipulado discipulado) {
        if (!podeAlterar(usuario, discipulado)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O usuário não pode alterar dados deste discipulado.");
    }

    private boolean liderDoDiscipulado(User usuario, Discipulado discipulado) {
        if (usuario.getPerfis().contains(Role.DISCIPULADOR) && discipulado.getDiscipulador().getId().equals(usuario.getId())) return true;
        return usuario.getPerfis().contains(Role.CO_LIDER) && discipulado.getCoLideres().stream().anyMatch(u -> u.getId().equals(usuario.getId()));
    }
}
