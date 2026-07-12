package br.com.sgd.frequencia;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VisitanteRepository extends JpaRepository<Visitante,Long>{Optional<Visitante> findByEncontroId(Long encontroId);}
