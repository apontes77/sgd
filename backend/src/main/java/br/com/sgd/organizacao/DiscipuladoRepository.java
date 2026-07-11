package br.com.sgd.organizacao;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscipuladoRepository extends JpaRepository<Discipulado, Long> {
    List<Discipulado> findAllByGerenciaIdAndAtivoTrue(Long gerenciaId);
    Page<Discipulado> findAllByGerenciaId(Long gerenciaId, Pageable pageable);
    Page<Discipulado> findAllByAtivo(boolean ativo, Pageable pageable);
    Page<Discipulado> findAllByGerenciaIdAndAtivo(Long gerenciaId, boolean ativo, Pageable pageable);
}
