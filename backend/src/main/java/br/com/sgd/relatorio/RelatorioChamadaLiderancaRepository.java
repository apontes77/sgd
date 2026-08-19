package br.com.sgd.relatorio;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import br.com.sgd.lideranca.ChamadaLideranca;

public interface RelatorioChamadaLiderancaRepository extends Repository<ChamadaLideranca, Long> {
  @Query(
      value =
          """
          select c.id as chamadaId, c.data as data, c.observacao_geral as observacaoGeral,
                 d.id as discipuladoId, d.nome as discipuladoNome, d.sexo as sexo,
                 g.nome as gerenciaNome, i.observacao as observacao
            from chamadas_lideranca c
            left join chamadas_lideranca_discipulados i
              on i.chamada_id = c.id
             and (:discipuladoId is null or i.discipulado_id = :discipuladoId)
            left join discipulados d on d.id = i.discipulado_id
            left join gerencias g on g.id = d.gerencia_id
           where c.data between :inicio and :fim
           order by c.data, g.nome, d.nome, c.id, d.id
          """,
      nativeQuery = true)
  List<CabecalhoChamada> cabecalhos(
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim,
      @Param("discipuladoId") Long discipuladoId);

  @Query(
      value =
          """
          select i.chamada_id as chamadaId, i.discipulado_id as discipuladoId,
                 u.id as usuarioId, u.nome as nome, p.papel as papel, p.situacao as situacao
            from chamadas_lideranca c
            join chamadas_lideranca_discipulados i on i.chamada_id = c.id
            join presencas_lideranca p on p.item_id = i.id
            join usuarios u on u.id = p.usuario_id
           where c.data between :inicio and :fim
             and (:discipuladoId is null or i.discipulado_id = :discipuladoId)
          """,
      nativeQuery = true)
  List<PresencaRow> presencas(
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim,
      @Param("discipuladoId") Long discipuladoId);

  @Query(
      value =
          """
          select c.id as chamadaId,
                 coalesce(sum(case when p.situacao = 'PRESENTE' then 1 else 0 end), 0) as presentes,
                 coalesce(sum(case when p.situacao = 'AUSENTE' then 1 else 0 end), 0) as ausentes
            from chamadas_lideranca c
            join chamadas_lideranca_discipulados i on i.chamada_id = c.id
            left join presencas_lideranca p on p.item_id = i.id
           where c.data between :inicio and :fim
             and (:discipuladoId is null or i.discipulado_id = :discipuladoId)
           group by c.id
          """,
      nativeQuery = true)
  List<ResumoChamadaSql> resumir(
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim,
      @Param("discipuladoId") Long discipuladoId);

  interface CabecalhoChamada {
    Long getChamadaId();

    LocalDate getData();

    String getObservacaoGeral();

    Long getDiscipuladoId();

    String getDiscipuladoNome();

    String getSexo();

    String getGerenciaNome();

    String getObservacao();
  }

  interface PresencaRow {
    Long getChamadaId();

    Long getDiscipuladoId();

    Long getUsuarioId();

    String getNome();

    String getPapel();

    String getSituacao();
  }

  interface ResumoChamadaSql {
    Long getChamadaId();

    Number getPresentes();

    Number getAusentes();
  }
}
