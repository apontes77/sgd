import { request } from './api'

export type SituacaoRelatorio = 'PRESENTE' | 'AUSENTE'
export interface IdentificacaoRelatorio { id:number; nome:string }
export interface DiscipuladoRelatorio extends IdentificacaoRelatorio { sexo:'MASCULINO' | 'FEMININO' }
export interface ParticipanteRelatorio { adolescenteId:number; nome:string; telefone:string|null; situacao:SituacaoRelatorio }
export interface ResumoRelatorio { presentes:number; ausentes:number; participantes:number; visitantes:number; percentualPresenca:number }
export interface RelatorioEncontro {
  encontroId:number
  data:string
  gerencia:IdentificacaoRelatorio
  discipulado:DiscipuladoRelatorio
  discipulador:IdentificacaoRelatorio
  coLideres:IdentificacaoRelatorio[]
  participantes:ParticipanteRelatorio[]
  visitantes:number
  resumo:ResumoRelatorio
}
export interface RelatorioDiarioResponse { data:string; emitidoEm:string; relatorios:RelatorioEncontro[] }

export const relatorioApi = {
  consultarFrequenciaDiaria: (data:string) => request<RelatorioDiarioResponse>(`/relatorios/frequencia-diaria?${new URLSearchParams({ data })}`),
}
