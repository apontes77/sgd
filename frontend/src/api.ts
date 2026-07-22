export type Perfil = 'ADMIN' | 'GERENTE' | 'DISCIPULADOR' | 'CO_LIDER'

export interface Usuario {
  id: number
  nome: string
  email: string
  ativo?: boolean
  senhaDefinida: boolean
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
  ativo?: boolean
}

export interface GerenciaRequest {
  nome: string
  gerenteId: number
}

export interface CriarUsuarioRequest {
  nome: string
  email: string
  perfis: Perfil[]
}

export interface AtualizarUsuarioRequest {
  nome?: string
  perfis?: Perfil[]
  ativo?: boolean
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

export function buildApiBaseUrl(origin?: string) {
  const normalizedOrigin = origin?.trim().replace(/\/+$/, '') ?? ''
  return `${normalizedOrigin}/api/v1`
}

const API_BASE_URL = buildApiBaseUrl(import.meta.env.VITE_API_ORIGIN)
const ACCESS_TOKEN_KEY = 'sgd.access-token'
const REFRESH_TOKEN_KEY = 'sgd.refresh-token'
let refreshInFlight: Promise<boolean> | undefined

interface SessaoResponse {
  accessToken: string
  refreshToken: string
  usuario: Usuario
}

function saveSession(session: SessaoResponse) {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken)
  sessionStorage.setItem(REFRESH_TOKEN_KEY, session.refreshToken)
}

export function clearSession() {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  window.dispatchEvent(new Event('sgd:session-expired'))
}

async function refreshSession() {
  if (refreshInFlight) return refreshInFlight
  refreshInFlight = (async () => {
    const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY)
    if (!refreshToken) return false
    const response = await fetch(`${API_BASE_URL}/autenticacao/atualizar-token`, {
      method: 'POST', headers: { Accept: 'application/json', 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken }),
    })
    if (!response.ok) return false
    saveSession(await response.json() as SessaoResponse)
    return true
  })().catch(() => false).finally(() => { refreshInFlight = undefined })
  return refreshInFlight
}

export async function request<T>(path: string, options: RequestInit = {}, retry = true): Promise<T> {
  const token = sessionStorage.getItem(ACCESS_TOKEN_KEY)
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })

  if (response.status === 401 && retry && path !== '/autenticacao/atualizar-token') {
    if (await refreshSession()) return request<T>(path, options, false)
    clearSession()
  }

  if (!response.ok) {
    const problem = await response.json().catch(() => null) as { detail?: string; title?: string } | null
    throw new ApiError(problem?.detail ?? problem?.title ?? 'Não foi possível concluir a operação.', response.status)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const authApi = {
  login: async (email: string, senha: string) => {
    const response = await request<SessaoResponse>('/autenticacao/login', { method: 'POST', body: JSON.stringify({ email, senha }) }, false)
    saveSession(response)
    return response.usuario
  },
  me: () => request<Usuario>('/autenticacao/eu'),
  logout: async () => {
    const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY)
    try {
      if (refreshToken) await request<void>('/autenticacao/logout', { method: 'POST', body: JSON.stringify({ refreshToken }) }, false)
    } finally {
      clearSession()
    }
  },
  logoutLocal: clearSession,
  solicitarRedefinicaoSenha: (email: string) => request<void>('/autenticacao/esqueci-a-senha', {
    method: 'POST', body: JSON.stringify({ email }),
  }, false),
  redefinirSenha: (token: string, novaSenha: string) => request<void>('/autenticacao/redefinir-senha', {
    method: 'POST', body: JSON.stringify({ token, novaSenha }),
  }, false),
  hasSession: () => Boolean(sessionStorage.getItem(ACCESS_TOKEN_KEY) && sessionStorage.getItem(REFRESH_TOKEN_KEY)),
}

export const passwordRecoveryApi = {
  request: (email: string) => request<void>('/autenticacao/esqueci-a-senha', {
    method: 'POST', body: JSON.stringify({ email }),
  }, false),
  reset: (token: string, novaSenha: string) => request<void>('/autenticacao/redefinir-senha', {
    method: 'POST', body: JSON.stringify({ token, novaSenha }),
  }, false),
}

export const organizationApi = {
  listarUsuarios: (page = 0, size = 100, ativo?: boolean) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (ativo !== undefined) params.set('ativo', String(ativo))
    return request<Pagina<Usuario>>(`/usuarios?${params}`)
  },
  criarUsuario: (body: CriarUsuarioRequest) => request<Usuario>('/usuarios', { method: 'POST', body: JSON.stringify(body) }),
  atualizarUsuario: (id: number, body: AtualizarUsuarioRequest) => request<Usuario>(`/usuarios/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  reenviarDefinicaoSenha: (id: number) => request<void>(`/usuarios/${id}/reenviar-definicao-senha`, { method: 'POST' }),
  listarGerencias: () => request<Pagina<Gerencia>>('/gerencias?page=0&size=100'),
  criarGerencia: (body: GerenciaRequest) => request<Gerencia>('/gerencias', { method: 'POST', body: JSON.stringify(body) }),
  atualizarGerencia: (id: number, body: GerenciaRequest) => request<Gerencia>(`/gerencias/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  listarDiscipulados: (ativo?: boolean) => {
    const params = new URLSearchParams({ page: '0', size: '100' })
    if (ativo !== undefined) params.set('ativo', String(ativo))
    return request<Pagina<Discipulado>>(`/discipulados?${params}`)
  },
  listarDiscipuladosLiderados: (ativo?: boolean) => {
    const params = new URLSearchParams()
    if (ativo !== undefined) params.set('ativo', String(ativo))
    const query = params.size ? `?${params}` : ''
    return request<Discipulado[]>(`/discipulados/liderados${query}`)
  },
  criarDiscipulado: (body: DiscipuladoRequest) => request<Discipulado>('/discipulados', { method: 'POST', body: JSON.stringify(body) }),
  atualizarDiscipulado: (id: number, body: DiscipuladoRequest) => request<Discipulado>(`/discipulados/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  definirCoLideres: (id: number, usuarioIds: number[]) => request<Discipulado>(`/discipulados/${id}/co-lideres`, { method: 'PUT', body: JSON.stringify({ usuarioIds }) }),
}

export const userManagementClient = {
  list: (page: number, size: number, active?: boolean) => organizationApi.listarUsuarios(page, size, active),
  create: (body: CriarUsuarioRequest) => organizationApi.criarUsuario(body),
  update: (id: number, body: AtualizarUsuarioRequest) => organizationApi.atualizarUsuario(id, body),
  resendSetup: (id: number) => organizationApi.reenviarDefinicaoSenha(id),
}
