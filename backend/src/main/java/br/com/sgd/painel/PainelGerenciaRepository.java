package br.com.sgd.painel;

import br.com.sgd.frequencia.Encontro;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface PainelGerenciaRepository extends Repository<Encontro, Long> {
    @Query(value = """
        select substring(cast(e.data as varchar), 1, 7) as referencia,
               coalesce(sum(case when f.situacao = 'PRESENTE' then 1 else 0 end), 0) as presentes,
               coalesce(sum(case when f.situacao = 'AUSENTE' then 1 else 0 end), 0) as ausentes
          from encontros e join discipulados d on d.id = e.discipulado_id
          left join frequencias f on f.encontro_id = e.id
         where d.gerencia_id = :gerenciaId and e.situacao = 'REALIZADO' and e.data between :inicio and :fim
         group by substring(cast(e.data as varchar), 1, 7) order by referencia
        """, nativeQuery = true)
    List<ContagemMensal> frequenciasMensais(long gerenciaId, LocalDate inicio, LocalDate fim);

    @Query(value = """
        select substring(cast(e.data as varchar), 1, 7) as referencia, coalesce(sum(v.quantidade), 0) as visitantes
          from encontros e join discipulados d on d.id = e.discipulado_id
          left join visitantes v on v.encontro_id = e.id
         where d.gerencia_id = :gerenciaId and e.situacao = 'REALIZADO' and e.data between :inicio and :fim
         group by substring(cast(e.data as varchar), 1, 7) order by referencia
        """, nativeQuery = true)
    List<VisitantesMensais> visitantesMensais(long gerenciaId, LocalDate inicio, LocalDate fim);

    @Query(value = "select count(*) from encontros e join discipulados d on d.id=e.discipulado_id where d.gerencia_id=:gerenciaId and e.situacao='REALIZADO' and e.data between :inicio and :fim", nativeQuery = true)
    long encontrosRealizados(long gerenciaId, LocalDate inicio, LocalDate fim);

    @Query(value = """
        select d.id as discipuladoId,
               coalesce(sum(case when f.situacao = 'PRESENTE' then 1 else 0 end), 0) as presentes,
               coalesce(sum(case when f.situacao = 'AUSENTE' then 1 else 0 end), 0) as ausentes,
               count(distinct e.id) as encontrosRealizados
          from encontros e join discipulados d on d.id=e.discipulado_id
          left join frequencias f on f.encontro_id=e.id
         where d.gerencia_id=:gerenciaId and e.situacao='REALIZADO' and e.data between :inicio and :fim
         group by d.id
        """, nativeQuery = true)
    List<ContagemDiscipulado> frequenciasPorDiscipulado(long gerenciaId, LocalDate inicio, LocalDate fim);

    @Query(value = """
        select d.id as discipuladoId, coalesce(sum(v.quantidade), 0) as visitantes
          from encontros e join discipulados d on d.id=e.discipulado_id
          left join visitantes v on v.encontro_id=e.id
         where d.gerencia_id=:gerenciaId and e.situacao='REALIZADO' and e.data between :inicio and :fim
         group by d.id
        """, nativeQuery = true)
    List<VisitantesDiscipulado> visitantesPorDiscipulado(long gerenciaId, LocalDate inicio, LocalDate fim);

    @Query(value = """
        select d.id as discipuladoId, substring(cast(e.data as varchar), 1, 7) as referencia,
               coalesce(sum(case when f.situacao = 'PRESENTE' then 1 else 0 end), 0) as presentes,
               coalesce(sum(case when f.situacao = 'AUSENTE' then 1 else 0 end), 0) as ausentes
          from encontros e join discipulados d on d.id=e.discipulado_id
          left join frequencias f on f.encontro_id=e.id
         where d.gerencia_id=:gerenciaId and e.situacao='REALIZADO' and e.data between :inicio and :fim
         group by d.id, substring(cast(e.data as varchar), 1, 7)
         order by d.id, referencia
        """, nativeQuery = true)
    List<ContagemMensalDiscipulado> frequenciasMensaisPorDiscipulado(long gerenciaId, LocalDate inicio, LocalDate fim);

    @Query(value = """
        select d.id as discipuladoId, substring(cast(e.data as varchar), 1, 7) as referencia, coalesce(sum(v.quantidade), 0) as visitantes
          from encontros e join discipulados d on d.id=e.discipulado_id
          left join visitantes v on v.encontro_id=e.id
         where d.gerencia_id=:gerenciaId and e.situacao='REALIZADO' and e.data between :inicio and :fim
         group by d.id, substring(cast(e.data as varchar), 1, 7)
         order by d.id, referencia
        """, nativeQuery = true)
    List<VisitantesMensaisDiscipulado> visitantesMensaisPorDiscipulado(long gerenciaId, LocalDate inicio, LocalDate fim);

    interface ContagemMensal { String getReferencia(); Long getPresentes(); Long getAusentes(); }
    interface VisitantesMensais { String getReferencia(); Long getVisitantes(); }
    interface ContagemDiscipulado { Long getDiscipuladoId(); Long getPresentes(); Long getAusentes(); Long getEncontrosRealizados(); }
    interface VisitantesDiscipulado { Long getDiscipuladoId(); Long getVisitantes(); }
    interface ContagemMensalDiscipulado extends ContagemMensal { Long getDiscipuladoId(); }
    interface VisitantesMensaisDiscipulado extends VisitantesMensais { Long getDiscipuladoId(); }
}
