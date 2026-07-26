package br.com.sgd.frequencia;

import java.time.LocalDate;
import java.util.*;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FrequenciaRepository extends JpaRepository<Frequencia, Long> {
  @EntityGraph(attributePaths = "adolescente")
  List<Frequencia> findAllByEncontroIdOrderByAdolescenteNome(Long encontroId);

  @EntityGraph(attributePaths = {"encontro", "adolescente"})
  List<Frequencia> findAllByEncontroIdInOrderByEncontroIdAscAdolescenteNomeAsc(
      Collection<Long> encontroIds);

  boolean existsByEncontroId(Long encontroId);

  @Query(
      """
      select a.id as adolescenteId, a.nome as nome, count(f) as faltas
      from Frequencia f
      join f.encontro e
      join f.adolescente a
      where e.discipulado.id = :discipuladoId
        and e.situacao = br.com.sgd.frequencia.SituacaoEncontro.REALIZADO
        and f.situacao = br.com.sgd.frequencia.SituacaoFrequencia.AUSENTE
        and e.data between :inicio and :fim
        and a.ativo = true
        and a.categoria = br.com.sgd.adolescente.CategoriaAdolescente.DISCIPULO
        and exists (
          select 1 from VinculoAdolescenteDiscipulado v
          where v.adolescente = a and v.ativo = true and v.discipulado.id = :discipuladoId
        )
      group by a.id, a.nome
      having count(f) >= :minimoFaltas
      order by a.nome
      """)
  List<AlertaGoeRow> encontrarPotenciaisGoe(
      @Param("discipuladoId") long discipuladoId,
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim,
      @Param("minimoFaltas") long minimoFaltas);

  interface AlertaGoeRow {
    Long getAdolescenteId();

    String getNome();

    Long getFaltas();
  }
}
