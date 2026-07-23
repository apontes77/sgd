import type { Usuario } from '@/shared/api/types'

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
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

export interface SessaoResponse {
  accessToken: string
  refreshToken: string
  usuario: Usuario
}

export function saveSession(session: SessaoResponse) {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken)
  sessionStorage.setItem(REFRESH_TOKEN_KEY, session.refreshToken)
}

export function clearSession() {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  window.dispatchEvent(new Event('sgd:session-expired'))
}

export function getAccessToken() {
  return sessionStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getRefreshToken() {
  return sessionStorage.getItem(REFRESH_TOKEN_KEY)
}

export function hasStoredSession() {
  return Boolean(getAccessToken() && getRefreshToken())
}

async function refreshSession() {
  if (refreshInFlight) return refreshInFlight
  refreshInFlight = (async () => {
    const refreshToken = getRefreshToken()
    if (!refreshToken) return false
    const response = await fetch(`${API_BASE_URL}/autenticacao/atualizar-token`, {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
    if (!response.ok) return false
    saveSession((await response.json()) as SessaoResponse)
    return true
  })()
    .catch(() => false)
    .finally(() => {
      refreshInFlight = undefined
    })
  return refreshInFlight
}

export async function request<T>(path: string, options: RequestInit = {}, retry = true): Promise<T> {
  const token = getAccessToken()
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
    const problem = (await response.json().catch(() => null)) as { detail?: string; title?: string } | null
    throw new ApiError(problem?.detail ?? problem?.title ?? 'Não foi possível concluir a operação.', response.status)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}
