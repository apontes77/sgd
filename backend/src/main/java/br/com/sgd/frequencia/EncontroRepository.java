package br.com.sgd.frequencia;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sgd.organizacao.Discipulado;

public interface EncontroRepository extends JpaRepository<Encontro, Long> {
  List<Encontro> findAllByDiscipuladoIdAndDataBetweenOrderByDataDesc(
      Long discipuladoId, LocalDate inicio, LocalDate fim);

  boolean existsByDiscipuladoIdAndData(Long discipuladoId, LocalDate data);

  boolean existsByDiscipuladoIdAndDataAndIdNot(Long discipuladoId, LocalDate data, Long id);

  Optional<Encontro> findByDiscipuladoIdAndData(Long discipuladoId, LocalDate data);

  @Query(
      """
      select d from Discipulado d
      where d.ativo = true
        and not exists (
          select 1 from Encontro e
          where e.discipulado = d
            and e.data = :data
            and (e.situacao = :naoRealizado or e.chamadaSalvaEm is not null)
        )
      """)
  List<Discipulado> findAtivosPendentesDeLancamento(
      @Param("data") LocalDate data, @Param("naoRealizado") SituacaoEncontro naoRealizado);
}
