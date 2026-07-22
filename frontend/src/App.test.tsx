import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { authApi } from './api'

describe('recuperacao publica de senha', () => {
  beforeEach(() => {
    sessionStorage.clear()
    window.history.replaceState({}, '', '/')
    vi.restoreAllMocks()
  })
  afterEach(cleanup)

  it('abre a solicitacao a partir do login e volta pelo historico', async () => {
    render(<App />)
    await userEvent.click(screen.getByRole('button', { name: 'Esqueci minha senha' }))

    expect(screen.getByRole('heading', { name: 'Recuperar senha' })).toBeInTheDocument()
    expect(window.location.pathname).toBe('/esqueci-senha')

    window.history.pushState({}, '', '/')
    window.dispatchEvent(new PopStateEvent('popstate'))
    expect(await screen.findByRole('heading', { name: 'Bem-vindo de volta' })).toBeInTheDocument()
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
    expect(screen.getByRole('heading', { name: 'Bem-vindo de volta' })).toBeInTheDocument()
  })

  it('ainda aceita token por query string legado', async () => {
    window.history.replaceState({}, '', '/redefinir-senha?token=token-da-url')
    vi.spyOn(authApi, 'redefinirSenha').mockResolvedValue()
    render(<App />)

    await userEvent.type(screen.getByLabelText(/Nova senha/), 'senha-nova-com-12')
    await userEvent.type(screen.getByLabelText(/Confirmar senha/), 'senha-nova-com-12')
    await userEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }))

    expect(authApi.redefinirSenha).toHaveBeenCalledWith('token-da-url', 'senha-nova-com-12')
    expect(await screen.findByRole('heading', { name: 'Bem-vindo de volta' })).toBeInTheDocument()
  })
})
