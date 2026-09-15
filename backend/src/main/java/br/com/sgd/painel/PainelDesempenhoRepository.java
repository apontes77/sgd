package br.com.sgd.painel;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import br.com.sgd.organizacao.Discipulado;

public interface PainelDesempenhoRepository extends Repository<Discipulado, Long> {
  @Query(
      value =
          """
        select d.id as id,
               d.nome as nome,
               d.sexo as sexo,
               d.faixa_etaria as faixaEtaria,
               d.ativo as ativo,
               g.id as gerenciaId,
               g.nome as gerenciaNome,
               u.nome as discipuladorNome
          from discipulados d
          join gerencias g on g.id = d.gerencia_id
          join usuarios u on u.id = d.discipulador_id
         where d.em_formacao = false
         order by d.nome, d.id
        """,
      nativeQuery = true)
  List<DiscipuladoMeta> listarDiscipulados();

  @Query(
      value =
          """
        select d.id as id,
               d.nome as nome,
               d.sexo as sexo,
               d.faixa_etaria as faixaEtaria,
               d.ativo as ativo,
               g.id as gerenciaId,
               g.nome as gerenciaNome,
               u.nome as discipuladorNome
          from discipulados d
          join gerencias g on g.id = d.gerencia_id
          join usuarios u on u.id = d.discipulador_id
         where d.em_formacao = false
           and d.gerencia_id = :gerenciaId
         order by d.nome, d.id
        """,
      nativeQuery = true)
  List<DiscipuladoMeta> listarDiscipuladosDaGerencia(@Param("gerenciaId") long gerenciaId);

  @Query(
      value =
          """
        select d.id as discipuladoId,
               to_char(e.data, 'YYYY-MM') as referencia,
               coalesce(sum(case when f.situacao = 'PRESENTE' then 1 else 0 end), 0) as presentes,
               coalesce(sum(case when f.situacao = 'PRESENTE' and a.categoria = 'DISCIPULO' then 1 else 0 end), 0) as presentesDiscipulos,
               coalesce(sum(case when f.situacao = 'PRESENTE' and a.categoria = 'VISITANTE' then 1 else 0 end), 0) as presentesVisitantes,
               coalesce(sum(case when f.situacao = 'PRESENTE' and a.categoria = 'DISCIPULO_GOE' then 1 else 0 end), 0) as presentesGoe,
               coalesce(sum(case when f.situacao = 'AUSENTE' then 1 else 0 end), 0) as ausentes
          from encontros e
          join discipulados d on d.id = e.discipulado_id
          left join frequencias f on f.encontro_id = e.id
          left join adolescentes a on a.id = f.adolescente_id
         where d.em_formacao = false
           and e.situacao = 'REALIZADO'
           and e.data between :inicio and :fim
         group by d.id, to_char(e.data, 'YYYY-MM')
         order by d.id, referencia
        """,
      nativeQuery = true)
  List<FrequenciaMensal> frequenciasMensais(
      @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

  @Query(
      value =
          """
        select d.id as discipuladoId,
               to_char(e.data, 'YYYY-MM') as referencia,
               coalesce(sum(case when f.situacao = 'PRESENTE' then 1 else 0 end), 0) as presentes,
               coalesce(sum(case when f.situacao = 'PRESENTE' and a.categoria = 'DISCIPULO' then 1 else 0 end), 0) as presentesDiscipulos,
               coalesce(sum(case when f.situacao = 'PRESENTE' and a.categoria = 'VISITANTE' then 1 else 0 end), 0) as presentesVisitantes,
               coalesce(sum(case when f.situacao = 'PRESENTE' and a.categoria = 'DISCIPULO_GOE' then 1 else 0 end), 0) as presentesGoe,
               coalesce(sum(case when f.situacao = 'AUSENTE' then 1 else 0 end), 0) as ausentes
          from encontros e
          join discipulados d on d.id = e.discipulado_id
          left join frequencias f on f.encontro_id = e.id
          left join adolescentes a on a.id = f.adolescente_id
         where d.em_formacao = false
           and d.gerencia_id = :gerenciaId
           and e.situacao = 'REALIZADO'
           and e.data between :inicio and :fim
         group by d.id, to_char(e.data, 'YYYY-MM')
         order by d.id, referencia
        """,
      nativeQuery = true)
  List<FrequenciaMensal> frequenciasMensaisDaGerencia(
      @Param("gerenciaId") long gerenciaId,
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim);

  @Query(
      value =
          """
        select vin.discipulado_id as discipuladoId,
               vin.adolescente_id as adolescenteId,
               vin.data_inicio as dataInicio,
               vin.data_fim as dataFim
          from vinculos_adolescente_discipulado vin
          join discipulados d on d.id = vin.discipulado_id
         where d.em_formacao = false
           and vin.data_inicio <= :fim
           and (vin.data_fim is null or vin.data_fim >= :inicio)
        """,
      nativeQuery = true)
  List<VinculoPeriodo> vinculosNoPeriodo(
      @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

  @Query(
      value =
          """
        select vin.discipulado_id as discipuladoId,
               vin.adolescente_id as adolescenteId,
               vin.data_inicio as dataInicio,
               vin.data_fim as dataFim
          from vinculos_adolescente_discipulado vin
          join discipulados d on d.id = vin.discipulado_id
         where d.em_formacao = false
           and d.gerencia_id = :gerenciaId
           and vin.data_inicio <= :fim
           and (vin.data_fim is null or vin.data_fim >= :inicio)
        """,
      nativeQuery = true)
  List<VinculoPeriodo> vinculosNoPeriodoDaGerencia(
      @Param("gerenciaId") long gerenciaId,
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim);

  interface DiscipuladoMeta {
    Long getId();

    String getNome();

    String getSexo();

    String getFaixaEtaria();

    Boolean getAtivo();

    Long getGerenciaId();

    String getGerenciaNome();

    String getDiscipuladorNome();
  }

  interface FrequenciaMensal {
    Long getDiscipuladoId();

    String getReferencia();

    Long getPresentes();

    Long getPresentesDiscipulos();

    Long getPresentesVisitantes();

    Long getPresentesGoe();

    Long getAusentes();
  }

  interface VinculoPeriodo {
    Long getDiscipuladoId();

    Long getAdolescenteId();

    LocalDate getDataInicio();

    LocalDate getDataFim();
  }
}
