package br.com.sgd.frequencia;

import br.com.sgd.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/encontros")
public class FrequenciaController {
 private final EncontroService encontros; private final ChamadaService chamadas;
 public FrequenciaController(EncontroService e,ChamadaService c){encontros=e;chamadas=c;}
 @GetMapping public List<EncontroResponse> listar(@AuthenticationPrincipal User u,@RequestParam long discipuladoId,@RequestParam(required=false)LocalDate dataInicio,@RequestParam(required=false)LocalDate dataFim){return encontros.listar(u,discipuladoId,dataInicio,dataFim).stream().map(EncontroResponse::of).toList();}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public EncontroResponse criar(@AuthenticationPrincipal User u,@Valid @RequestBody EncontroRequest r){return EncontroResponse.of(encontros.criar(u,r.discipuladoId(),r.data(),r.situacao(),r.justificativa()));}
 @PatchMapping("/{id}") public EncontroResponse atualizar(@AuthenticationPrincipal User u,@PathVariable long id,@Valid @RequestBody AtualizarEncontroRequest r){return EncontroResponse.of(encontros.atualizar(u,id,r.data(),r.situacao(),r.justificativa()));}
 @GetMapping("/{id}/frequencias") public List<FrequenciaResponse> chamada(@AuthenticationPrincipal User u,@PathVariable long id){return chamadas.listar(u,id).stream().map(FrequenciaResponse::of).toList();}
 @PutMapping("/{id}/frequencias") public List<FrequenciaResponse> salvar(@AuthenticationPrincipal User u,@PathVariable long id,@Valid @RequestBody ChamadaRequest r){return chamadas.salvar(u,id,r.frequencias().stream().map(i->new ChamadaService.ItemChamada(i.adolescenteId(),i.situacao())).toList()).stream().map(FrequenciaResponse::of).toList();}
 @PutMapping("/{id}/visitantes") public VisitantesResponse visitantes(@AuthenticationPrincipal User u,@PathVariable long id,@Valid @RequestBody VisitantesRequest r){return new VisitantesResponse(chamadas.salvarVisitantes(u,id,r.quantidade()));}
 public record EncontroRequest(@NotNull Long discipuladoId,@NotNull LocalDate data,@NotNull SituacaoEncontro situacao,@Size(max=500) String justificativa){}
 public record AtualizarEncontroRequest(LocalDate data,SituacaoEncontro situacao,@Size(max=500) String justificativa){}
 public record ChamadaRequest(@NotNull List<@NotNull @Valid FrequenciaRequest> frequencias){}
 public record FrequenciaRequest(@NotNull Long adolescenteId,@NotNull SituacaoFrequencia situacao){}
 public record VisitantesRequest(@Min(0) int quantidade){} public record VisitantesResponse(int quantidade){}
 public record EncontroResponse(Long id,Long discipuladoId,LocalDate data,SituacaoEncontro situacao,String justificativa,Instant criadoEm){static EncontroResponse of(Encontro e){return new EncontroResponse(e.getId(),e.getDiscipulado().getId(),e.getData(),e.getSituacao(),e.getJustificativa(),e.getCriadoEm());}}
 public record FrequenciaResponse(Long id,Long encontroId,Long adolescenteId,String adolescenteNome,SituacaoFrequencia situacao,Instant registradaEm){static FrequenciaResponse of(Frequencia f){return new FrequenciaResponse(f.getId(),f.getEncontro().getId(),f.getAdolescente().getId(),f.getAdolescente().getNome(),f.getSituacao(),f.getRegistradaEm());}}
}
