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
      where (:admin = true
         or exists (
           select 1 from VinculoAdolescenteDiscipulado v
           join v.discipulado d
           left join d.coLideres c
           where v.adolescente = f.adolescente
             and v.ativo = true
             and (
               (:gerenteId is not null and d.gerencia.gerente.id = :gerenteId)
               or (:discipuladorId is not null and d.discipulador.id = :discipuladorId)
               or (:coLiderId is not null and c.id = :coLiderId)
             )
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
        and (
          :situacaoFicha is null
          or :situacaoFicha
            = case
              when exists (
                select 1 from f.responsaveis r
                where r.nome <> 'Não consta'
              )
                then 'PREENCHIDA'
              else 'NAO_CONSTA'
            end
        )
      order by f.adolescente.nome asc
      """)
  Page<FichaFamilia> listarNoEscopo(
      @Param("admin") boolean admin,
      @Param("gerenteId") Long gerenteId,
      @Param("discipuladorId") Long discipuladorId,
      @Param("coLiderId") Long coLiderId,
      @Param("busca") String busca,
      @Param("situacaoIgreja") SituacaoIgrejaFamilia situacaoIgreja,
      @Param("situacaoPais") SituacaoPaisFamilia situacaoPais,
      @Param("situacaoFicha") String situacaoFicha,
      Pageable pageable);
}
