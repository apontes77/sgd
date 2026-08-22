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
import br.com.sgd.organizacao.Sexo;
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

    Map<Long, RegistroDoDiaResponse> registrosDoDia = ChamadaLiderancaConflitos.indexar(salva);

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
          montarDiscipulado(
              d, item == null ? null : item.getObservacao(), situacoes, registrosDoDia));
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

    List<ChamadaLiderancaDiscipulado> novosItens =
        montarItensDoPayload(comando.discipulados(), ativosPorId);
    aplicarPresencas(chamada, novosItens, comando.confirmarAtualizacao(), agora);
    chamadas.save(chamada);
    return consultar(ator, comando.data());
  }

  private List<ChamadaLiderancaDiscipulado> montarItensDoPayload(
      List<DiscipuladoChamadaCommand> comandos, Map<Long, Discipulado> ativosPorId) {
    Set<Long> vistos = new HashSet<>();
    List<ChamadaLiderancaDiscipulado> novosItens = new ArrayList<>();
    for (DiscipuladoChamadaCommand itemCmd : comandos) {
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
    return novosItens;
  }

  private static void aplicarPresencas(
      ChamadaLideranca chamada,
      List<ChamadaLiderancaDiscipulado> novosItens,
      boolean confirmarAtualizacao,
      Instant agora) {
    ChamadaLiderancaConflitos.exigirSemDuplicidadeNoPayload(novosItens);
    List<ConflitoPresenca> conflitos = ChamadaLiderancaConflitos.detectar(chamada, novosItens);
    if (!conflitos.isEmpty() && !confirmarAtualizacao) {
      throw new AtualizacaoPresencaNaoConfirmadaException(conflitos);
    }
    if (confirmarAtualizacao) {
      chamada.removerPresencasDeOutrosDiscipulados(
          ChamadaLiderancaConflitos.destinosDoPayload(novosItens));
    }
    chamada.mesclarItens(novosItens, agora);
  }

  private List<PresencaLideranca> montarPresencas(
      Discipulado discipulado, List<PresencaCommand> comandos) {
    Map<Long, PresencaCommand> porUsuario = indexarPresencas(discipulado.getId(), comandos);
    Long discipuladorId = discipulado.getDiscipulador().getId();
    Set<Long> coLiderIds =
        discipulado.getCoLideres().stream().map(User::getId).collect(Collectors.toSet());
    exigirSomenteLideresAtuais(
        discipulado.getNome(), porUsuario.keySet(), discipuladorId, coLiderIds);

    List<PresencaLideranca> resultado = new ArrayList<>();
    PresencaCommand cmdLider = porUsuario.get(discipuladorId);
    if (cmdLider != null) {
      resultado.add(
          presencaDoPapel(
              discipuladorId,
              cmdLider,
              PapelLideranca.DISCIPULADOR,
              "O discipulador deve ter papel DISCIPULADOR."));
    }
    for (Long coId : coLiderIds.stream().sorted().toList()) {
      PresencaCommand cmdCo = porUsuario.get(coId);
      if (cmdCo == null) continue;
      resultado.add(
          presencaDoPapel(
              coId, cmdCo, PapelLideranca.CO_LIDER, "Co-líder deve ter papel CO_LIDER."));
    }
    return resultado;
  }

  private static Map<Long, PresencaCommand> indexarPresencas(
      Long discipuladoId, List<PresencaCommand> comandos) {
    Map<Long, PresencaCommand> porUsuario = new LinkedHashMap<>();
    for (PresencaCommand cmd : comandos) {
      if (cmd == null || cmd.usuarioId() == null || cmd.papel() == null || cmd.situacao() == null)
        throw badRequest("Cada presença precisa de usuarioId, papel e situacao.");
      if (porUsuario.put(cmd.usuarioId(), cmd) != null)
        throw badRequest("Usuário duplicado nas presenças do discipulado " + discipuladoId);
    }
    return porUsuario;
  }

  private static void exigirSomenteLideresAtuais(
      String discipuladoNome, Set<Long> enviados, Long discipuladorId, Set<Long> coLiderIds) {
    Set<Long> esperados = new HashSet<>();
    esperados.add(discipuladorId);
    esperados.addAll(coLiderIds);
    if (!esperados.containsAll(enviados))
      throw badRequest(
          "As presenças do discipulado "
              + discipuladoNome
              + " devem conter somente o discipulador e os co-líderes atuais.");
  }

  private PresencaLideranca presencaDoPapel(
      Long usuarioId, PresencaCommand cmd, PapelLideranca esperado, String mensagemPapel) {
    if (cmd.papel() != esperado) throw badRequest(mensagemPapel);
    User usuario =
        usuarios
            .findById(usuarioId)
            .orElseThrow(() -> notFound("Usuário não encontrado: " + usuarioId));
    return new PresencaLideranca(usuario, esperado, cmd.situacao());
  }

  private static DiscipuladoChamadaResponse montarDiscipulado(
      Discipulado d,
      String observacao,
      Map<Long, SituacaoFrequencia> situacoes,
      Map<Long, RegistroDoDiaResponse> registrosDoDia) {
    List<PresencaResponse> presencas = new ArrayList<>();
    User lider = d.getDiscipulador();
    presencas.add(presencaDaGrade(lider, PapelLideranca.DISCIPULADOR, situacoes, registrosDoDia));
    d.getCoLideres().stream()
        .sorted(Comparator.comparing(User::getNome, String.CASE_INSENSITIVE_ORDER))
        .forEach(
            co ->
                presencas.add(
                    presencaDaGrade(co, PapelLideranca.CO_LIDER, situacoes, registrosDoDia)));
    return new DiscipuladoChamadaResponse(
        d.getId(), d.getNome(), d.getSexo(), d.getGerencia().getNome(), observacao, presencas);
  }

  private static PresencaResponse presencaDaGrade(
      User usuario,
      PapelLideranca papel,
      Map<Long, SituacaoFrequencia> situacoes,
      Map<Long, RegistroDoDiaResponse> registrosDoDia) {
    return new PresencaResponse(
        usuario.getId(),
        usuario.getNome(),
        papel,
        situacoes.get(usuario.getId()),
        registrosDoDia.get(usuario.getId()));
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
      LocalDate data,
      String observacaoGeral,
      List<DiscipuladoChamadaCommand> discipulados,
      boolean confirmarAtualizacao) {}

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
      Sexo sexo,
      String gerenciaNome,
      String observacao,
      List<PresencaResponse> presencas) {}

  public record PresencaResponse(
      long usuarioId,
      String nome,
      PapelLideranca papel,
      SituacaoFrequencia situacao,
      RegistroDoDiaResponse registroDoDia) {}

  public record RegistroDoDiaResponse(
      long discipuladoId, String discipuladoNome, SituacaoFrequencia situacao) {}

  public record ConflitoPresenca(
      long usuarioId,
      String nome,
      long discipuladoId,
      String discipuladoNome,
      SituacaoFrequencia situacao) {}

  public static class AtualizacaoPresencaNaoConfirmadaException extends RuntimeException {
    private final List<ConflitoPresenca> conflitos;

    AtualizacaoPresencaNaoConfirmadaException(List<ConflitoPresenca> conflitos) {
      super(
          "Este discipulador/co-líder já teve chamada salva. Tem certeza que quer atualizar essa chamada?");
      this.conflitos = List.copyOf(conflitos);
    }

    public List<ConflitoPresenca> getConflitos() {
      return conflitos;
    }
  }
}
