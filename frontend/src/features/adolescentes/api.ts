import { request } from '@/shared/api/httpClient'
import type { Pagina } from '@/shared/api/types'

export type { Pagina }

export type CategoriaAdolescente = 'DISCIPULO' | 'VISITANTE' | 'DISCIPULO_GOE'

export interface Adolescente {
  id: number
  nome: string
  dataNascimento: string
  telefone?: string
  instagram?: string
  responsavelNome?: string
  responsavelTelefone?: string
  consentimentoEm?: string
  categoria: CategoriaAdolescente
  nomeMae?: string
  telefoneMae?: string
  nomePai?: string
  telefonePai?: string
  estrutura?: string
  motivoAfastamento?: string
  anonimizado: boolean
  discipuladoId: number
  discipuladoNome?: string
  ativo: boolean
}

export interface AdolescenteInput {
  nome: string
  dataNascimento: string
  telefone?: string
  instagram?: string
  responsavelNome: string
  responsavelTelefone?: string
  consentimentoEm: string
  categoria: CategoriaAdolescente
  nomeMae?: string
  telefoneMae?: string
  nomePai?: string
  telefonePai?: string
  estrutura?: string
  motivoAfastamento?: string
  discipuladoId: number
  ativo?: boolean
  dataInicio?: string
}

export interface AlertaGoe {
  adolescenteId: number
  nome: string
  faltas: number
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

export const CATEGORIA_LABEL: Record<CategoriaAdolescente, string> = {
  DISCIPULO: 'Discípulo',
  VISITANTE: 'Visitante',
  DISCIPULO_GOE: 'Discípulo GOE',
}

export const adolescentesApi = {
  listar: (discipuladoId?: number, ativo?: boolean, categoria?: CategoriaAdolescente | CategoriaAdolescente[]) => {
    const params = new URLSearchParams({ page: '0', size: '100' })
    if (discipuladoId) params.set('discipuladoId', String(discipuladoId))
    if (ativo !== undefined) params.set('ativo', String(ativo))
    if (categoria) {
      const categorias = Array.isArray(categoria) ? categoria : [categoria]
      categorias.forEach((c) => params.append('categoria', c))
    }
    return request<Pagina<Adolescente>>(`/adolescentes?${params}`)
  },
  alertasGoe: (discipuladoId: number) =>
    request<AlertaGoe[]>(`/adolescentes/alertas-goe?discipuladoId=${discipuladoId}`),
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
  anonimizar: (id: number) => request<void>(`/adolescentes/${id}/dados-pessoais`, { method: 'DELETE' }),
}
