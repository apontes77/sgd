package br.com.sgd.frequencia;
import java.util.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FrequenciaRepository extends JpaRepository<Frequencia,Long>{
 @EntityGraph(attributePaths = "adolescente")
 List<Frequencia> findAllByEncontroIdOrderByAdolescenteNome(Long encontroId);
 boolean existsByEncontroId(Long encontroId);
}
