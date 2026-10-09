package br.com.sgd.familia;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FichaFamiliaRepositoryCustom {
  Page<FichaFamilia> listarNoEscopo(
      boolean admin,
      Long gerenteId,
      Long discipuladorId,
      Long coLiderId,
      FiltroListagemFamilia filtro,
      Pageable pageable);
}
