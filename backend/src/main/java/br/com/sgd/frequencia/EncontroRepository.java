package br.com.sgd.frequencia;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EncontroRepository extends JpaRepository<Encontro,Long>{
 List<Encontro> findAllByDiscipuladoIdAndDataBetweenOrderByDataDesc(Long discipuladoId,LocalDate inicio,LocalDate fim);
}
