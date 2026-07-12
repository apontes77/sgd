package br.com.sgd.frequencia;

import br.com.sgd.user.User;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class ChamadaService {
 private final EncontroService encontros; private final FrequenciaRepository frequencias; private final VisitanteRepository visitantes; private final Clock clock;
 public ChamadaService(EncontroService e,FrequenciaRepository f,VisitanteRepository v,Clock c){encontros=e;frequencias=f;visitantes=v;clock=c;}
 @Transactional(readOnly=true) public List<Frequencia> listar(User ator,long encontroId){var e=encontros.encontro(encontroId);encontros.escopoAcesso().exigirLeitura(ator,e.getDiscipulado());return frequencias.findAllByEncontroIdOrderByAdolescenteNome(encontroId);}
 public List<Frequencia> salvar(User ator,long encontroId,List<ItemChamada> itens){var e=encontros.encontro(encontroId);encontros.exigirEditavel(ator,e);if(itens==null)throw new IllegalArgumentException("A chamada é obrigatória.");
   var elegiveis=encontros.elegiveis(e);var porId=elegiveis.stream().collect(Collectors.toMap(v->v.getAdolescente().getId(),Function.identity()));
   var ids=itens.stream().map(ItemChamada::adolescenteId).collect(Collectors.toList());
   if(new HashSet<>(ids).size()!=ids.size()||!porId.keySet().equals(new HashSet<>(ids)))EncontroService.conflito("A chamada deve conter exatamente os adolescentes vinculados na data do encontro.");
   var agora=clock.instant();var resultado=new ArrayList<Frequencia>();var mudancas=new ArrayList<Map<String,Object>>();
   for(var item:itens){if(item.situacao()==null)throw new IllegalArgumentException("A situação da frequência é obrigatória.");var existente=frequencias.findByEncontroIdAndAdolescenteId(encontroId,item.adolescenteId());var anterior=existente.map(Frequencia::getSituacao).orElse(null);var f=existente.orElseGet(()->new Frequencia(e,porId.get(item.adolescenteId()).getAdolescente(),item.situacao(),agora));if(existente.isPresent())f.atualizar(item.situacao(),agora);resultado.add(frequencias.save(f));if(anterior!=item.situacao()){var m=new LinkedHashMap<String,Object>();m.put("adolescenteId",item.adolescenteId());m.put("anterior",anterior);m.put("novo",item.situacao());mudancas.add(m);}}
   if(!mudancas.isEmpty())encontros.auditar(ator,"FREQUENCIA","SUBSTITUIR_CHAMADA",Map.of("encontroId",encontroId,"alteracoes",mudancas));return resultado;
 }
 public int salvarVisitantes(User ator,long encontroId,int quantidade){var e=encontros.encontro(encontroId);encontros.exigirEditavel(ator,e);if(quantidade<0)throw new IllegalArgumentException("A quantidade não pode ser negativa.");var existente=visitantes.findByEncontroId(encontroId);int anterior=existente.map(Visitante::getQuantidade).orElse(0);var v=existente.orElseGet(()->new Visitante(e,quantidade,clock.instant()));if(existente.isPresent())v.atualizar(quantidade,clock.instant());visitantes.save(v);if(anterior!=quantidade)encontros.auditar(ator,"VISITANTE","ALTERAR",Map.of("encontroId",encontroId,"anterior",anterior,"novo",quantidade));return quantidade;}
 public record ItemChamada(Long adolescenteId,SituacaoFrequencia situacao){}
}
