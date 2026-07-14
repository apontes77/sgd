package br.com.sgd.frequencia;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VisitanteRepository extends JpaRepository<Visitante,Long>{
 Optional<Visitante> findByEncontroId(Long encontroId);
 @EntityGraph(attributePaths = "encontro")
 List<Visitante> findAllByEncontroIdIn(Collection<Long> encontroIds);
}
