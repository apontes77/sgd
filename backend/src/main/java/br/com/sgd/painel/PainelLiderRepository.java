package br.com.sgd.painel;

import br.com.sgd.frequencia.Encontro;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface PainelLiderRepository extends Repository<Encontro, Long> {
    @Query(value = """
        select substring(cast(e.data as varchar), 1, 7) as referencia,
               coalesce(sum(case when f.situacao = 'PRESENTE' then 1 else 0 end), 0) as presentes,
               coalesce(sum(case when f.situacao = 'AUSENTE' then 1 else 0 end), 0) as ausentes
          from encontros e left join frequencias f on f.encontro_id = e.id
         where e.discipulado_id = :discipuladoId and e.situacao = 'REALIZADO' and e.data between :inicio and :fim
         group by substring(cast(e.data as varchar), 1, 7) order by referencia
        """, nativeQuery = true)
    List<ContagemMensal> frequenciasMensais(long discipuladoId, LocalDate inicio, LocalDate fim);

    @Query(value = """
        select substring(cast(e.data as varchar), 1, 7) as referencia, count(distinct f.adolescente_id) as visitantes
          from encontros e
          join frequencias f on f.encontro_id = e.id and f.situacao = 'PRESENTE'
          join vinculos_adolescente_discipulado vin on vin.adolescente_id = f.adolescente_id
           and vin.discipulado_id = e.discipulado_id and vin.data_inicio = e.data
         where e.discipulado_id = :discipuladoId and e.situacao = 'REALIZADO' and e.data between :inicio and :fim
         group by substring(cast(e.data as varchar), 1, 7) order by referencia
        """, nativeQuery = true)
    List<VisitantesMensais> visitantesMensais(long discipuladoId, LocalDate inicio, LocalDate fim);

    @Query(value = "select count(*) from encontros where discipulado_id=:discipuladoId and situacao='REALIZADO' and data between :inicio and :fim", nativeQuery = true)
    long encontrosRealizados(long discipuladoId, LocalDate inicio, LocalDate fim);

    @Query(value = "select distinct a.id as adolescenteId, a.nome as nome from adolescentes a "
        + "join vinculos_adolescente_discipulado v on v.adolescente_id = a.id "
        + "where v.discipulado_id = :discipuladoId and v.data_inicio <= :fim "
        + "and (v.data_fim is null or v.data_fim >= :inicio) union "
        + "select distinct a.id as adolescenteId, a.nome as nome from adolescentes a "
        + "join frequencias f on f.adolescente_id = a.id join encontros e on e.id = f.encontro_id "
        + "where e.discipulado_id = :discipuladoId and e.situacao = 'REALIZADO' "
        + "and e.data between :inicio and :fim order by nome, adolescenteId", nativeQuery = true)
    List<DiscipuloPeriodo> discipulosNoPeriodo(long discipuladoId, LocalDate inicio, LocalDate fim);

    @Query(value = "select a.id as adolescenteId, a.nome as nome, "
        + "substring(cast(e.data as varchar), 1, 7) as referencia, "
        + "coalesce(sum(case when f.situacao = 'PRESENTE' then 1 else 0 end), 0) as presentes, "
        + "coalesce(sum(case when f.situacao = 'AUSENTE' then 1 else 0 end), 0) as ausentes "
        + "from encontros e join frequencias f on f.encontro_id = e.id "
        + "join adolescentes a on a.id = f.adolescente_id "
        + "where e.discipulado_id = :discipuladoId and e.situacao = 'REALIZADO' "
        + "and e.data between :inicio and :fim group by a.id, a.nome, "
        + "substring(cast(e.data as varchar), 1, 7) order by a.nome, a.id, referencia", nativeQuery = true)
    List<FrequenciaIndividualMensal> frequenciasIndividuaisMensais(long discipuladoId, LocalDate inicio, LocalDate fim);

    interface ContagemMensal { String getReferencia(); Long getPresentes(); Long getAusentes(); }
    interface VisitantesMensais { String getReferencia(); Long getVisitantes(); }
    interface DiscipuloPeriodo { Long getAdolescenteId(); String getNome(); }
    interface FrequenciaIndividualMensal {
        Long getAdolescenteId(); String getNome(); String getReferencia(); Long getPresentes(); Long getAusentes();
    }
}
