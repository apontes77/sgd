import { request } from '@/shared/api/httpClient'
import type { Pagina } from '@/shared/api/types'

export type SituacaoEncontro = 'REALIZADO' | 'NAO_REALIZADO'
export type SituacaoFrequencia = 'PRESENTE' | 'AUSENTE'
export interface Encontro {
  id: number
  discipuladoId: number
  data: string
  situacao: SituacaoEncontro
  justificativa: string | null
  criadoEm: string
}
export interface Frequencia {
  id: number
  encontroId: number
  adolescenteId: number
  adolescenteNome: string
  situacao: SituacaoFrequencia
  registradaEm: string
}
export interface AdolescenteResumo {
  id: number
  nome: string
}

export const frequenciaApi = {
  listarEncontros: (discipuladoId: number, inicio?: string, fim?: string) => {
    const query = new URLSearchParams({ discipuladoId: String(discipuladoId) })
    if (inicio) query.set('dataInicio', inicio)
    if (fim) query.set('dataFim', fim)
    return request<Encontro[]>(`/encontros?${query}`)
  },
  criarEncontro: (body: { discipuladoId: number; data: string; situacao: SituacaoEncontro; justificativa?: string }) =>
    request<Encontro>('/encontros', { method: 'POST', body: JSON.stringify(body) }),
  atualizarEncontro: (id: number, body: { data?: string; situacao?: SituacaoEncontro; justificativa?: string }) =>
    request<Encontro>(`/encontros/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  listarChamada: (id: number) => request<Frequencia[]>(`/encontros/${id}/frequencias`),
  salvarChamada: (id: number, frequencias: Array<{ adolescenteId: number; situacao: SituacaoFrequencia }>) =>
    request<Frequencia[]>(`/encontros/${id}/frequencias`, { method: 'PUT', body: JSON.stringify({ frequencias }) }),
  salvarVisitantes: (id: number, quantidade: number) =>
    request<{ quantidade: number }>(`/encontros/${id}/visitantes`, {
      method: 'PUT',
      body: JSON.stringify({ quantidade }),
    }),
  obterVisitantes: (id: number) => request<{ quantidade: number }>(`/encontros/${id}/visitantes`),
  listarAdolescentes: (discipuladoId: number) =>
    request<Pagina<AdolescenteResumo>>(`/adolescentes?discipuladoId=${discipuladoId}&ativo=true&page=0&size=100`),
}
