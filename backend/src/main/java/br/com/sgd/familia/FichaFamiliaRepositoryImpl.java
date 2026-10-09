package br.com.sgd.familia;

import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.hibernate.query.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class FichaFamiliaRepositoryImpl implements FichaFamiliaRepositoryCustom {
  private static final String NAO_CONSTA_INTERVENCAO =
      """
      (
        f.observacoes.intervencao is null
        or trim(f.observacoes.intervencao) = ''
        or trim(f.observacoes.intervencao) = 'Não consta'
        or lower(trim(f.observacoes.intervencao)) = 'não consta'
        or upper(trim(f.observacoes.intervencao)) = 'NÃO CONSTA'
      )
      """;

  private static final String WHERE =
      """
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
        and (
          :tipoDesafio is null
          or (:tipoDesafio = 'FINANCEIRO' and f.conhecendo.desafioFinanceiro = true)
          or (:tipoDesafio = 'EMOCIONAL' and f.conhecendo.desafioEmocional = true)
          or (:tipoDesafio = 'ESPIRITUAL' and f.conhecendo.desafioEspiritual = true)
        )
        and (
          :situacaoIntervencao is null
          or (:situacaoIntervencao = 'REGISTRADA' and not
      """
          + NAO_CONSTA_INTERVENCAO
          + """
          )
          or (:situacaoIntervencao = 'NAO_CONSTA' and
      """
          + NAO_CONSTA_INTERVENCAO
          + """
          )
        )
      """;

  private static final String SELECT = "select f from FichaFamilia f " + WHERE;
  private static final String COUNT = "select count(f) from FichaFamilia f " + WHERE;
  private static final String ORDER = " order by f.adolescente.nome asc";

  private final EntityManager em;

  public FichaFamiliaRepositoryImpl(EntityManager em) {
    this.em = em;
  }

  @Override
  public Page<FichaFamilia> listarNoEscopo(
      boolean admin,
      Long gerenteId,
      Long discipuladorId,
      Long coLiderId,
      FiltroListagemFamilia filtro,
      Pageable pageable) {
    TypedQuery<FichaFamilia> dados = em.createQuery(SELECT + ORDER, FichaFamilia.class);
    TypedQuery<Long> total = em.createQuery(COUNT, Long.class);
    aplicar(dados, admin, gerenteId, discipuladorId, coLiderId, filtro);
    aplicar(total, admin, gerenteId, discipuladorId, coLiderId, filtro);
    dados.setFirstResult(Math.toIntExact(pageable.getOffset()));
    dados.setMaxResults(pageable.getPageSize());
    List<FichaFamilia> conteudo = dados.getResultList();
    return new PageImpl<>(conteudo, pageable, total.getSingleResult());
  }

  private static void aplicar(
      TypedQuery<?> query,
      boolean admin,
      Long gerenteId,
      Long discipuladorId,
      Long coLiderId,
      FiltroListagemFamilia filtro) {
    Query<?> hibernate = query.unwrap(Query.class);
    hibernate.setParameter("admin", admin);
    hibernate.setParameter("gerenteId", gerenteId, Long.class);
    hibernate.setParameter("discipuladorId", discipuladorId, Long.class);
    hibernate.setParameter("coLiderId", coLiderId, Long.class);
    hibernate.setParameter("busca", filtro.busca(), String.class);
    hibernate.setParameter("situacaoIgreja", filtro.situacaoIgreja(), SituacaoIgrejaFamilia.class);
    hibernate.setParameter("situacaoPais", filtro.situacaoPais(), SituacaoPaisFamilia.class);
    hibernate.setParameter("situacaoFicha", filtro.situacaoFicha(), String.class);
    hibernate.setParameter("tipoDesafio", filtro.tipoDesafio(), String.class);
    hibernate.setParameter("situacaoIntervencao", filtro.situacaoIntervencao(), String.class);
  }
}
