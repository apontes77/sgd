import { beforeEach, describe, expect, it, vi } from 'vitest'

import { authApi } from '@/features/auth/api'
import { request } from '@/shared/api/httpClient'

describe('cliente HTTP', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.restoreAllMocks()
  })

  it('envia o campo senha no login e armazena a sessão retornada', async () => {
    const usuario = { id: 1, nome: 'Admin', email: 'admin@sgd.local', ativo: true, perfis: ['ADMIN'] as const }
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          accessToken: 'access',
          refreshToken: 'refresh',
          usuario,
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    )

    await expect(authApi.login('admin@sgd.local', 'senha-segura')).resolves.toEqual(usuario)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/autenticacao/login',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ email: 'admin@sgd.local', senha: 'senha-segura' }),
      }),
    )
    expect(sessionStorage.getItem('sgd.access-token')).toBe('access')
    expect(sessionStorage.getItem('sgd.refresh-token')).toBe('refresh')
  })

  it('renova uma sessão expirada e repete a requisição uma vez', async () => {
    sessionStorage.setItem('sgd.access-token', 'expirado')
    sessionStorage.setItem('sgd.refresh-token', 'refresh-antigo')
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ accessToken: 'novo', refreshToken: 'refresh-novo' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ ok: true }), { status: 200, headers: { 'Content-Type': 'application/json' } }),
      )

    await expect(request<{ ok: boolean }>('/teste')).resolves.toEqual({ ok: true })
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(sessionStorage.getItem('sgd.access-token')).toBe('novo')
  })

  it('revoga o refresh token no logout e sempre limpa a sessão local', async () => {
    sessionStorage.setItem('sgd.access-token', 'access')
    sessionStorage.setItem('sgd.refresh-token', 'refresh')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 204 }))

    await authApi.logout()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/autenticacao/logout',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ refreshToken: 'refresh' }),
      }),
    )
    expect(sessionStorage.getItem('sgd.access-token')).toBeNull()
    expect(sessionStorage.getItem('sgd.refresh-token')).toBeNull()
  })

  it('limpa a sessão local mesmo quando o backend de logout falha', async () => {
    sessionStorage.setItem('sgd.access-token', 'access')
    sessionStorage.setItem('sgd.refresh-token', 'refresh')
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('indisponível'))

    await expect(authApi.logout()).rejects.toThrow('indisponível')
    expect(sessionStorage.getItem('sgd.access-token')).toBeNull()
    expect(sessionStorage.getItem('sgd.refresh-token')).toBeNull()
  })
})
