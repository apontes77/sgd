package br.com.sgd.relatorio;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sgd.frequencia.Encontro;

public interface RelatorioFrequenciaRepository extends JpaRepository<Encontro, Long> {
  @EntityGraph(
      attributePaths = {
        "discipulado",
        "discipulado.gerencia",
        "discipulado.gerencia.gerente",
        "discipulado.discipulador",
        "discipulado.coLideres"
      })
  @Query(
      "select distinct e from Encontro e where e.data between :inicio and :fim order by e.data, e.discipulado.gerencia.nome, e.discipulado.nome, e.id")
  List<Encontro> noPeriodo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

  @EntityGraph(
      attributePaths = {
        "discipulado",
        "discipulado.gerencia",
        "discipulado.gerencia.gerente",
        "discipulado.discipulador",
        "discipulado.coLideres"
      })
  @Query(
      "select distinct e from Encontro e where e.data between :inicio and :fim and e.discipulado.id in :discipuladoIds order by e.data, e.discipulado.gerencia.nome, e.discipulado.nome, e.id")
  List<Encontro> noPeriodoDoEscopo(
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim,
      @Param("discipuladoIds") Collection<Long> discipuladoIds);

  @Query(
      value =
          """
        select v.encontro_id as encontroId, coalesce(sum(v.quantidade), 0) as visitantes
          from visitantes v
         where v.encontro_id in :encontroIds
         group by v.encontro_id
        """,
      nativeQuery = true)
  List<VisitantesPorEncontro> contarVisitantesPorEncontro(
      @Param("encontroIds") Collection<Long> encontroIds);

  interface VisitantesPorEncontro {
    Long getEncontroId();

    Number getVisitantes();
  }
}
