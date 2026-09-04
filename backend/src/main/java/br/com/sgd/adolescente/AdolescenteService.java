package br.com.sgd.adolescente;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.JoinType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.sgd.audit.AuditLog;
import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.familia.FamiliaService;
import br.com.sgd.familia.FichaFamilia;
import br.com.sgd.frequencia.FrequenciaRepository;
import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;

@Service
@Transactional
public class AdolescenteService {
  private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Sao_Paulo");
  private static final int JANELA_GOE_DIAS = 42;
  private static final long MINIMO_FALTAS_GOE = 4;

  /** Janela inclusiva a partir do primeiro vínculo: inicio .. inicio+34 = 35 dias corridos. */
  private static final int JANELA_PROMOCAO_VISITANTE_DIAS = 35;

  private static final long MINIMO_PRESENCAS_PROMOCAO_VISITANTE = 3;
  private final AdolescenteRepository adolescentes;
  private final VinculoAdolescenteRepository vinculos;
  private final DiscipuladoRepository discipulados;
  private final EscopoOrganizacionalService escopo;
  private final AuditLogRepository auditoria;
  private final FrequenciaRepository frequencias;
  private final FamiliaService familias;
  private final Clock clock;

  public AdolescenteService(
      AdolescenteRepository adolescentes,
      VinculoAdolescenteRepository vinculos,
      DiscipuladoRepository discipulados,
      EscopoOrganizacionalService escopo,
      AuditLogRepository auditoria,
      FrequenciaRepository frequencias,
      FamiliaService familias,
      Clock clock) {
    this.adolescentes = adolescentes;
    this.vinculos = vinculos;
    this.discipulados = discipulados;
    this.escopo = escopo;
    this.auditoria = auditoria;
    this.frequencias = frequencias;
    this.familias = familias;
    this.clock = clock;
  }

  public Adolescente criar(User usuario, DadosAdolescente dados, FichaFamilia.DadosFicha familia) {
    Discipulado discipulado = discipuladoAtivo(dados.discipuladoId());
    escopo.exigirAlteracao(usuario, discipulado);
    FichaFamilia.DadosFicha ficha = fichaParaCadastro(usuario, familia);
    CategoriaAdolescente categoria =
        dados.categoria() == null ? CategoriaAdolescente.DISCIPULO : dados.categoria();
    Adolescente adolescente =
        adolescentes.save(
            new Adolescente(cadastro(dados, categoria), dados.ativo() == null || dados.ativo()));
    LocalDate inicioVinculo =
        dados.dataInicio() == null ? LocalDate.now(ZONA_NEGOCIO) : dados.dataInicio();
    vinculos.save(new VinculoAdolescenteDiscipulado(adolescente, discipulado, inicioVinculo));
    familias.criarObrigatoria(adolescente, ficha);
    return adolescente;
  }

  /**
   * Qualquer perfil no escopo pode informar a ficha (RN051). Sem body, ADMIN/GERENTE recebem 400;
   * discipulador e co-líder recebem “Não consta”.
   */
  private static FichaFamilia.DadosFicha fichaParaCadastro(
      User usuario, FichaFamilia.DadosFicha familia) {
    if (familia != null) return familia;
    var perfis = usuario.getPerfis();
    boolean soLideranca =
        perfis != null
            && !perfis.contains(Role.ADMIN)
            && !perfis.contains(Role.GERENTE)
            && (perfis.contains(Role.DISCIPULADOR) || perfis.contains(Role.CO_LIDER));
    if (soLideranca) return FichaFamilia.dadosNaoConsta();
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "A ficha de família é obrigatória no cadastro do adolescente.");
  }

  public Adolescente atualizar(User usuario, long id, DadosAdolescente dados) {
    Adolescente adolescente = buscar(id);
    VinculoAdolescenteDiscipulado atual = vinculoAtivo(id);
    escopo.exigirAlteracao(usuario, atual.getDiscipulado());
    if (!atual.getDiscipulado().getId().equals(dados.discipuladoId())) {
      throw conflito("Use o endpoint de vínculos para transferir o adolescente.");
    }
    CategoriaAdolescente categoria =
        dados.categoria() == null ? adolescente.getCategoria() : dados.categoria();
    adolescente.atualizar(cadastro(dados, categoria), dados.ativo());
    return adolescente;
  }

  private static DadosCadastroAdolescente cadastro(
      DadosAdolescente dados, CategoriaAdolescente categoria) {
    boolean naoPossuiTelefone = Boolean.TRUE.equals(dados.naoPossuiTelefone());
    return new DadosCadastroAdolescente(
        dados.nome(),
        dados.dataNascimento(),
        naoPossuiTelefone ? null : dados.telefone(),
        dados.instagram(),
        dados.consentimentoEm(),
        categoria,
        dados.estrutura(),
        dados.motivoAfastamento(),
        naoPossuiTelefone);
  }

  public void anonimizar(User usuario, long adolescenteId) {
    Adolescente adolescente = buscar(adolescenteId);
    adolescente.anonimizar();
    familias.anonimizarSeExistir(adolescenteId);
    auditoria.save(
        new AuditLog(
            usuario,
            "ADOLESCENTE",
            "ANONIMIZACAO_DADOS_PESSOAIS",
            "adolescenteId=" + adolescenteId));
  }

  public VinculoAdolescenteDiscipulado transferir(
      User usuario, long adolescenteId, long discipuladoId, LocalDate dataInicio) {
    Adolescente adolescente = buscar(adolescenteId);
    VinculoAdolescenteDiscipulado atual = vinculoAtivo(adolescenteId);
    Discipulado destino = discipuladoAtivo(discipuladoId);
    escopo.exigirAlteracao(usuario, atual.getDiscipulado());
    escopo.exigirAlteracao(usuario, destino);
    if (atual.getDiscipulado().getId().equals(discipuladoId))
      throw conflito("O adolescente já pertence ao discipulado informado.");
    if (dataInicio == null || !dataInicio.isAfter(atual.getDataInicio()))
      throw conflito("A transferência deve ocorrer após o início do vínculo atual.");
    atual.encerrar(dataInicio.minusDays(1));
    return vinculos.save(new VinculoAdolescenteDiscipulado(adolescente, destino, dataInicio));
  }

  @Transactional(readOnly = true)
  public Page<AdolescenteComVinculo> listar(
      User usuario,
      Long discipuladoId,
      Boolean ativo,
      List<CategoriaAdolescente> categorias,
      Pageable pageable) {
    if (discipuladoId != null)
      escopo.exigirLeitura(usuario, discipuladoAtivoOuInativo(discipuladoId));
    Specification<Adolescente> filtro =
        Specification.where(
            ativo == null ? null : (root, query, cb) -> cb.equal(root.get("ativo"), ativo));
    if (categorias != null && !categorias.isEmpty())
      filtro = filtro.and((root, query, cb) -> root.get("categoria").in(categorias));
    if (discipuladoId != null) filtro = filtro.and(noDiscipulado(discipuladoId));
    filtro = filtro.and(noEscopo(usuario));
    return adolescentes.findAll(filtro, pageable).map(this::comVinculoAtual);
  }

  @Transactional(readOnly = true)
  public List<AlertaGoe> listarAlertasGoe(User usuario, long discipuladoId) {
    Discipulado discipulado = discipuladoAtivoOuInativo(discipuladoId);
    escopo.exigirLeitura(usuario, discipulado);
    LocalDate fim = LocalDate.now(clock.withZone(ZONA_NEGOCIO));
    LocalDate inicio = fim.minusDays(JANELA_GOE_DIAS - 1L);
    return frequencias
        .encontrarPotenciaisGoe(discipuladoId, inicio, fim, MINIMO_FALTAS_GOE)
        .stream()
        .map(r -> new AlertaGoe(r.getAdolescenteId(), r.getNome(), r.getFaltas()))
        .toList();
  }

  /**
   * Promove automaticamente VISITANTE → DISCIPULO quando há ao menos três presenças em encontros
   * REALIZADO na janela de 35 dias a partir do dataInicio do primeiro vínculo. Sem confirmação de
   * UI; unidirecional (não rebaixa se a presença for editada depois).
   */
  public void promoverVisitanteSeElegivel(User ator, long adolescenteId) {
    Adolescente adolescente = buscar(adolescenteId);
    if (!adolescente.isAtivo()
        || adolescente.isAnonimizado()
        || adolescente.getCategoria() != CategoriaAdolescente.VISITANTE) {
      return;
    }
    List<VinculoAdolescenteDiscipulado> historico =
        vinculos.findAllByAdolescenteIdOrderByDataInicioAsc(adolescenteId);
    if (historico.isEmpty()) return;
    LocalDate inicio = historico.get(0).getDataInicio();
    LocalDate fim = inicio.plusDays(JANELA_PROMOCAO_VISITANTE_DIAS - 1L);
    long presencas = frequencias.contarPresencasRealizadas(adolescenteId, inicio, fim);
    if (presencas < MINIMO_PRESENCAS_PROMOCAO_VISITANTE) return;
    adolescente.promoverDeVisitanteParaDiscipulo();
    auditoria.save(
        new AuditLog(
            ator,
            "ADOLESCENTE",
            "PROMOVER_VISITANTE",
            "adolescenteId="
                + adolescenteId
                + ",anterior=VISITANTE,novo=DISCIPULO,presencas="
                + presencas
                + ",janelaInicio="
                + inicio
                + ",janelaFim="
                + fim));
  }

  @Transactional(readOnly = true)
  public AdolescenteComVinculo comVinculoAtual(Adolescente adolescente) {
    VinculoAdolescenteDiscipulado vinculo = vinculoAtual(adolescente.getId());
    Discipulado discipulado = vinculo.getDiscipulado();
    return new AdolescenteComVinculo(adolescente, discipulado.getId(), discipulado.getNome());
  }

  @Transactional(readOnly = true)
  public VinculoAdolescenteDiscipulado vinculoAtual(long adolescenteId) {
    return vinculos
        .findFirstByAdolescenteIdAndAtivoTrue(adolescenteId)
        .orElseThrow(() -> conflito("O adolescente não possui vínculo ativo."));
  }

  private Specification<Adolescente> noDiscipulado(long discipuladoId) {
    return (root, query, cb) -> {
      var sub = query.subquery(Long.class);
      var v = sub.from(VinculoAdolescenteDiscipulado.class);
      sub.select(v.get("adolescente").get("id"))
          .where(
              cb.isTrue(v.get("ativo")), cb.equal(v.get("discipulado").get("id"), discipuladoId));
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
      if (usuario.getPerfis().contains(Role.GERENTE))
        acessos.add(cb.equal(d.get("gerencia").get("gerente").get("id"), usuario.getId()));
      if (usuario.getPerfis().contains(Role.DISCIPULADOR))
        acessos.add(cb.equal(d.get("discipulador").get("id"), usuario.getId()));
      if (usuario.getPerfis().contains(Role.CO_LIDER)) {
        var coLider = d.join("coLideres", JoinType.LEFT);
        acessos.add(cb.equal(coLider.get("id"), usuario.getId()));
      }
      if (acessos.isEmpty()) return cb.disjunction();
      sub.select(v.get("adolescente").get("id"))
          .where(
              cb.isTrue(v.get("ativo")),
              cb.or(acessos.toArray(jakarta.persistence.criteria.Predicate[]::new)));
      return root.get("id").in(sub);
    };
  }

  private Adolescente buscar(long id) {
    return adolescentes
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adolescente não encontrado."));
  }

  private VinculoAdolescenteDiscipulado vinculoAtivo(long id) {
    return vinculos
        .findByAdolescenteIdAndAtivoTrue(id)
        .orElseThrow(() -> conflito("O adolescente não possui vínculo ativo."));
  }

  private Discipulado discipuladoAtivo(long id) {
    Discipulado d = discipuladoAtivoOuInativo(id);
    if (!d.isAtivo() || (d.getGerencia() != null && !d.getGerencia().isAtivo()))
      throw conflito("O discipulado informado está inativo.");
    return d;
  }

  private Discipulado discipuladoAtivoOuInativo(long id) {
    return discipulados
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discipulado não encontrado."));
  }

  private static ResponseStatusException conflito(String mensagem) {
    return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
  }

  public record DadosAdolescente(
      String nome,
      LocalDate dataNascimento,
      String telefone,
      String instagram,
      LocalDate consentimentoEm,
      CategoriaAdolescente categoria,
      String estrutura,
      String motivoAfastamento,
      Long discipuladoId,
      Boolean ativo,
      LocalDate dataInicio,
      Boolean naoPossuiTelefone) {}

  public record AdolescenteComVinculo(
      Adolescente adolescente, long discipuladoId, String discipuladoNome) {}

  public record AlertaGoe(long adolescenteId, String nome, long faltas) {}
}
