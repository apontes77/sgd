import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ForgotPassword, PasswordRecoveryClient, ResetPassword } from './PasswordRecovery'

afterEach(cleanup)

describe('recuperação de senha', () => {
  it('impede envio duplicado enquanto a solicitação está pendente', async () => {
    let finish!: () => void
    const client = recoveryClient()
    client.request = vi.fn(() => new Promise<void>((resolve) => { finish = resolve }))
    render(<ForgotPassword client={client} onBack={() => undefined} />)

    await userEvent.type(screen.getByLabelText(/E-mail/), 'pessoa@sgd.local')
    await userEvent.click(screen.getByRole('button', { name: 'Solicitar redefinição' }))

    expect(client.request).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: 'Enviando...' })).toBeDisabled()
    fireEvent.submit(screen.getByRole('button', { name: 'Enviando...' }).closest('form')!)
    expect(client.request).toHaveBeenCalledTimes(1)

    finish()
    expect(await screen.findByText(/Se o e-mail estiver cadastrado e ativo/)).toBeInTheDocument()
  })

  it('explica que o fluxo também serve para quem ainda não definiu a senha', () => {
    render(<ForgotPassword client={recoveryClient()} onBack={() => undefined} />)
    expect(screen.getByText(/ainda não definiu a senha do convite inicial/)).toBeInTheDocument()
  })

  it('valida confirmação e tamanho UTF-8 antes de chamar a API', async () => {
    const client = recoveryClient()
    render(<ResetPassword client={client} token="token" onSuccess={() => undefined} />)

    await userEvent.type(screen.getByLabelText(/Nova senha/), 'senhas-diferentes')
    await userEvent.type(screen.getByLabelText(/Confirmar senha/), 'outra-senha-segura')
    await userEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }))
    expect(screen.getByText('As senhas não coincidem.')).toBeInTheDocument()

    await userEvent.clear(screen.getByLabelText(/Nova senha/))
    await userEvent.clear(screen.getByLabelText(/Confirmar senha/))
    await userEvent.type(screen.getByLabelText(/Nova senha/), 'ááááá')
    await userEvent.type(screen.getByLabelText(/Confirmar senha/), 'ááááá')
    await userEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }))
    expect(screen.getByText('Use pelo menos 12 caracteres (limite técnico de 72 bytes UTF-8).')).toBeInTheDocument()
    expect(client.reset).not.toHaveBeenCalled()
  })

  it('não expõe detalhes do backend para token inválido ou expirado', async () => {
    const client = recoveryClient()
    client.reset = vi.fn().mockRejectedValue(new Error('registro interno não encontrado'))
    render(<ResetPassword client={client} token="token" onSuccess={() => undefined} />)

    await userEvent.type(screen.getByLabelText(/Nova senha/), 'uma-senha-segura')
    await userEvent.type(screen.getByLabelText(/Confirmar senha/), 'uma-senha-segura')
    await userEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }))

    expect(await screen.findByText(/link de redefinição é inválido ou expirou/)).toBeInTheDocument()
    expect(screen.queryByText(/registro interno/)).not.toBeInTheDocument()
  })
})

function recoveryClient(): PasswordRecoveryClient {
  return { request: vi.fn().mockResolvedValue(undefined), reset: vi.fn().mockResolvedValue(undefined) }
}
