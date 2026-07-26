package br.com.sgd.organizacao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GerenciaRepository
    extends JpaRepository<Gerencia, Long>, JpaSpecificationExecutor<Gerencia> {
  @Override
  @EntityGraph(attributePaths = {"gerente", "faixasEtarias"})
  Page<Gerencia> findAll(Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"gerente", "faixasEtarias"})
  Optional<Gerencia> findById(Long id);

  @EntityGraph(attributePaths = {"gerente", "faixasEtarias"})
  List<Gerencia> findAllByGerenteIdAndAtivoTrue(Long gerenteId);
}
