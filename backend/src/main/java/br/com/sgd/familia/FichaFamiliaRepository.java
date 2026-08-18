package br.com.sgd.familia;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FichaFamiliaRepository extends JpaRepository<FichaFamilia, Long> {
  Optional<FichaFamilia> findByAdolescenteId(long adolescenteId);

  List<FichaFamilia> findByAdolescenteIdIn(Collection<Long> adolescenteIds);

  @Query(
      """
      select f from FichaFamilia f
      where :gerenteId is null
         or exists (
           select 1 from VinculoAdolescenteDiscipulado v
           where v.adolescente = f.adolescente
             and v.ativo = true
             and v.discipulado.gerencia.gerente.id = :gerenteId
         )
      order by f.adolescente.nome asc
      """)
  Page<FichaFamilia> listarNoEscopo(@Param("gerenteId") Long gerenteId, Pageable pageable);
}
