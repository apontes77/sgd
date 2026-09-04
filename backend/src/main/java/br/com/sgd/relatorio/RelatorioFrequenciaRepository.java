package br.com.sgd.relatorio;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import br.com.sgd.frequencia.Encontro;

public interface RelatorioFrequenciaRepository extends Repository<Encontro, Long> {
  @Query(
      value =
          """
          select e.id as encontroId, e.data as data, e.situacao as situacao,
                 e.justificativa as justificativa, e.observacao as observacao,
                 e.fechamento_automatico as fechamentoAutomatico,
                 g.id as gerenciaId, g.nome as gerenciaNome,
                 d.id as discipuladoId, d.nome as discipuladoNome, d.sexo as sexo,
                 lider.id as discipuladorId, lider.nome as discipuladorNome,
                 coalesce(gerente.nome, g.nome) as gerenteNome
            from encontros e
            join discipulados d on d.id = e.discipulado_id
            left join gerencias g on g.id = d.gerencia_id
            join usuarios lider on lider.id = d.discipulador_id
            left join usuarios gerente on gerente.id = g.gerente_id
           where e.data between :inicio and :fim
           order by e.data, coalesce(g.nome, 'Formação'), d.nome, e.id
          """,
      nativeQuery = true)
  List<EncontroCabecalho> cabecalhosNoPeriodo(
      @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

  @Query(
      value =
          """
          select e.id as encontroId, e.data as data, e.situacao as situacao,
                 e.justificativa as justificativa, e.observacao as observacao,
                 e.fechamento_automatico as fechamentoAutomatico,
                 g.id as gerenciaId, g.nome as gerenciaNome,
                 d.id as discipuladoId, d.nome as discipuladoNome, d.sexo as sexo,
                 lider.id as discipuladorId, lider.nome as discipuladorNome,
                 coalesce(gerente.nome, g.nome) as gerenteNome
            from encontros e
            join discipulados d on d.id = e.discipulado_id
            left join gerencias g on g.id = d.gerencia_id
            join usuarios lider on lider.id = d.discipulador_id
            left join usuarios gerente on gerente.id = g.gerente_id
           where e.data between :inicio and :fim
             and e.discipulado_id in (:discipuladoIds)
           order by e.data, coalesce(g.nome, 'Formação'), d.nome, e.id
          """,
      nativeQuery = true)
  List<EncontroCabecalho> cabecalhosNoPeriodoDoEscopo(
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim,
      @Param("discipuladoIds") Collection<Long> discipuladoIds);

  @Query(
      value =
          """
          select c.discipulado_id as discipuladoId, u.id as usuarioId, u.nome as nome
            from discipulado_co_lideres c
            join usuarios u on u.id = c.usuario_id
           where c.discipulado_id in (:discipuladoIds)
           order by u.nome, u.id
          """,
      nativeQuery = true)
  List<CoLiderRow> coLideresPorDiscipulado(
      @Param("discipuladoIds") Collection<Long> discipuladoIds);

  @Query(
      value =
          """
          select e.id as encontroId,
                 coalesce(sum(case when e.situacao = 'REALIZADO' and f.situacao = 'PRESENTE'
                   and a.categoria = 'DISCIPULO' then 1 else 0 end), 0) as presentes,
                 coalesce(sum(case when e.situacao = 'REALIZADO' and f.situacao = 'AUSENTE'
                   and a.categoria = 'DISCIPULO' then 1 else 0 end), 0) as ausentes,
                 coalesce(sum(case when e.situacao = 'REALIZADO' and f.situacao = 'PRESENTE'
                   and a.categoria = 'VISITANTE' then 1 else 0 end), 0) as visitantesNominais,
                 coalesce(sum(case when e.situacao = 'REALIZADO' and f.situacao = 'PRESENTE'
                   and a.categoria = 'DISCIPULO_GOE' then 1 else 0 end), 0) as goe
            from encontros e
            left join frequencias f on f.encontro_id = e.id
            left join adolescentes a on a.id = f.adolescente_id
           where e.id in (:encontroIds)
           group by e.id
          """,
      nativeQuery = true)
  List<ResumoEncontro> resumirPorEncontro(@Param("encontroIds") Collection<Long> encontroIds);

  @Query(
      value =
          """
          select f.encontro_id as encontroId, a.id as adolescenteId, a.nome as nome,
                 a.telefone as telefone, f.situacao as situacao
            from frequencias f
            join adolescentes a on a.id = f.adolescente_id
           where f.encontro_id in (:encontroIds)
           order by a.nome, a.id, f.encontro_id
          """,
      nativeQuery = true)
  List<ParticipanteRow> participantesPorEncontro(
      @Param("encontroIds") Collection<Long> encontroIds);

  @Query(
      value =
          """
          select v.encontro_id as encontroId, coalesce(sum(v.quantidade), 0) as visitantes
            from visitantes v
           where v.encontro_id in (:encontroIds)
           group by v.encontro_id
          """,
      nativeQuery = true)
  List<VisitantesPorEncontro> contarVisitantesPorEncontro(
      @Param("encontroIds") Collection<Long> encontroIds);

  @Query(
      value =
          """
          select c.data as data, i.discipulado_id as discipuladoId, i.observacao as observacao
            from chamadas_lideranca c
            join chamadas_lideranca_discipulados i on i.chamada_id = c.id
           where c.data between :inicio and :fim
             and i.discipulado_id in (:discipuladoIds)
          """,
      nativeQuery = true)
  List<ObservacaoLiderancaRow> observacoesChamadaLideranca(
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim,
      @Param("discipuladoIds") Collection<Long> discipuladoIds);

  @Query(
      value =
          """
          select d.id
            from discipulados d
            join gerencias g on g.id = d.gerencia_id
           where g.gerente_id = :usuarioId and g.ativo = true
          """,
      nativeQuery = true)
  List<Long> idsPorGerente(@Param("usuarioId") long usuarioId);

  @Query(
      value =
          """
          select distinct d.id
            from discipulados d
            left join discipulado_co_lideres c on c.discipulado_id = d.id
           where d.discipulador_id = :usuarioId or c.usuario_id = :usuarioId
          """,
      nativeQuery = true)
  List<Long> idsPorLideranca(@Param("usuarioId") long usuarioId);

  interface EncontroCabecalho {
    Long getEncontroId();

    LocalDate getData();

    String getSituacao();

    String getJustificativa();

    String getObservacao();

    Boolean getFechamentoAutomatico();

    Long getGerenciaId();

    String getGerenciaNome();

    Long getDiscipuladoId();

    String getDiscipuladoNome();

    String getSexo();

    Long getDiscipuladorId();

    String getDiscipuladorNome();

    String getGerenteNome();
  }

  interface CoLiderRow {
    Long getDiscipuladoId();

    Long getUsuarioId();

    String getNome();
  }

  interface ResumoEncontro {
    Long getEncontroId();

    Number getPresentes();

    Number getAusentes();

    Number getVisitantesNominais();

    Number getGoe();
  }

  interface ParticipanteRow {
    Long getEncontroId();

    Long getAdolescenteId();

    String getNome();

    String getTelefone();

    String getSituacao();
  }

  interface VisitantesPorEncontro {
    Long getEncontroId();

    Number getVisitantes();
  }

  interface ObservacaoLiderancaRow {
    LocalDate getData();

    Long getDiscipuladoId();

    String getObservacao();
  }
}
