package br.com.sgd.organizacao;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface DiscipuladoRepository extends JpaRepository<Discipulado, Long>, JpaSpecificationExecutor<Discipulado> {
    @Override
    @EntityGraph(attributePaths = {"gerencia", "discipulador"})
    Page<Discipulado> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"gerencia", "discipulador", "coLideres", "coLideres.perfis"})
    Optional<Discipulado> findById(Long id);

    List<Discipulado> findAllByGerenciaIdAndAtivoTrue(Long gerenciaId);
    List<Discipulado> findAllByGerenciaIdOrderByNomeAsc(Long gerenciaId);
    @EntityGraph(attributePaths = {"gerencia", "discipulador", "coLideres", "coLideres.perfis"})
    @Query("select distinct d from Discipulado d left join d.coLideres c where d.discipulador.id = :usuarioId or c.id = :usuarioId")
    List<Discipulado> findAllByLiderancaUsuarioId(@Param("usuarioId") Long usuarioId);
    @EntityGraph(attributePaths = {"gerencia", "discipulador"})
    Page<Discipulado> findAllByGerenciaId(Long gerenciaId, Pageable pageable);
    @EntityGraph(attributePaths = {"gerencia", "discipulador"})
    Page<Discipulado> findAllByAtivo(boolean ativo, Pageable pageable);
    @EntityGraph(attributePaths = {"gerencia", "discipulador"})
    Page<Discipulado> findAllByGerenciaIdAndAtivo(Long gerenciaId, boolean ativo, Pageable pageable);
}
