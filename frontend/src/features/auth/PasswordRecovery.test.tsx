import { cleanup, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { ForgotPassword, type PasswordRecoveryClient, ResetPassword } from '@/features/auth/PasswordRecovery'
import { render } from '@/test/test-utils'

function clientStub(overrides: Partial<PasswordRecoveryClient> = {}): PasswordRecoveryClient {
  return {
    request: vi.fn().mockResolvedValue(undefined),
    reset: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  }
}

afterEach(cleanup)

describe('ForgotPassword', () => {
  it('redireciona ao login depois de solicitar o e-mail', async () => {
    const client = clientStub()
    const onSuccess = vi.fn()
    render(<ForgotPassword client={client} onBack={() => undefined} onSuccess={onSuccess} />)

    await userEvent.type(screen.getByLabelText(/E-mail/), 'user@example.com')
    await userEvent.click(screen.getByRole('button', { name: 'Solicitar redefinição' }))

    expect(client.request).toHaveBeenCalledWith('user@example.com')
    expect(onSuccess).toHaveBeenCalled()
  })
})

describe('ResetPassword', () => {
  it('mostra o aviso dinamico dos requisitos conforme a senha e digitada', async () => {
    render(<ResetPassword client={clientStub()} token="token" onSuccess={() => undefined} />)

    expect(screen.getByText('Pelo menos 6 caracteres')).toBeInTheDocument()
    expect(screen.getByText('1 letra maiúscula')).toBeInTheDocument()
    expect(screen.getByText('1 letra minúscula')).toBeInTheDocument()
    expect(screen.getByText('1 caractere especial')).toBeInTheDocument()
    expect(screen.getByText('1 número')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Redefinir senha' })).toBeDisabled()

    await userEvent.type(screen.getByLabelText(/Nova senha/), 'Senha1!')
    await userEvent.type(screen.getByLabelText(/Confirmar senha/), 'Senha1!')

    expect(screen.getByRole('button', { name: 'Redefinir senha' })).toBeEnabled()
  })

  it('mantem o botao desabilitado enquanto faltar um requisito', async () => {
    render(<ResetPassword client={clientStub()} token="token" onSuccess={() => undefined} />)

    await userEvent.type(screen.getByLabelText(/Nova senha/), 'senha1!')
    await userEvent.type(screen.getByLabelText(/Confirmar senha/), 'senha1!')

    expect(screen.getByRole('button', { name: 'Redefinir senha' })).toBeDisabled()
  })
})
