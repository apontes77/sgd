package br.com.sgd.frequencia;
import br.com.sgd.adolescente.VinculoAdolescenteDiscipulado;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface VinculoHistoricoRepository extends JpaRepository<VinculoAdolescenteDiscipulado,Long>{
 @Query("select v from VinculoAdolescenteDiscipulado v join fetch v.adolescente where v.discipulado.id=:discipuladoId and v.ativo=true and v.adolescente.ativo=true order by v.adolescente.nome")
 List<VinculoAdolescenteDiscipulado> atuais(@Param("discipuladoId")Long discipuladoId);
}
