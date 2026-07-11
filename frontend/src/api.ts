export type Perfil = 'ADMIN' | 'GERENTE' | 'DISCIPULADOR' | 'CO_LIDER'

export interface Usuario {
  id: number
  nome: string
  email: string
  ativo?: boolean
  perfis: Perfil[]
}

export interface Pagina<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Gerencia {
  id: number
  nome: string
  gerenteId: number
}

export interface GerenciaRequest {
  nome: string
  gerenteId: number
}

export type SexoDiscipulado = 'MASCULINO' | 'FEMININO'

export interface Discipulado {
  id: number
  nome: string
  sexo: SexoDiscipulado
  gerenciaId: number
  discipuladorId: number
  ativo?: boolean
  coLideres: Usuario[]
}

export interface DiscipuladoRequest {
  nome: string
  sexo: SexoDiscipulado
  gerenciaId: number
  discipuladorId: number
  ativo?: boolean
}

export class ApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message)
  }
}

const API_BASE_URL = '/api/v1'

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = sessionStorage.getItem('sgd.access-token')
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })

  if (!response.ok) {
    const problem = await response.json().catch(() => null) as { detail?: string; title?: string } | null
    throw new ApiError(problem?.detail ?? problem?.title ?? 'Não foi possível concluir a operação.', response.status)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const organizationApi = {
  listarUsuarios: () => request<Pagina<Usuario>>('/usuarios?page=0&size=100'),
  listarGerencias: () => request<Pagina<Gerencia>>('/gerencias?page=0&size=100'),
  criarGerencia: (body: GerenciaRequest) => request<Gerencia>('/gerencias', { method: 'POST', body: JSON.stringify(body) }),
  atualizarGerencia: (id: number, body: GerenciaRequest) => request<Gerencia>(`/gerencias/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  listarDiscipulados: () => request<Pagina<Discipulado>>('/discipulados?page=0&size=100'),
  criarDiscipulado: (body: DiscipuladoRequest) => request<Discipulado>('/discipulados', { method: 'POST', body: JSON.stringify(body) }),
  atualizarDiscipulado: (id: number, body: DiscipuladoRequest) => request<Discipulado>(`/discipulados/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  definirCoLideres: (id: number, usuarioIds: number[]) => request<Discipulado>(`/discipulados/${id}/co-lideres`, { method: 'PUT', body: JSON.stringify({ usuarioIds }) }),
}
