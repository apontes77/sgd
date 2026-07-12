package br.com.sgd.frequencia;
import br.com.sgd.adolescente.VinculoAdolescenteDiscipulado;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface VinculoHistoricoRepository extends JpaRepository<VinculoAdolescenteDiscipulado,Long>{
 @Query("select v from VinculoAdolescenteDiscipulado v join fetch v.adolescente where v.discipulado.id=:discipuladoId and v.dataInicio<=:data and (v.dataFim is null or v.dataFim>=:data)")
 List<VinculoAdolescenteDiscipulado> elegiveis(@Param("discipuladoId")Long discipuladoId,@Param("data")LocalDate data);
}
