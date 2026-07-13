package br.com.sgd.frequencia;

import br.com.sgd.adolescente.EscopoOrganizacionalService;
import br.com.sgd.audit.*;
import br.com.sgd.organizacao.*;
import br.com.sgd.user.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional
public class EncontroService {
 private final EncontroRepository encontros; private final FrequenciaRepository frequencias; private final VisitanteRepository visitantes;
 private final VinculoHistoricoRepository vinculos; private final DiscipuladoRepository discipulados; private final EscopoOrganizacionalService escopo;
 private final AuditLogRepository auditoria; private final ObjectMapper json; private final Clock clock;
 public EncontroService(EncontroRepository e,FrequenciaRepository f,VisitanteRepository vi,VinculoHistoricoRepository vh,DiscipuladoRepository d,EscopoOrganizacionalService es,AuditLogRepository a,ObjectMapper j,Clock c){encontros=e;frequencias=f;visitantes=vi;vinculos=vh;discipulados=d;escopo=es;auditoria=a;json=j;clock=c;}
 public Encontro criar(User ator,long discipuladoId,LocalDate data,SituacaoEncontro situacao){var d=discipulado(discipuladoId);escopo.exigirAlteracao(ator,d);if(!d.isAtivo()) conflito("O discipulado está inativo.");var e=encontros.save(new Encontro(d,data,situacao,clock.instant()));auditar(ator,"ENCONTRO","CRIAR",Map.of("id",e.getId(),"situacao",situacao));return e;}
 public Encontro atualizar(User ator,long id,LocalDate data,SituacaoEncontro situacao){var e=encontro(id);escopo.exigirAlteracao(ator,e.getDiscipulado());if(situacao==SituacaoEncontro.CANCELADO&&(frequencias.existsByEncontroId(id)||visitantes.findByEncontroId(id).map(v->v.getQuantidade()>0).orElse(false)))conflito("Um encontro com chamada registrada não pode ser cancelado.");var antes=Map.of("data",e.getData(),"situacao",e.getSituacao());e.atualizar(data,situacao,clock.instant());auditar(ator,"ENCONTRO","ALTERAR",Map.of("id",id,"anterior",antes,"novo",Map.of("data",e.getData(),"situacao",e.getSituacao())));return e;}
 @Transactional(readOnly=true) public List<Encontro> listar(User ator,long discipuladoId,LocalDate inicio,LocalDate fim){var d=discipulado(discipuladoId);escopo.exigirLeitura(ator,d);return encontros.findAllByDiscipuladoIdAndDataBetweenOrderByDataDesc(discipuladoId,inicio==null?LocalDate.of(1900,1,1):inicio,fim==null?LocalDate.of(2999,12,31):fim);}
 @Transactional(readOnly=true) public Encontro encontro(long id){return encontros.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Encontro não encontrado."));}
 private Discipulado discipulado(long id){return discipulados.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Discipulado não encontrado."));}
 void exigirEditavel(User ator,Encontro e){escopo.exigirAlteracao(ator,e.getDiscipulado());if(e.getSituacao()==SituacaoEncontro.CANCELADO)conflito("Não é possível registrar dados em encontro cancelado.");if(!ator.getPerfis().contains(br.com.sgd.user.Role.ADMIN)&&clock.instant().isAfter(e.getCriadoEm().plus(Duration.ofHours(3))))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"A janela de três horas para alteração terminou.");}
 List<br.com.sgd.adolescente.VinculoAdolescenteDiscipulado> participantesAtuais(Encontro e){return vinculos.atuais(e.getDiscipulado().getId());}
 void auditar(User ator,String entidade,String acao,Object detalhes){try{auditoria.save(new AuditLog(ator,entidade,acao,json.writeValueAsString(detalhes)));}catch(JsonProcessingException ex){throw new IllegalStateException("Falha ao registrar auditoria.",ex);}}
 static void conflito(String msg){throw new ResponseStatusException(HttpStatus.CONFLICT,msg);}
}
