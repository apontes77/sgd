package br.com.sgd.frequencia;

import br.com.sgd.audit.AuditLog;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AuditoriaConsultaRepository extends JpaRepository<AuditLog,Long> {
 @Query(value="SELECT a.id AS id, a.usuario_id AS usuarioId, a.data_hora AS dataHora, a.entidade AS entidade, a.acao AS acao, a.detalhes AS detalhes FROM auditoria a WHERE (:entidade IS NULL OR a.entidade=:entidade) AND (:usuarioId IS NULL OR a.usuario_id=:usuarioId)",countQuery="SELECT count(*) FROM auditoria a WHERE (:entidade IS NULL OR a.entidade=:entidade) AND (:usuarioId IS NULL OR a.usuario_id=:usuarioId)",nativeQuery=true)
 Page<LinhaAuditoria> consultar(@Param("entidade")String entidade,@Param("usuarioId")Long usuarioId,Pageable pageable);
 interface LinhaAuditoria { Long getId(); Long getUsuarioId(); java.time.OffsetDateTime getDataHora(); String getEntidade(); String getAcao(); String getDetalhes(); }
}
