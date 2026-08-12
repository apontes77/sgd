import { request } from '@/shared/api/httpClient'

export type PapelLideranca = 'DISCIPULADOR' | 'CO_LIDER'
export type SituacaoPresencaLideranca = 'PRESENTE' | 'AUSENTE'

export interface PresencaLideranca {
  usuarioId: number
  nome: string
  papel: PapelLideranca
  situacao: SituacaoPresencaLideranca | null
}

export interface DiscipuladoChamadaLideranca {
  discipuladoId: number
  discipuladoNome: string
  gerenciaNome: string
  observacao: string | null
  presencas: PresencaLideranca[]
}

export interface ChamadaLiderancaResponse {
  id: number | null
  data: string
  observacaoGeral: string | null
  discipulados: DiscipuladoChamadaLideranca[]
}

export interface SalvarChamadaLiderancaRequest {
  data: string
  observacaoGeral: string | null
  discipulados: {
    discipuladoId: number
    observacao: string | null
    presencas: {
      usuarioId: number
      papel: PapelLideranca
      situacao: SituacaoPresencaLideranca
    }[]
  }[]
}

export const liderancaApi = {
  consultar: (data: string) =>
    request<ChamadaLiderancaResponse>(`/chamadas-lideranca?data=${encodeURIComponent(data)}`),
  salvar: (body: SalvarChamadaLiderancaRequest) =>
    request<ChamadaLiderancaResponse>('/chamadas-lideranca', {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
}
