package br.com.sgd.adolescente;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface VinculoAdolescenteRepository
    extends JpaRepository<VinculoAdolescenteDiscipulado, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<VinculoAdolescenteDiscipulado> findByAdolescenteIdAndAtivoTrue(Long adolescenteId);

  @EntityGraph(attributePaths = "discipulado")
  Optional<VinculoAdolescenteDiscipulado> findFirstByAdolescenteIdAndAtivoTrue(Long adolescenteId);

  List<VinculoAdolescenteDiscipulado> findAllByAdolescenteIdOrderByDataInicioAsc(
      Long adolescenteId);
}
