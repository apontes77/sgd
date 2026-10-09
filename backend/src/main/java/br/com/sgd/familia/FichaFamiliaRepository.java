package br.com.sgd.familia;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FichaFamiliaRepository
    extends JpaRepository<FichaFamilia, Long>, FichaFamiliaRepositoryCustom {
  Optional<FichaFamilia> findByAdolescenteId(long adolescenteId);

  List<FichaFamilia> findByAdolescenteIdIn(Collection<Long> adolescenteIds);
}
