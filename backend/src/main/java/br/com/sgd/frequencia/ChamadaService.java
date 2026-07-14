package br.com.sgd.frequencia;

import br.com.sgd.adolescente.Adolescente;
import br.com.sgd.adolescente.EscopoOrganizacionalService;
import br.com.sgd.user.User;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class ChamadaService {
 private final EncontroService encontros; private final FrequenciaRepository frequencias; private final VisitanteRepository visitantes; private final EscopoOrganizacionalService escopo; private final Clock clock;
 public ChamadaService(EncontroService e,FrequenciaRepository f,VisitanteRepository v,EscopoOrganizacionalService es,Clock c){encontros=e;frequencias=f;visitantes=v;escopo=es;clock=c;}
 @Transactional(readOnly=true) public List<Frequencia> listar(User ator,long encontroId){var e=encontros.encontro(encontroId);escopo.exigirLeitura(ator,e.getDiscipulado());return frequencias.findAllByEncontroIdOrderByAdolescenteNome(encontroId);}
 public List<Frequencia> salvar(User ator,long encontroId,List<ItemChamada> itens){var e=encontros.encontro(encontroId);encontros.exigirEditavel(ator,e);if(itens==null)throw new IllegalArgumentException("A chamada é obrigatória.");
   var atuais=encontros.participantesAtuais(e);var existentes=frequencias.findAllByEncontroIdOrderByAdolescenteNome(encontroId);
   Map<Long,Adolescente> porId=atuais.stream().map(v->v.getAdolescente()).collect(Collectors.toMap(Adolescente::getId,Function.identity()));
   existentes.forEach(f->porId.putIfAbsent(f.getAdolescente().getId(),f.getAdolescente()));
   var existentesPorId=existentes.stream().collect(Collectors.toMap(f->f.getAdolescente().getId(),Function.identity()));
   var ids=itens.stream().map(ItemChamada::adolescenteId).collect(Collectors.toList());
   if(ids.contains(null)||new HashSet<>(ids).size()!=ids.size()||!porId.keySet().equals(new HashSet<>(ids)))EncontroService.conflito("A chamada deve conter os adolescentes ativos do discipulado e os registros anteriores deste encontro.");
   var agora=clock.instant();var resultado=new ArrayList<Frequencia>();var mudancas=new ArrayList<Map<String,Object>>();
   for(var item:itens){if(item.situacao()==null)throw new IllegalArgumentException("A situação da frequência é obrigatória.");var existente=Optional.ofNullable(existentesPorId.get(item.adolescenteId()));var anterior=existente.map(Frequencia::getSituacao).orElse(null);var f=existente.orElseGet(()->new Frequencia(e,porId.get(item.adolescenteId()),item.situacao(),agora));if(existente.isPresent())f.atualizar(item.situacao(),agora);resultado.add(frequencias.save(f));if(anterior!=item.situacao()){var m=new LinkedHashMap<String,Object>();m.put("adolescenteId",item.adolescenteId());m.put("anterior",anterior);m.put("novo",item.situacao());mudancas.add(m);}}
   if(!mudancas.isEmpty())encontros.auditar(ator,"FREQUENCIA","SUBSTITUIR_CHAMADA",Map.of("encontroId",encontroId,"alteracoes",mudancas));return resultado;
 }
 public int salvarVisitantes(User ator,long encontroId,int quantidade){var e=encontros.encontro(encontroId);encontros.exigirEditavel(ator,e);if(quantidade<0)throw new IllegalArgumentException("A quantidade não pode ser negativa.");var existente=visitantes.findByEncontroId(encontroId);int anterior=existente.map(Visitante::getQuantidade).orElse(0);var v=existente.orElseGet(()->new Visitante(e,quantidade,clock.instant()));if(existente.isPresent())v.atualizar(quantidade,clock.instant());visitantes.save(v);if(anterior!=quantidade)encontros.auditar(ator,"VISITANTE","ALTERAR",Map.of("encontroId",encontroId,"anterior",anterior,"novo",quantidade));return quantidade;}
 public record ItemChamada(Long adolescenteId,SituacaoFrequencia situacao){}
}
