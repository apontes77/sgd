import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from './api'

describe('cliente HTTP', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.restoreAllMocks()
  })

  it('renova uma sessão expirada e repete a requisição uma vez', async () => {
    sessionStorage.setItem('sgd.access-token', 'expirado')
    sessionStorage.setItem('sgd.refresh-token', 'refresh-antigo')
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ accessToken: 'novo', refreshToken: 'refresh-novo' }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ ok: true }), { status: 200, headers: { 'Content-Type': 'application/json' } }))

    await expect(request<{ ok: boolean }>('/teste')).resolves.toEqual({ ok: true })
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(sessionStorage.getItem('sgd.access-token')).toBe('novo')
  })
})
