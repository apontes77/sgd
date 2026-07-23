package br.com.sgd.frequencia;

import java.time.Instant;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import br.com.sgd.adolescente.Adolescente;

public interface InativacaoAdolescenteRepository extends JpaRepository<Adolescente, Long> {
  @Modifying
  @Query(
      value =
          "UPDATE adolescentes a SET ativo=false, atualizado_em=CURRENT_TIMESTAMP WHERE a.ativo=true AND a.criado_em < :limite AND NOT EXISTS (SELECT 1 FROM frequencias f JOIN encontros e ON e.id=f.encontro_id WHERE f.adolescente_id=a.id AND f.situacao='PRESENTE' AND e.data >= :dataLimite)",
      nativeQuery = true)
  int inativarSemParticipacao(
      @Param("limite") Instant limite, @Param("dataLimite") java.time.LocalDate dataLimite);
}
