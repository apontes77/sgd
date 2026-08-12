import { request, requestBlob } from '@/shared/api/httpClient'

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

export type FiltroAtivoExport = 'ativos' | 'inativos' | 'todos'

export const relatorioApi = {
  consultarFrequenciaDiaria: (data: string, discipuladoId?: number) => {
    const params = new URLSearchParams({ data })
    if (discipuladoId != null) params.set('discipuladoId', String(discipuladoId))
    return request<RelatorioDiarioResponse>(`/relatorios/frequencia-diaria?${params}`)
  },
  consultarFrequencia: (dataInicio: string, dataFim: string, discipuladoId?: number) => {
    const params = new URLSearchParams({ dataInicio, dataFim })
    if (discipuladoId != null) params.set('discipuladoId', String(discipuladoId))
    return request<RelatorioPeriodoResponse>(`/relatorios/frequencia?${params}`)
  },
  exportarAdolescentes: async (filtro: { discipuladoId?: number; ativo?: FiltroAtivoExport } = {}) => {
    const params = new URLSearchParams()
    if (filtro.discipuladoId != null) params.set('discipuladoId', String(filtro.discipuladoId))
    if (filtro.ativo === 'ativos') params.set('ativo', 'true')
    if (filtro.ativo === 'inativos') params.set('ativo', 'false')
    const query = params.toString()
    const path = `/relatorios/adolescentes/export${query ? `?${query}` : ''}`
    const { blob, filename } = await requestBlob(path)
    const url = URL.createObjectURL(blob)
    try {
      const link = document.createElement('a')
      link.href = url
      link.download = filename ?? 'adolescentes.csv'
      document.body.appendChild(link)
      link.click()
      link.remove()
    } finally {
      URL.revokeObjectURL(url)
    }
  },
}
