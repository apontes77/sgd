package br.com.sgd.lideranca;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class ChamadaLiderancaConflitos {
  private ChamadaLiderancaConflitos() {}

  static Map<Long, ChamadaLiderancaService.RegistroDoDiaResponse> indexar(
      ChamadaLideranca chamada) {
    Map<Long, ChamadaLiderancaService.RegistroDoDiaResponse> registros = new LinkedHashMap<>();
    if (chamada == null) return registros;
    for (ChamadaLiderancaDiscipulado item : chamada.getItens()) {
      long discipuladoId = item.getDiscipulado().getId();
      String grupo = item.getDiscipulado().getNome();
      for (PresencaLideranca presenca : item.getPresencas()) {
        registros.putIfAbsent(
            presenca.getUsuario().getId(),
            new ChamadaLiderancaService.RegistroDoDiaResponse(
                discipuladoId, grupo, presenca.getSituacao()));
      }
    }
    return registros;
  }

  static Map<Long, Long> destinosDoPayload(List<ChamadaLiderancaDiscipulado> novosItens) {
    Map<Long, Long> destinos = new LinkedHashMap<>();
    for (ChamadaLiderancaDiscipulado item : novosItens) {
      Long discipuladoId = item.getDiscipulado().getId();
      for (PresencaLideranca presenca : item.getPresencas()) {
        destinos.put(presenca.getUsuario().getId(), discipuladoId);
      }
    }
    return destinos;
  }

  static void exigirSemDuplicidadeNoPayload(List<ChamadaLiderancaDiscipulado> novosItens) {
    Map<Long, String> discipuladoNoPayload = new LinkedHashMap<>();
    for (ChamadaLiderancaDiscipulado item : novosItens) {
      String grupo = item.getDiscipulado().getNome();
      for (PresencaLideranca presenca : item.getPresencas()) {
        Long usuarioId = presenca.getUsuario().getId();
        String anterior = discipuladoNoPayload.put(usuarioId, grupo);
        if (anterior != null) {
          throw new ResponseStatusException(
              HttpStatus.CONFLICT,
              mensagemDuplicidadeNoPayload(presenca.getUsuario().getNome(), anterior, grupo));
        }
      }
    }
  }

  static List<ChamadaLiderancaService.ConflitoPresenca> detectar(
      ChamadaLideranca chamada, List<ChamadaLiderancaDiscipulado> novosItens) {
    Map<Long, ChamadaLiderancaService.RegistroDoDiaResponse> jaLancados = indexar(chamada);
    List<ChamadaLiderancaService.ConflitoPresenca> conflitos = new ArrayList<>();
    for (ChamadaLiderancaDiscipulado item : novosItens) {
      long discipuladoId = item.getDiscipulado().getId();
      for (PresencaLideranca presenca : item.getPresencas()) {
        ChamadaLiderancaService.RegistroDoDiaResponse existente =
            jaLancados.get(presenca.getUsuario().getId());
        if (existente == null) continue;
        if (existente.discipuladoId() == discipuladoId
            && existente.situacao() == presenca.getSituacao()) continue;
        conflitos.add(
            new ChamadaLiderancaService.ConflitoPresenca(
                presenca.getUsuario().getId(),
                presenca.getUsuario().getNome(),
                existente.discipuladoId(),
                existente.discipuladoNome(),
                existente.situacao()));
      }
    }
    return conflitos;
  }

  private static String mensagemDuplicidadeNoPayload(String nome, String grupoA, String grupoB) {
    return "Não é possível lançar "
        + nome
        + " em "
        + grupoA
        + " e em "
        + grupoB
        + " na mesma sexta-feira. A chamada de liderança aceita apenas um lançamento por pessoa no mesmo dia.";
  }
}
