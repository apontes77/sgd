package br.com.sgd.familia;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FichaFamiliaRepository extends JpaRepository<FichaFamilia, Long> {
  Optional<FichaFamilia> findByAdolescenteId(long adolescenteId);

  List<FichaFamilia> findByAdolescenteIdIn(Collection<Long> adolescenteIds);

  @Query(
      """
      select f from FichaFamilia f
      where (:gerenteId is null
         or exists (
           select 1 from VinculoAdolescenteDiscipulado v
           where v.adolescente = f.adolescente
             and v.ativo = true
             and v.discipulado.gerencia.gerente.id = :gerenteId
         ))
        and (:busca is null
         or lower(f.adolescente.nome) like lower(concat('%', cast(:busca as string), '%'))
         or exists (
           select 1 from VinculoAdolescenteDiscipulado v2
           where v2.adolescente = f.adolescente
             and v2.ativo = true
             and lower(v2.discipulado.nome) like lower(concat('%', cast(:busca as string), '%'))
         ))
        and (:situacaoIgreja is null or f.situacao.situacaoIgreja = :situacaoIgreja)
        and (:situacaoPais is null or f.situacao.situacaoPais = :situacaoPais)
      order by f.adolescente.nome asc
      """)
  Page<FichaFamilia> listarNoEscopo(
      @Param("gerenteId") Long gerenteId,
      @Param("busca") String busca,
      @Param("situacaoIgreja") SituacaoIgrejaFamilia situacaoIgreja,
      @Param("situacaoPais") SituacaoPaisFamilia situacaoPais,
      Pageable pageable);
}
