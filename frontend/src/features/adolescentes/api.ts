import { request } from '@/shared/api/httpClient'
import type { Pagina } from '@/shared/api/types'

export type { Pagina }
export interface Adolescente {
  id: number
  nome: string
  dataNascimento: string
  telefone?: string
  instagram?: string
  discipuladoId: number
  ativo: boolean
}
export interface AdolescenteInput {
  nome: string
  dataNascimento: string
  telefone?: string
  instagram?: string
  discipuladoId: number
  ativo?: boolean
  dataInicio?: string
}
export interface Vinculo {
  id: number
  adolescenteId: number
  discipuladoId: number
  dataInicio: string
  dataFim?: string
  ativo: boolean
}
export interface DiscipuladoResumo {
  id: number
  nome: string
  ativo?: boolean
}

export const adolescentesApi = {
  listar: (discipuladoId?: number, ativo?: boolean) => {
    const params = new URLSearchParams({ page: '0', size: '100' })
    if (discipuladoId) params.set('discipuladoId', String(discipuladoId))
    if (ativo !== undefined) params.set('ativo', String(ativo))
    return request<Pagina<Adolescente>>(`/adolescentes?${params}`)
  },
  listarDiscipulados: () => request<Pagina<DiscipuladoResumo>>('/discipulados?ativo=true&page=0&size=100'),
  criar: (body: AdolescenteInput) =>
    request<Adolescente>('/adolescentes', { method: 'POST', body: JSON.stringify(body) }),
  atualizar: (id: number, body: AdolescenteInput) =>
    request<Adolescente>(`/adolescentes/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  transferir: (id: number, discipuladoId: number, dataInicio: string) =>
    request<Vinculo>(`/adolescentes/${id}/vinculos`, {
      method: 'POST',
      body: JSON.stringify({ discipuladoId, dataInicio }),
    }),
}
