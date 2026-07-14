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
        select substring(cast(e.data as varchar), 1, 7) as referencia, coalesce(sum(v.quantidade), 0) as visitantes
          from encontros e left join visitantes v on v.encontro_id = e.id
         where e.discipulado_id = :discipuladoId and e.situacao = 'REALIZADO' and e.data between :inicio and :fim
         group by substring(cast(e.data as varchar), 1, 7) order by referencia
        """, nativeQuery = true)
    List<VisitantesMensais> visitantesMensais(long discipuladoId, LocalDate inicio, LocalDate fim);

    @Query(value = "select count(*) from encontros where discipulado_id=:discipuladoId and situacao='REALIZADO' and data between :inicio and :fim", nativeQuery = true)
    long encontrosRealizados(long discipuladoId, LocalDate inicio, LocalDate fim);

    interface ContagemMensal { String getReferencia(); Long getPresentes(); Long getAusentes(); }
    interface VisitantesMensais { String getReferencia(); Long getVisitantes(); }
}
