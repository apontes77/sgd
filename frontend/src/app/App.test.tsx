import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import App from '@/app/App'
import { authApi } from '@/features/auth/api'

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

  it('consome o token da URL e retorna ao login depois do sucesso', async () => {
    window.history.replaceState({}, '', '/redefinir-senha?token=token-da-url')
    expect(window.location.pathname).toBe('/redefinir-senha')
    sessionStorage.setItem('sgd.access-token', 'sessao-antiga')
    sessionStorage.setItem('sgd.refresh-token', 'refresh-antigo')
    vi.spyOn(authApi, 'redefinirSenha').mockResolvedValue()
    render(<App />)

    await userEvent.type(screen.getByLabelText(/Nova senha/), 'senha-nova-com-12')
    await userEvent.type(screen.getByLabelText(/Confirmar senha/), 'senha-nova-com-12')
    await userEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }))

    expect(authApi.redefinirSenha).toHaveBeenCalledWith('token-da-url', 'senha-nova-com-12')
    expect(window.location.pathname).toBe('/')
    expect(window.location.search).toBe('')
    expect(sessionStorage.getItem('sgd.access-token')).toBeNull()
    expect(sessionStorage.getItem('sgd.refresh-token')).toBeNull()
    expect(await screen.findByRole('heading', { name: 'Bem-vindo de volta' })).toBeInTheDocument()
  })
})
