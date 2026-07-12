package br.com.sgd.frequencia;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FrequenciaRepository extends JpaRepository<Frequencia,Long>{
 List<Frequencia> findAllByEncontroIdOrderByAdolescenteNome(Long encontroId);
 Optional<Frequencia> findByEncontroIdAndAdolescenteId(Long encontroId,Long adolescenteId);
 boolean existsByEncontroId(Long encontroId);
}
