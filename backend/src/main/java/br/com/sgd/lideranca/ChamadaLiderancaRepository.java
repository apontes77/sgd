package br.com.sgd.lideranca;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChamadaLiderancaRepository extends JpaRepository<ChamadaLideranca, Long> {
  Optional<ChamadaLideranca> findByData(LocalDate data);
}
