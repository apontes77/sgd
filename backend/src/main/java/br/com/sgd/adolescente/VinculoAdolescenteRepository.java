package br.com.sgd.adolescente;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VinculoAdolescenteRepository
    extends JpaRepository<VinculoAdolescenteDiscipulado, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<VinculoAdolescenteDiscipulado> findByAdolescenteIdAndAtivoTrue(Long adolescenteId);

  @EntityGraph(attributePaths = "discipulado")
  Optional<VinculoAdolescenteDiscipulado> findFirstByAdolescenteIdAndAtivoTrue(Long adolescenteId);

  List<VinculoAdolescenteDiscipulado> findAllByAdolescenteIdOrderByDataInicioAsc(
      Long adolescenteId);

  @EntityGraph(
      attributePaths = {
        "adolescente",
        "discipulado",
        "discipulado.discipulador",
        "discipulado.gerencia"
      })
  @Query(
      """
      select v from VinculoAdolescenteDiscipulado v
      where v.ativo = true
        and (:discipuladoId is null or v.discipulado.id = :discipuladoId)
        and (:ativo is null or v.adolescente.ativo = :ativo)
      order by v.discipulado.gerencia.nome asc, v.discipulado.nome asc, v.adolescente.nome asc
      """)
  List<VinculoAdolescenteDiscipulado> findAtivosParaExport(
      @Param("discipuladoId") Long discipuladoId, @Param("ativo") Boolean ativo);
}
