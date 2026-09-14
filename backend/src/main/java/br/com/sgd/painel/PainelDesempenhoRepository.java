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
               g.nome as gerenciaNome
          from discipulados d
          join gerencias g on g.id = d.gerencia_id
         where d.em_formacao = false
         order by d.nome, d.id
        """,
      nativeQuery = true)
  List<DiscipuladoMeta> listarDiscipulados();

  @Query(
      value =
          """
        select d.id as discipuladoId,
               to_char(e.data, 'YYYY-MM') as referencia,
               coalesce(sum(case when f.situacao = 'PRESENTE' then 1 else 0 end), 0) as presentes,
               coalesce(sum(case when f.situacao = 'AUSENTE' then 1 else 0 end), 0) as ausentes
          from encontros e
          join discipulados d on d.id = e.discipulado_id
          left join frequencias f on f.encontro_id = e.id
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

  interface DiscipuladoMeta {
    Long getId();

    String getNome();

    String getSexo();

    String getFaixaEtaria();

    Boolean getAtivo();

    Long getGerenciaId();

    String getGerenciaNome();
  }

  interface FrequenciaMensal {
    Long getDiscipuladoId();

    String getReferencia();

    Long getPresentes();

    Long getAusentes();
  }

  interface VinculoPeriodo {
    Long getDiscipuladoId();

    Long getAdolescenteId();

    LocalDate getDataInicio();

    LocalDate getDataFim();
  }
}
