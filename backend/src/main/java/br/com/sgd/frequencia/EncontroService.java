package br.com.sgd.frequencia;

import br.com.sgd.adolescente.EscopoOrganizacionalService;
import br.com.sgd.audit.*;
import br.com.sgd.organizacao.*;
import br.com.sgd.user.Role;
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
 public Encontro criar(User ator,long discipuladoId,LocalDate data,SituacaoEncontro situacao){return criar(ator,discipuladoId,data,situacao,null);}
 public Encontro criar(User ator,long discipuladoId,LocalDate data,SituacaoEncontro situacao,String justificativa){var d=discipulado(discipuladoId);escopo.exigirAlteracao(ator,d);if(!d.isAtivo()) conflito("O discipulado está inativo.");if(situacao==SituacaoEncontro.CANCELADO)exigirRegistroNaoRealizado(ator);String motivo=validarJustificativa(situacao,justificativa);var e=encontros.save(new Encontro(d,data,situacao,motivo,clock.instant()));var detalhes=new LinkedHashMap<String,Object>();detalhes.put("id",e.getId());detalhes.put("situacao",situacao);detalhes.put("justificativa",motivo);auditar(ator,"ENCONTRO","CRIAR",detalhes);return e;}
 public Encontro atualizar(User ator,long id,LocalDate data,SituacaoEncontro situacao){return atualizar(ator,id,data,situacao,null);}
 public Encontro atualizar(User ator,long id,LocalDate data,SituacaoEncontro situacao,String justificativa){var e=encontro(id);escopo.exigirAlteracao(ator,e.getDiscipulado());var novaSituacao=situacao==null?e.getSituacao():situacao;if(e.getSituacao()==SituacaoEncontro.CANCELADO||novaSituacao==SituacaoEncontro.CANCELADO)exigirRegistroNaoRealizado(ator);String motivo=novaSituacao==SituacaoEncontro.CANCELADO?validarJustificativa(novaSituacao,justificativa==null?e.getJustificativa():justificativa):validarJustificativa(novaSituacao,justificativa);if(novaSituacao==SituacaoEncontro.CANCELADO&&(frequencias.existsByEncontroId(id)||visitantes.findByEncontroId(id).map(v->v.getQuantidade()>0).orElse(false)))conflito("Um encontro com chamada registrada não pode ser marcado como não realizado.");var antes=estado(e);e.atualizar(data,novaSituacao,motivo,clock.instant());auditar(ator,"ENCONTRO","ALTERAR",Map.of("id",id,"anterior",antes,"novo",estado(e)));return e;}
 @Transactional(readOnly=true) public List<Encontro> listar(User ator,long discipuladoId,LocalDate inicio,LocalDate fim){var d=discipulado(discipuladoId);escopo.exigirLeitura(ator,d);return encontros.findAllByDiscipuladoIdAndDataBetweenOrderByDataDesc(discipuladoId,inicio==null?LocalDate.of(1900,1,1):inicio,fim==null?LocalDate.of(2999,12,31):fim);}
 @Transactional(readOnly=true) public Encontro encontro(long id){return encontros.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Encontro não encontrado."));}
 private Discipulado discipulado(long id){return discipulados.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Discipulado não encontrado."));}
 private static String validarJustificativa(SituacaoEncontro situacao,String justificativa){String valor=justificativa==null?null:justificativa.trim();if(situacao==SituacaoEncontro.CANCELADO&&(valor==null||valor.isEmpty()))throw new IllegalArgumentException("A justificativa é obrigatória para encontro não realizado.");if(valor!=null&&valor.length()>500)throw new IllegalArgumentException("A justificativa deve ter no máximo 500 caracteres.");if(situacao==SituacaoEncontro.REALIZADO&&valor!=null&&!valor.isEmpty())throw new IllegalArgumentException("A justificativa só pode ser informada para encontro não realizado.");return situacao==SituacaoEncontro.CANCELADO?valor:null;}
 private static void exigirRegistroNaoRealizado(User ator){if(!ator.getPerfis().contains(Role.ADMIN)&&!ator.getPerfis().contains(Role.DISCIPULADOR))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Somente administradores e discipuladores podem marcar um encontro como não realizado.");}
 private static Map<String,Object> estado(Encontro e){var estado=new LinkedHashMap<String,Object>();estado.put("data",e.getData());estado.put("situacao",e.getSituacao());estado.put("justificativa",e.getJustificativa());return estado;}
 void exigirEditavel(User ator,Encontro e){escopo.exigirAlteracao(ator,e.getDiscipulado());if(e.getSituacao()==SituacaoEncontro.CANCELADO)conflito("Não é possível registrar dados em encontro cancelado.");if(!ator.getPerfis().contains(br.com.sgd.user.Role.ADMIN)&&clock.instant().isAfter(e.getCriadoEm().plus(Duration.ofHours(3))))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"A janela de três horas para alteração terminou.");}
 List<br.com.sgd.adolescente.VinculoAdolescenteDiscipulado> participantesAtuais(Encontro e){return vinculos.atuais(e.getDiscipulado().getId());}
 void auditar(User ator,String entidade,String acao,Object detalhes){try{auditoria.save(new AuditLog(ator,entidade,acao,json.writeValueAsString(detalhes)));}catch(JsonProcessingException ex){throw new IllegalStateException("Falha ao registrar auditoria.",ex);}}
 static void conflito(String msg){throw new ResponseStatusException(HttpStatus.CONFLICT,msg);}
}
