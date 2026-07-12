package br.com.sgd.organizacao;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

public interface DiscipuladoRepository extends JpaRepository<Discipulado, Long> {
    @Override
    @EntityGraph(attributePaths = {"gerencia", "discipulador"})
    Page<Discipulado> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"gerencia", "discipulador", "coLideres", "coLideres.perfis"})
    Optional<Discipulado> findById(Long id);

    List<Discipulado> findAllByGerenciaIdAndAtivoTrue(Long gerenciaId);
    @EntityGraph(attributePaths = {"gerencia", "discipulador"})
    Page<Discipulado> findAllByGerenciaId(Long gerenciaId, Pageable pageable);
    @EntityGraph(attributePaths = {"gerencia", "discipulador"})
    Page<Discipulado> findAllByAtivo(boolean ativo, Pageable pageable);
    @EntityGraph(attributePaths = {"gerencia", "discipulador"})
    Page<Discipulado> findAllByGerenciaIdAndAtivo(Long gerenciaId, boolean ativo, Pageable pageable);
}
