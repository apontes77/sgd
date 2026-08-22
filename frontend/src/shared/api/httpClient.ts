import type { Usuario } from '@/shared/api/types'

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly body: Record<string, unknown> | null = null,
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

function persistToken(key: string, value: string) {
  localStorage.setItem(key, value)
  sessionStorage.removeItem(key)
}

function readToken(key: string) {
  const persisted = localStorage.getItem(key)
  if (persisted) return persisted
  const legacy = sessionStorage.getItem(key)
  if (!legacy) return null
  persistToken(key, legacy)
  return legacy
}

function removeToken(key: string) {
  localStorage.removeItem(key)
  sessionStorage.removeItem(key)
}

export function saveSession(session: SessaoResponse) {
  persistToken(ACCESS_TOKEN_KEY, session.accessToken)
  persistToken(REFRESH_TOKEN_KEY, session.refreshToken)
}

export function clearSession() {
  removeToken(ACCESS_TOKEN_KEY)
  removeToken(REFRESH_TOKEN_KEY)
  window.dispatchEvent(new Event('sgd:session-expired'))
}

export function getAccessToken() {
  return readToken(ACCESS_TOKEN_KEY)
}

export function getRefreshToken() {
  return readToken(REFRESH_TOKEN_KEY)
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
    const problem = (await response.json().catch(() => null)) as Record<string, unknown> | null
    const detail = typeof problem?.detail === 'string' ? problem.detail : undefined
    const title = typeof problem?.title === 'string' ? problem.title : undefined
    throw new ApiError(detail ?? title ?? 'Não foi possível concluir a operação.', response.status, problem)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export async function requestBlob(
  path: string,
  options: RequestInit = {},
  retry = true,
): Promise<{ blob: Blob; filename: string | null }> {
  const token = getAccessToken()
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      Accept: '*/*',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })

  if (response.status === 401 && retry && path !== '/autenticacao/atualizar-token') {
    if (await refreshSession()) return requestBlob(path, options, false)
    clearSession()
  }

  if (!response.ok) {
    const problem = (await response.json().catch(() => null)) as Record<string, unknown> | null
    const detail = typeof problem?.detail === 'string' ? problem.detail : undefined
    const title = typeof problem?.title === 'string' ? problem.title : undefined
    throw new ApiError(detail ?? title ?? 'Não foi possível concluir a operação.', response.status, problem)
  }

  const disposition = response.headers.get('Content-Disposition')
  const filenameMatch = disposition?.match(/filename="?([^";]+)"?/i)
  return { blob: await response.blob(), filename: filenameMatch?.[1] ?? null }
}
