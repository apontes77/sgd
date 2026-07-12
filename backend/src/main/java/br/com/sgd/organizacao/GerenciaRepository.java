package br.com.sgd.organizacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface GerenciaRepository extends JpaRepository<Gerencia, Long> {
    @Override
    @EntityGraph(attributePaths = "gerente")
    Page<Gerencia> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "gerente")
    Optional<Gerencia> findById(Long id);
}
