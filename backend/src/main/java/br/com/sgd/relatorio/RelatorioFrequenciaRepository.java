package br.com.sgd.relatorio;

import br.com.sgd.frequencia.Encontro;
import br.com.sgd.frequencia.SituacaoEncontro;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RelatorioFrequenciaRepository extends JpaRepository<Encontro, Long> {
    @EntityGraph(attributePaths = {"discipulado", "discipulado.gerencia", "discipulado.discipulador", "discipulado.coLideres"})
    @Query("select distinct e from Encontro e where e.data between :inicio and :fim and e.situacao = :situacao order by e.data, e.discipulado.gerencia.nome, e.discipulado.nome, e.id")
    List<Encontro> realizadosNoPeriodo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim, @Param("situacao") SituacaoEncontro situacao);

    @EntityGraph(attributePaths = {"discipulado", "discipulado.gerencia", "discipulado.discipulador", "discipulado.coLideres"})
    @Query("select distinct e from Encontro e where e.data between :inicio and :fim and e.situacao = :situacao and e.discipulado.id in :discipuladoIds order by e.data, e.discipulado.gerencia.nome, e.discipulado.nome, e.id")
    List<Encontro> realizadosNoPeriodoDoEscopo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim, @Param("situacao") SituacaoEncontro situacao,
            @Param("discipuladoIds") Collection<Long> discipuladoIds);

    @Query(value = """
        select e.id as encontroId, count(distinct f.adolescente_id) as visitantes
          from encontros e
          join frequencias f on f.encontro_id = e.id and f.situacao = 'PRESENTE'
          join vinculos_adolescente_discipulado vin on vin.adolescente_id = f.adolescente_id
           and vin.discipulado_id = e.discipulado_id and vin.data_inicio = e.data
         where e.id in :encontroIds
         group by e.id
        """, nativeQuery = true)
    List<VisitantesPorEncontro> contarVisitantesPorEncontro(@Param("encontroIds") Collection<Long> encontroIds);

    interface VisitantesPorEncontro { Long getEncontroId(); Integer getVisitantes(); }
}
