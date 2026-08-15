package br.com.sgd.lideranca;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChamadaLiderancaRepository extends JpaRepository<ChamadaLideranca, Long> {
  Optional<ChamadaLideranca> findByData(LocalDate data);

  @EntityGraph(
      attributePaths = {
        "itens",
        "itens.discipulado",
        "itens.discipulado.gerencia",
        "itens.presencas",
        "itens.presencas.usuario"
      })
  List<ChamadaLideranca> findAllByDataBetweenOrderByDataAsc(LocalDate inicio, LocalDate fim);
}
