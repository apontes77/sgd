import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

describe('entrada e redefinição de senha', () => {
  beforeEach(() => {
    sessionStorage.clear()
    window.history.replaceState(null, '', '/')
  })

  afterEach(() => {
    cleanup(); vi.restoreAllMocks(); sessionStorage.clear(); window.history.replaceState(null, '', '/')
  })

  it('abre a recuperação a partir do login e envia a solicitação', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 204 }))
    render(<App />)

    await userEvent.click(screen.getByRole('button', { name: 'Esqueci minha senha' }))
    await userEvent.type(screen.getByLabelText(/E-mail/), 'pessoa@sgd.local')
    await userEvent.click(screen.getByRole('button', { name: 'Solicitar redefinição' }))

    expect(await screen.findByText(/Se o e-mail estiver cadastrado e ativo/)).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/autenticacao/esqueci-a-senha', expect.objectContaining({
      method: 'POST', body: JSON.stringify({ email: 'pessoa@sgd.local' }),
    }))
  })

  it('captura o token do fragmento, remove a URL sensível e retorna ao login após sucesso', async () => {
    window.history.replaceState(null, '', '/#/redefinir-senha/token-seguro')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 204 }))
    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Definir nova senha' })).toBeInTheDocument()
    await waitFor(() => expect(window.location.hash).toBe(''))
    await userEvent.type(screen.getByLabelText(/Nova senha/), 'uma-senha-segura')
    await userEvent.type(screen.getByLabelText(/Confirmar senha/), 'uma-senha-segura')
    await userEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }))

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/autenticacao/redefinir-senha', expect.objectContaining({
      method: 'POST', body: JSON.stringify({ token: 'token-seguro', novaSenha: 'uma-senha-segura' }),
    }))
    expect(await screen.findByText('Senha definida com sucesso. Entre com a sua nova senha.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeInTheDocument()
  })
})
