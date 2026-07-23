import { request } from '@/shared/api/httpClient'

export type SituacaoRelatorio = 'PRESENTE' | 'AUSENTE'
export type SituacaoEncontroRelatorio = 'REALIZADO' | 'NAO_REALIZADO'
export interface IdentificacaoRelatorio {
  id: number
  nome: string
}
export interface DiscipuladoRelatorio extends IdentificacaoRelatorio {
  sexo: 'MASCULINO' | 'FEMININO'
}
export interface ParticipanteRelatorio {
  adolescenteId: number
  nome: string
  telefone: string | null
  situacao: SituacaoRelatorio
}
export interface ResumoRelatorio {
  presentes: number
  ausentes: number
  participantes: number
  visitantes: number
  percentualPresenca: number
}
export interface RelatorioEncontro {
  encontroId: number
  data: string
  situacao: SituacaoEncontroRelatorio
  justificativa: string | null
  gerencia: IdentificacaoRelatorio
  discipulado: DiscipuladoRelatorio
  discipulador: IdentificacaoRelatorio
  coLideres: IdentificacaoRelatorio[]
  participantes: ParticipanteRelatorio[]
  visitantes: number
  resumo: ResumoRelatorio
}
export interface RelatorioDiarioResponse {
  data: string
  emitidoEm: string
  relatorios: RelatorioEncontro[]
}

export interface RelatorioPeriodoResponse {
  dataInicio: string
  dataFim: string
  emitidoEm: string
  relatorios: RelatorioEncontro[]
}
export const relatorioApi = {
  consultarFrequenciaDiaria: (data: string) =>
    request<RelatorioDiarioResponse>(`/relatorios/frequencia-diaria?${new URLSearchParams({ data })}`),
  consultarFrequencia: (dataInicio: string, dataFim: string) =>
    request<RelatorioPeriodoResponse>(`/relatorios/frequencia?${new URLSearchParams({ dataInicio, dataFim })}`),
}
