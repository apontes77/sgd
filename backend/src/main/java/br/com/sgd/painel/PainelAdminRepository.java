package br.com.sgd.painel;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import br.com.sgd.frequencia.Encontro;

public interface PainelAdminRepository extends Repository<Encontro, Long> {
  @Query(
      value =
          """
        select substring(cast(e.data as varchar), 1, 7) as referencia,
               coalesce(sum(case when f.situacao = 'PRESENTE' then 1 else 0 end), 0) as presentes,
               coalesce(sum(case when f.situacao = 'AUSENTE' then 1 else 0 end), 0) as ausentes
          from encontros e
          left join frequencias f on f.encontro_id = e.id
         where e.situacao = 'REALIZADO' and e.data between :inicio and :fim
         group by substring(cast(e.data as varchar), 1, 7)
         order by referencia
        """,
      nativeQuery = true)
  List<ContagemMensal> frequenciasMensais(
      @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

  @Query(
      value =
          """
        select substring(cast(e.data as varchar), 1, 7) as referencia,
               count(distinct f.adolescente_id) as visitantes
          from encontros e
          join frequencias f on f.encontro_id = e.id and f.situacao = 'PRESENTE'
          join vinculos_adolescente_discipulado vin on vin.adolescente_id = f.adolescente_id
           and vin.discipulado_id = e.discipulado_id and vin.data_inicio = e.data
         where e.situacao = 'REALIZADO' and e.data between :inicio and :fim
         group by substring(cast(e.data as varchar), 1, 7)
         order by referencia
        """,
      nativeQuery = true)
  List<VisitantesMensais> visitantesMensais(
      @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

  @Query(
      value =
          "select count(*) from encontros e where e.situacao = 'REALIZADO' and e.data between :inicio and :fim",
      nativeQuery = true)
  long encontrosRealizados(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

  @Query(
      value =
          "select count(*) from encontros e where e.situacao = 'NAO_REALIZADO' and e.data between :inicio and :fim",
      nativeQuery = true)
  long encontrosNaoRealizados(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

  @Query(
      value =
          """
        select g.id as id, g.nome as nome,
               coalesce(sum(case when f.situacao = 'PRESENTE' then 1 else 0 end), 0) as presentes,
               coalesce(sum(case when f.situacao = 'AUSENTE' then 1 else 0 end), 0) as ausentes
          from encontros e
          join discipulados d on d.id = e.discipulado_id
          join gerencias g on g.id = d.gerencia_id
          left join frequencias f on f.encontro_id = e.id
         where e.situacao = 'REALIZADO' and e.data between :inicio and :fim
         group by g.id, g.nome
         order by g.nome
        """,
      nativeQuery = true)
  List<ContagemGerencia> porGerencia(
      @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

  @Query(
      value =
          """
        select g.id as gerenciaId, g.nome as gerenciaNome,
               substring(cast(e.data as varchar), 1, 7) as referencia,
               coalesce(sum(case when f.situacao = 'PRESENTE' then 1 else 0 end), 0) as presentes,
               coalesce(sum(case when f.situacao = 'AUSENTE' then 1 else 0 end), 0) as ausentes
          from encontros e
          join discipulados d on d.id = e.discipulado_id
          join gerencias g on g.id = d.gerencia_id
          left join frequencias f on f.encontro_id = e.id
         where e.situacao = 'REALIZADO' and e.data between :inicio and :fim
         group by g.id, g.nome, substring(cast(e.data as varchar), 1, 7)
         order by g.nome, referencia
        """,
      nativeQuery = true)
  List<ContagemGerenciaMensal> porGerenciaMensal(
      @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

  @Query(
      value =
          """
        select d.sexo as sexo,
               coalesce(sum(case when f.situacao = 'PRESENTE' then 1 else 0 end), 0) as presentes,
               coalesce(sum(case when f.situacao = 'AUSENTE' then 1 else 0 end), 0) as ausentes
          from encontros e
          join discipulados d on d.id = e.discipulado_id
          left join frequencias f on f.encontro_id = e.id
         where e.situacao = 'REALIZADO' and e.data between :inicio and :fim
         group by d.sexo
         order by d.sexo
        """,
      nativeQuery = true)
  List<ContagemSexo> porSexo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

  interface ContagemMensal {
    String getReferencia();

    Long getPresentes();

    Long getAusentes();
  }

  interface VisitantesMensais {
    String getReferencia();

    Long getVisitantes();
  }

  interface ContagemGerencia {
    Long getId();

    String getNome();

    Long getPresentes();

    Long getAusentes();
  }

  interface ContagemGerenciaMensal {
    Long getGerenciaId();

    String getGerenciaNome();

    String getReferencia();

    Long getPresentes();

    Long getAusentes();
  }

  interface ContagemSexo {
    String getSexo();

    Long getPresentes();

    Long getAusentes();
  }
}
