import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Pagina, Usuario } from './api'
import UserManagement, { UserManagementClient } from './UserManagement'

afterEach(cleanup)

describe('gestão de usuários sem senha inicial', () => {
  it('exibe convite pendente e cria usuário sem enviar senha', async () => {
    const pendingUser: Usuario = {
      id: 2, nome: 'Nova Pessoa', email: 'nova@sgd.local', ativo: true, senhaDefinida: false, perfis: ['ADMIN'],
    }
    const page: Pagina<Usuario> = { content: [pendingUser], page: 0, size: 20, totalElements: 1, totalPages: 1 }
    const client: UserManagementClient = {
      list: vi.fn().mockResolvedValue(page),
      create: vi.fn().mockResolvedValue(pendingUser),
      update: vi.fn().mockResolvedValue(pendingUser),
    }
    render(<UserManagement client={client} />)

    expect(await screen.findByText('Aguardando definição de senha')).toBeInTheDocument()
    expect(screen.queryByLabelText(/Senha inicial/)).not.toBeInTheDocument()
    await userEvent.type(screen.getByLabelText(/Nome/), 'Nova Pessoa')
    await userEvent.type(screen.getByLabelText(/E-mail/), 'nova@sgd.local')
    await userEvent.click(screen.getByRole('checkbox', { name: 'ADMIN' }))
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar usuário' }))

    await waitFor(() => expect(client.create).toHaveBeenCalledWith({
      nome: 'Nova Pessoa', email: 'nova@sgd.local', perfis: ['ADMIN'],
    }))
    expect(await screen.findByText(/Enviamos um convite/)).toBeInTheDocument()
  })
})
