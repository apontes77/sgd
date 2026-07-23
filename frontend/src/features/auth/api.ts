import {
  clearSession,
  getRefreshToken,
  hasStoredSession,
  request,
  saveSession,
  type SessaoResponse,
} from '@/shared/api/httpClient'
import type { Usuario } from '@/shared/api/types'

export const authApi = {
  login: async (email: string, senha: string) => {
    const response = await request<SessaoResponse>(
      '/autenticacao/login',
      { method: 'POST', body: JSON.stringify({ email, senha }) },
      false,
    )
    saveSession(response)
    return response.usuario
  },
  me: () => request<Usuario>('/autenticacao/eu'),
  logout: async () => {
    const refreshToken = getRefreshToken()
    try {
      if (refreshToken)
        await request<void>('/autenticacao/logout', { method: 'POST', body: JSON.stringify({ refreshToken }) }, false)
    } finally {
      clearSession()
    }
  },
  logoutLocal: clearSession,
  solicitarRedefinicaoSenha: (email: string) =>
    request<void>(
      '/autenticacao/esqueci-a-senha',
      {
        method: 'POST',
        body: JSON.stringify({ email }),
      },
      false,
    ),
  redefinirSenha: (token: string, novaSenha: string) =>
    request<void>(
      '/autenticacao/redefinir-senha',
      {
        method: 'POST',
        body: JSON.stringify({ token, novaSenha }),
      },
      false,
    ),
  hasSession: hasStoredSession,
}
