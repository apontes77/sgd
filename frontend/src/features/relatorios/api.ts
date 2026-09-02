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
  goe: number
  percentualPresenca: number
}
export interface RelatorioEncontro {
  encontroId: number
  data: string
  situacao: SituacaoEncontroRelatorio
  justificativa: string | null
  observacao?: string | null
  observacaoEstrutura?: string | null
  fechamentoAutomatico?: boolean
  gerencia: IdentificacaoRelatorio
  discipulado: DiscipuladoRelatorio
  discipulador: IdentificacaoRelatorio
  coLideres: IdentificacaoRelatorio[]
  participantes: ParticipanteRelatorio[]
  visitantes: number
  goe: number
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

export type PapelLiderancaRelatorio = 'DISCIPULADOR' | 'CO_LIDER'

export interface PresencaLiderancaRelatorio {
  usuarioId: number
  nome: string
  papel: PapelLiderancaRelatorio
  situacao: SituacaoRelatorio
}

export interface DiscipuladoChamadaLiderancaRelatorio {
  discipuladoId: number
  discipuladoNome: string
  sexo: 'MASCULINO' | 'FEMININO'
  gerenciaNome: string
  observacao: string | null
  presencas: PresencaLiderancaRelatorio[]
}

export interface ResumoChamadaLideranca {
  presentes: number
  ausentes: number
  participantes: number
  percentualPresenca: number
}

export interface RelatorioChamadaLideranca {
  chamadaId: number
  data: string
  observacaoGeral: string | null
  discipulados: DiscipuladoChamadaLiderancaRelatorio[]
  resumo: ResumoChamadaLideranca
}

export interface RelatorioChamadaLiderancaPeriodoResponse {
  dataInicio: string
  dataFim: string
  emitidoEm: string
  relatorios: RelatorioChamadaLideranca[]
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
  exportarFrequencia: async (dataInicio: string, dataFim: string, discipuladoId?: number) => {
    const params = new URLSearchParams({ dataInicio, dataFim })
    if (discipuladoId != null) params.set('discipuladoId', String(discipuladoId))
    const { blob, filename } = await requestBlob(`/relatorios/frequencia/export?${params}`)
    const url = URL.createObjectURL(blob)
    try {
      const link = document.createElement('a')
      link.href = url
      link.download = filename ?? 'frequencias.xlsx'
      document.body.appendChild(link)
      link.click()
      link.remove()
    } finally {
      URL.revokeObjectURL(url)
    }
  },
  consultarChamadaLideranca: (dataInicio: string, dataFim: string, discipuladoId?: number) => {
    const params = new URLSearchParams({ dataInicio, dataFim })
    if (discipuladoId != null) params.set('discipuladoId', String(discipuladoId))
    return request<RelatorioChamadaLiderancaPeriodoResponse>(`/relatorios/chamadas-lideranca?${params}`)
  },
  exportarChamadaLideranca: async (dataInicio: string, dataFim: string, discipuladoId?: number) => {
    const params = new URLSearchParams({ dataInicio, dataFim })
    if (discipuladoId != null) params.set('discipuladoId', String(discipuladoId))
    const { blob, filename } = await requestBlob(`/relatorios/chamadas-lideranca/export?${params}`)
    const url = URL.createObjectURL(blob)
    try {
      const link = document.createElement('a')
      link.href = url
      link.download = filename ?? 'chamada-lideranca.xlsx'
      document.body.appendChild(link)
      link.click()
      link.remove()
    } finally {
      URL.revokeObjectURL(url)
    }
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
