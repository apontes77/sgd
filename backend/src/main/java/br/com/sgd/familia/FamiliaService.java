package br.com.sgd.familia;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.sgd.adolescente.Adolescente;
import br.com.sgd.adolescente.AdolescenteRepository;
import br.com.sgd.adolescente.EscopoOrganizacionalService;
import br.com.sgd.adolescente.VinculoAdolescenteDiscipulado;
import br.com.sgd.adolescente.VinculoAdolescenteRepository;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;

@Service
@Transactional
public class FamiliaService {
  private final FichaFamiliaRepository fichas;
  private final AdolescenteRepository adolescentes;
  private final VinculoAdolescenteRepository vinculos;
  private final EscopoOrganizacionalService escopo;

  public FamiliaService(
      FichaFamiliaRepository fichas,
      AdolescenteRepository adolescentes,
      VinculoAdolescenteRepository vinculos,
      EscopoOrganizacionalService escopo) {
    this.fichas = fichas;
    this.adolescentes = adolescentes;
    this.vinculos = vinculos;
    this.escopo = escopo;
  }

  public FichaFamilia criarObrigatoria(Adolescente adolescente, FichaFamilia.DadosFicha dados) {
    if (dados == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A ficha de família é obrigatória no cadastro do adolescente.");
    }
    if (fichas.findByAdolescenteId(adolescente.getId()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O adolescente já possui ficha de família.");
    }
    return fichas.save(new FichaFamilia(adolescente, dados));
  }

  public FichaFamilia consultar(User usuario, long adolescenteId) {
    buscarAdolescente(adolescenteId);
    VinculoAdolescenteDiscipulado vinculo = vinculoAtivoLeitura(adolescenteId);
    escopo.exigirLeitura(usuario, vinculo.getDiscipulado());
    return fichas
        .findByAdolescenteId(adolescenteId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Ficha de família não encontrada."));
  }

  @Transactional(readOnly = true)
  public FamiliaResponse consultarResponse(User usuario, long adolescenteId) {
    return toResponse(consultar(usuario, adolescenteId));
  }

  public FichaFamilia salvar(User usuario, long adolescenteId, FichaFamilia.DadosFicha dados) {
    Adolescente adolescente = buscarAdolescente(adolescenteId);
    VinculoAdolescenteDiscipulado vinculo = vinculoAtivoEscrita(adolescenteId);
    escopo.exigirAlteracao(usuario, vinculo.getDiscipulado());
    Optional<FichaFamilia> existente = fichas.findByAdolescenteId(adolescenteId);
    if (existente.isPresent()) {
      FichaFamilia ficha = existente.get();
      ficha.atualizar(dados);
      return ficha;
    }
    return fichas.save(new FichaFamilia(adolescente, dados));
  }

  public FamiliaResponse salvarResponse(
      User usuario, long adolescenteId, FichaFamilia.DadosFicha dados) {
    return toResponse(salvar(usuario, adolescenteId, dados));
  }

  public void anonimizarSeExistir(long adolescenteId) {
    fichas.findByAdolescenteId(adolescenteId).ifPresent(FichaFamilia::anonimizar);
  }

  @Transactional(readOnly = true)
  public Page<FamiliaResumo> listar(
      User usuario,
      Pageable pageable,
      String busca,
      SituacaoIgrejaFamilia situacaoIgreja,
      SituacaoPaisFamilia situacaoPais) {
    boolean admin = usuario.getPerfis().contains(Role.ADMIN);
    Long gerenteId = usuario.getPerfis().contains(Role.GERENTE) ? usuario.getId() : null;
    Long discipuladorId = usuario.getPerfis().contains(Role.DISCIPULADOR) ? usuario.getId() : null;
    Long coLiderId = usuario.getPerfis().contains(Role.CO_LIDER) ? usuario.getId() : null;
    String termo = busca == null || busca.isBlank() ? null : busca.trim();
    return fichas
        .listarNoEscopo(
            admin,
            gerenteId,
            discipuladorId,
            coLiderId,
            termo,
            situacaoIgreja,
            situacaoPais,
            pageable)
        .map(this::resumo);
  }

  private FamiliaResumo resumo(FichaFamilia ficha) {
    VinculoAdolescenteDiscipulado vinculo = vinculoAtivoLeitura(ficha.getAdolescente().getId());
    return new FamiliaResumo(
        ficha.getId(),
        ficha.getAdolescente().getId(),
        ficha.getAdolescente().getNome(),
        vinculo.getDiscipulado().getId(),
        vinculo.getDiscipulado().getNome(),
        ficha.situacaoFicha(),
        ficha.getSituacaoIgreja(),
        ficha.getSituacaoPais());
  }

  private Adolescente buscarAdolescente(long id) {
    return adolescentes
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adolescente não encontrado."));
  }

  private VinculoAdolescenteDiscipulado vinculoAtivoLeitura(long adolescenteId) {
    return vinculos
        .findFirstByAdolescenteIdAndAtivoTrue(adolescenteId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.CONFLICT, "O adolescente não possui vínculo ativo."));
  }

  private VinculoAdolescenteDiscipulado vinculoAtivoEscrita(long adolescenteId) {
    return vinculos
        .findByAdolescenteIdAndAtivoTrue(adolescenteId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.CONFLICT, "O adolescente não possui vínculo ativo."));
  }

  public static FichaFamilia.DadosFicha dadosFromRequest(FamiliaRequest r) {
    if (r == null) return null;
    return new FichaFamilia.DadosFicha(
        r.cep(),
        r.rua(),
        r.numero(),
        r.complemento(),
        r.bairro(),
        r.cidade(),
        r.situacaoIgreja(),
        r.atuaOnde(),
        r.situacaoPais(),
        r.descricao(),
        r.desafioFinanceiro(),
        r.desafioEmocional(),
        r.desafioEspiritual(),
        r.desafiosDescricao(),
        r.atividadesJuntas(),
        r.rotinaSemana(),
        r.irmaoDokmos(),
        r.pedidoOracao(),
        r.intervencao(),
        r.observacaoDiscipulador(),
        r.observacaoGerente(),
        responsavelFrom(r.responsavel1()),
        responsavelFrom(r.responsavel2()));
  }

  private static ResponsavelFamilia.DadosResponsavel responsavelFrom(ResponsavelRequest r) {
    if (r == null) return null;
    return new ResponsavelFamilia.DadosResponsavel(
        r.nome(),
        r.parentesco(),
        r.dataNascimento(),
        r.estadoCivil(),
        r.profissao(),
        r.telefone(),
        r.email(),
        r.interessePessoal());
  }

  public static FamiliaResponse toResponse(FichaFamilia ficha) {
    return new FamiliaResponse(
        ficha.getId(),
        ficha.getAdolescente().getId(),
        ficha.getCep(),
        ficha.getRua(),
        ficha.getNumero(),
        ficha.getComplemento(),
        ficha.getBairro(),
        ficha.getCidade(),
        ficha.getSituacaoIgreja(),
        ficha.getAtuaOnde(),
        ficha.getSituacaoPais(),
        ficha.getDescricao(),
        ficha.isDesafioFinanceiro(),
        ficha.isDesafioEmocional(),
        ficha.isDesafioEspiritual(),
        ficha.getDesafiosDescricao(),
        ficha.getAtividadesJuntas(),
        ficha.getRotinaSemana(),
        ficha.getIrmaoDokmos(),
        ficha.getPedidoOracao(),
        ficha.getIntervencao(),
        ficha.getObservacaoDiscipulador(),
        ficha.getObservacaoGerente(),
        ficha.situacaoFicha(),
        responsavelResponse(ficha.getResponsavel1()),
        responsavelResponse(ficha.getResponsavel2()));
  }

  private static ResponsavelResponse responsavelResponse(ResponsavelFamilia r) {
    if (r == null) return null;
    return new ResponsavelResponse(
        r.getOrdem(),
        r.getNome(),
        r.getParentesco(),
        r.getDataNascimento(),
        r.getEstadoCivil(),
        r.getProfissao(),
        r.getTelefone(),
        r.getEmail(),
        r.getInteressePessoal());
  }

  public record FamiliaResumo(
      long id,
      long adolescenteId,
      String adolescenteNome,
      long discipuladoId,
      String discipuladoNome,
      SituacaoFichaFamilia situacaoFicha,
      SituacaoIgrejaFamilia situacaoIgreja,
      SituacaoPaisFamilia situacaoPais) {}

  public record FamiliaRequest(
      String cep,
      String rua,
      String numero,
      String complemento,
      String bairro,
      String cidade,
      SituacaoIgrejaFamilia situacaoIgreja,
      String atuaOnde,
      SituacaoPaisFamilia situacaoPais,
      String descricao,
      Boolean desafioFinanceiro,
      Boolean desafioEmocional,
      Boolean desafioEspiritual,
      String desafiosDescricao,
      String atividadesJuntas,
      String rotinaSemana,
      String irmaoDokmos,
      String pedidoOracao,
      String intervencao,
      String observacaoDiscipulador,
      String observacaoGerente,
      ResponsavelRequest responsavel1,
      ResponsavelRequest responsavel2) {}

  public record ResponsavelRequest(
      String nome,
      String parentesco,
      LocalDate dataNascimento,
      String estadoCivil,
      String profissao,
      String telefone,
      String email,
      String interessePessoal) {}

  public record FamiliaResponse(
      long id,
      long adolescenteId,
      String cep,
      String rua,
      String numero,
      String complemento,
      String bairro,
      String cidade,
      SituacaoIgrejaFamilia situacaoIgreja,
      String atuaOnde,
      SituacaoPaisFamilia situacaoPais,
      String descricao,
      boolean desafioFinanceiro,
      boolean desafioEmocional,
      boolean desafioEspiritual,
      String desafiosDescricao,
      String atividadesJuntas,
      String rotinaSemana,
      String irmaoDokmos,
      String pedidoOracao,
      String intervencao,
      String observacaoDiscipulador,
      String observacaoGerente,
      SituacaoFichaFamilia situacaoFicha,
      ResponsavelResponse responsavel1,
      ResponsavelResponse responsavel2) {}

  public record ResponsavelResponse(
      int ordem,
      String nome,
      String parentesco,
      LocalDate dataNascimento,
      String estadoCivil,
      String profissao,
      String telefone,
      String email,
      String interessePessoal) {}
}
