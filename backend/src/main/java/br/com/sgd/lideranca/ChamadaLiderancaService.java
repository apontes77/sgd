package br.com.sgd.lideranca;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.sgd.frequencia.SituacaoFrequencia;
import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;

@Service
public class ChamadaLiderancaService {
  private final ChamadaLiderancaRepository chamadas;
  private final DiscipuladoRepository discipulados;
  private final UserRepository usuarios;
  private final Clock clock;

  public ChamadaLiderancaService(
      ChamadaLiderancaRepository chamadas,
      DiscipuladoRepository discipulados,
      UserRepository usuarios,
      Clock clock) {
    this.chamadas = chamadas;
    this.discipulados = discipulados;
    this.usuarios = usuarios;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public ChamadaLiderancaResponse consultar(User ator, LocalDate data) {
    exigirAdmin(ator);
    if (data == null) throw badRequest("A data é obrigatória.");

    List<Discipulado> ativos =
        discipulados.findAllByAtivoTrue().stream()
            .sorted(
                Comparator.comparing(
                        (Discipulado d) -> d.getGerencia().getNome(), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Discipulado::getNome, String.CASE_INSENSITIVE_ORDER))
            .toList();
    ChamadaLideranca salva = chamadas.findByData(data).orElse(null);

    Map<Long, ChamadaLiderancaDiscipulado> itensSalvos = new HashMap<>();
    if (salva != null) {
      for (ChamadaLiderancaDiscipulado item : salva.getItens()) {
        itensSalvos.put(item.getDiscipulado().getId(), item);
      }
    }

    List<DiscipuladoChamadaResponse> discipuladoResponses = new ArrayList<>();
    for (Discipulado d : ativos) {
      ChamadaLiderancaDiscipulado item = itensSalvos.get(d.getId());
      Map<Long, SituacaoFrequencia> situacoes = new HashMap<>();
      if (item != null) {
        for (PresencaLideranca p : item.getPresencas()) {
          situacoes.put(p.getUsuario().getId(), p.getSituacao());
        }
      }
      discipuladoResponses.add(
          montarDiscipulado(d, item == null ? null : item.getObservacao(), situacoes));
    }

    return new ChamadaLiderancaResponse(
        salva == null ? null : salva.getId(),
        data,
        salva == null ? null : salva.getObservacaoGeral(),
        discipuladoResponses);
  }

  @Transactional
  public ChamadaLiderancaResponse salvar(User ator, SalvarChamadaLiderancaCommand comando) {
    exigirAdmin(ator);
    if (comando == null || comando.data() == null) throw badRequest("A data é obrigatória.");
    if (comando.discipulados() == null) throw badRequest("A lista de discipulados é obrigatória.");

    Instant agora = clock.instant();
    ChamadaLideranca chamada =
        chamadas.findByData(comando.data()).orElseGet(() -> new ChamadaLideranca(comando.data()));
    chamada.atualizarObservacaoGeral(comando.observacaoGeral(), agora);

    Map<Long, Discipulado> ativosPorId =
        discipulados.findAllByAtivoTrue().stream()
            .collect(Collectors.toMap(Discipulado::getId, d -> d, (a, b) -> a, LinkedHashMap::new));

    Set<Long> vistos = new HashSet<>();
    List<ChamadaLiderancaDiscipulado> novosItens = new ArrayList<>();
    for (DiscipuladoChamadaCommand itemCmd : comando.discipulados()) {
      if (itemCmd == null || itemCmd.discipuladoId() == null)
        throw badRequest("Cada item deve informar o discipulado.");
      if (!vistos.add(itemCmd.discipuladoId()))
        throw badRequest("Discipulado duplicado no payload: " + itemCmd.discipuladoId());
      Discipulado discipulado = ativosPorId.get(itemCmd.discipuladoId());
      if (discipulado == null)
        throw badRequest("Discipulado inativo ou inexistente: " + itemCmd.discipuladoId());

      List<PresencaLideranca> presencas =
          montarPresencas(
              discipulado, itemCmd.presencas() == null ? List.of() : itemCmd.presencas());
      ChamadaLiderancaDiscipulado item =
          new ChamadaLiderancaDiscipulado(discipulado, itemCmd.observacao());
      item.substituirPresencas(presencas);
      novosItens.add(item);
    }

    chamada.substituirItens(novosItens, agora);
    chamadas.save(chamada);
    return consultar(ator, comando.data());
  }

  private List<PresencaLideranca> montarPresencas(
      Discipulado discipulado, List<PresencaCommand> comandos) {
    Map<Long, PresencaCommand> porUsuario = new LinkedHashMap<>();
    for (PresencaCommand cmd : comandos) {
      if (cmd == null || cmd.usuarioId() == null || cmd.papel() == null || cmd.situacao() == null)
        throw badRequest("Cada presença precisa de usuarioId, papel e situacao.");
      if (porUsuario.put(cmd.usuarioId(), cmd) != null)
        throw badRequest("Usuário duplicado nas presenças do discipulado " + discipulado.getId());
    }

    Long discipuladorId = discipulado.getDiscipulador().getId();
    Set<Long> coLiderIds =
        discipulado.getCoLideres().stream().map(User::getId).collect(Collectors.toSet());

    Set<Long> esperados = new HashSet<>();
    esperados.add(discipuladorId);
    esperados.addAll(coLiderIds);

    if (!porUsuario.keySet().equals(esperados))
      throw badRequest(
          "As presenças do discipulado "
              + discipulado.getNome()
              + " devem cobrir exatamente o discipulador e os co-líderes atuais.");

    List<PresencaLideranca> resultado = new ArrayList<>();
    PresencaCommand cmdLider = porUsuario.get(discipuladorId);
    if (cmdLider.papel() != PapelLideranca.DISCIPULADOR)
      throw badRequest("O discipulador deve ter papel DISCIPULADOR.");
    User lider =
        usuarios
            .findById(discipuladorId)
            .orElseThrow(() -> notFound("Usuário não encontrado: " + discipuladorId));
    resultado.add(new PresencaLideranca(lider, PapelLideranca.DISCIPULADOR, cmdLider.situacao()));

    for (Long coId : coLiderIds.stream().sorted().toList()) {
      PresencaCommand cmdCo = porUsuario.get(coId);
      if (cmdCo.papel() != PapelLideranca.CO_LIDER)
        throw badRequest("Co-líder deve ter papel CO_LIDER.");
      User co =
          usuarios.findById(coId).orElseThrow(() -> notFound("Usuário não encontrado: " + coId));
      resultado.add(new PresencaLideranca(co, PapelLideranca.CO_LIDER, cmdCo.situacao()));
    }
    return resultado;
  }

  private static DiscipuladoChamadaResponse montarDiscipulado(
      Discipulado d, String observacao, Map<Long, SituacaoFrequencia> situacoes) {
    List<PresencaResponse> presencas = new ArrayList<>();
    User lider = d.getDiscipulador();
    presencas.add(
        new PresencaResponse(
            lider.getId(),
            lider.getNome(),
            PapelLideranca.DISCIPULADOR,
            situacoes.get(lider.getId())));
    d.getCoLideres().stream()
        .sorted(Comparator.comparing(User::getNome, String.CASE_INSENSITIVE_ORDER))
        .forEach(
            co ->
                presencas.add(
                    new PresencaResponse(
                        co.getId(),
                        co.getNome(),
                        PapelLideranca.CO_LIDER,
                        situacoes.get(co.getId()))));
    return new DiscipuladoChamadaResponse(
        d.getId(), d.getNome(), d.getGerencia().getNome(), observacao, presencas);
  }

  private static void exigirAdmin(User ator) {
    if (ator == null || !ator.getPerfis().contains(Role.ADMIN))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
  }

  private static ResponseStatusException badRequest(String mensagem) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
  }

  private static ResponseStatusException notFound(String mensagem) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
  }

  public record SalvarChamadaLiderancaCommand(
      LocalDate data, String observacaoGeral, List<DiscipuladoChamadaCommand> discipulados) {}

  public record DiscipuladoChamadaCommand(
      Long discipuladoId, String observacao, List<PresencaCommand> presencas) {}

  public record PresencaCommand(
      Long usuarioId, PapelLideranca papel, SituacaoFrequencia situacao) {}

  public record ChamadaLiderancaResponse(
      Long id,
      LocalDate data,
      String observacaoGeral,
      List<DiscipuladoChamadaResponse> discipulados) {}

  public record DiscipuladoChamadaResponse(
      long discipuladoId,
      String discipuladoNome,
      String gerenciaNome,
      String observacao,
      List<PresencaResponse> presencas) {}

  public record PresencaResponse(
      long usuarioId, String nome, PapelLideranca papel, SituacaoFrequencia situacao) {}
}
