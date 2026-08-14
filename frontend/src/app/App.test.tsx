import { cleanup, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import App from '@/app/App'
import { authApi } from '@/features/auth/api'
import { render } from '@/test/test-utils'

describe('login', () => {
  beforeEach(() => {
    sessionStorage.clear()
    window.history.replaceState({}, '', '/')
    vi.restoreAllMocks()
  })
  afterEach(cleanup)

  it('alterna a visibilidade da senha pelo botao do olho', async () => {
    render(<App />)
    const senha = screen.getByLabelText(/^Senha/)
    expect(senha).toHaveAttribute('type', 'password')

    await userEvent.click(screen.getByRole('button', { name: 'Mostrar senha' }))
    expect(senha).toHaveAttribute('type', 'text')
    expect(screen.getByRole('button', { name: 'Ocultar senha' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Ocultar senha' }))
    expect(senha).toHaveAttribute('type', 'password')
  })
})

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

    await userEvent.type(screen.getByLabelText(/Nova senha/), 'Senha1!')
    await userEvent.type(screen.getByLabelText(/Confirmar senha/), 'Senha1!')
    await userEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }))

    expect(authApi.redefinirSenha).toHaveBeenCalledWith('token-da-url', 'Senha1!')
    expect(window.location.pathname).toBe('/')
    expect(window.location.search).toBe('?senhaRedefinida=1')
    expect(sessionStorage.getItem('sgd.access-token')).toBeNull()
    expect(sessionStorage.getItem('sgd.refresh-token')).toBeNull()
    expect(await screen.findByRole('heading', { name: 'Bem-vindo de volta' })).toBeInTheDocument()
    expect(screen.getByText('Senha redefinida com sucesso. Faça login.')).toBeInTheDocument()
  })

  it('mostra erro quando o e-mail de recuperacao nao esta cadastrado', async () => {
    vi.spyOn(authApi, 'solicitarRedefinicaoSenha').mockRejectedValue(new Error('E-mail não cadastrado.'))
    render(<App />)
    await userEvent.click(screen.getByRole('button', { name: 'Esqueci minha senha' }))
    await userEvent.type(screen.getByLabelText(/E-mail/), 'ausente@example.com')
    await userEvent.click(screen.getByRole('button', { name: 'Solicitar redefinição' }))

    expect(await screen.findByText('E-mail não cadastrado.')).toBeInTheDocument()
    expect(screen.queryByText('Enviamos as instruções para o seu e-mail.')).not.toBeInTheDocument()
  })

  it('mostra sucesso quando a solicitacao de recuperacao e aceita', async () => {
    vi.spyOn(authApi, 'solicitarRedefinicaoSenha').mockResolvedValue()
    render(<App />)
    await userEvent.click(screen.getByRole('button', { name: 'Esqueci minha senha' }))
    await userEvent.type(screen.getByLabelText(/E-mail/), 'user@example.com')
    await userEvent.click(screen.getByRole('button', { name: 'Solicitar redefinição' }))

    expect(await screen.findByRole('heading', { name: 'Bem-vindo de volta' })).toBeInTheDocument()
    expect(window.location.pathname).toBe('/')
    expect(window.location.search).toBe('?recuperacaoEnviada=1')
    expect(screen.getByText('Enviamos as instruções para o seu e-mail.')).toBeInTheDocument()
  })
})
