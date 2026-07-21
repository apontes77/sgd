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
}
