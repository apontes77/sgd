import { beforeEach, describe, expect, it, vi } from 'vitest'

import { authApi } from '@/features/auth/api'

describe('API de recuperacao de senha', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.restoreAllMocks()
  })

  it('solicita redefinicao sem depender de uma sessao', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 204 }))

    await authApi.solicitarRedefinicaoSenha('lider@sgd.local')

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/autenticacao/esqueci-a-senha',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ email: 'lider@sgd.local' }),
      }),
    )
  })

  it('envia token e nova senha para concluir a redefinicao', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 204 }))

    await authApi.redefinirSenha('token-unico', 'nova-senha-segura')

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/autenticacao/redefinir-senha',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ token: 'token-unico', novaSenha: 'nova-senha-segura' }),
      }),
    )
  })
})
