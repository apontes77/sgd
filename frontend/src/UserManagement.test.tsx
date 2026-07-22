import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Pagina, Usuario } from './api'
import UserManagement, { type UserManagementClient } from './UserManagement'

const emptyPage: Pagina<Usuario> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
const gerente: Usuario = { id: 7, nome: 'Maria Gestora', email: 'maria@sgd.local', ativo: true, senhaDefinida: true, perfis: ['GERENTE'] }

describe('gestão de usuários', () => {
  afterEach(() => { cleanup(); vi.restoreAllMocks() })

  it('abre e cancela o Drawer sem cadastrar', async () => {
    const client = createClient()
    render(<UserManagement client={client} />)
    await waitFor(() => expect(client.list).toHaveBeenCalled())

    await userEvent.click(screen.getByRole('button', { name: 'Novo usuário' }))
    expect(screen.getByRole('heading', { name: 'Novo usuário' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }))

    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Novo usuário' })).not.toBeInTheDocument())
    expect(client.create).not.toHaveBeenCalled()
  })

  it('submete um novo usuário pelo Drawer sem senha inicial', async () => {
    const client = createClient()
    client.create.mockResolvedValue({ ...gerente, senhaDefinida: false })
    render(<UserManagement client={client} />)
    await waitFor(() => expect(client.list).toHaveBeenCalled())

    await userEvent.click(screen.getByRole('button', { name: 'Novo usuário' }))
    await userEvent.type(await screen.findByLabelText(/Nome/), gerente.nome)
    await userEvent.type(screen.getByLabelText(/E-mail/), gerente.email)
    expect(screen.queryByLabelText(/Senha inicial/)).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('checkbox', { name: 'Gerente' }))
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar usuário' }))

    await waitFor(() => expect(client.create).toHaveBeenCalledWith({
      nome: gerente.nome,
      email: gerente.email,
      perfis: ['GERENTE'],
    }))
    expect(await screen.findByText(/Enviamos um convite/)).toBeInTheDocument()
  })

  it('exibe convite pendente e permite reenviar', async () => {
    const pendingUser: Usuario = {
      id: 2, nome: 'Nova Pessoa', email: 'nova@sgd.local', ativo: true, senhaDefinida: false, perfis: ['ADMIN'],
    }
    const client = createClient({ content: [pendingUser], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    client.resendSetup = vi.fn().mockResolvedValue(undefined)
    render(<UserManagement client={client} />)

    expect(await screen.findByText('Aguardando definição de senha')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Reenviar convite' }))
    await waitFor(() => expect(client.resendSetup).toHaveBeenCalledWith(2))
    expect(await screen.findByText(/Convite de definição de senha reenviado/)).toBeInTheDocument()
  })

  it('exige confirmação antes de inativar uma conta', async () => {
    const client = createClient({ ...emptyPage, content: [gerente], totalElements: 1, totalPages: 1 })
    client.update.mockResolvedValue({ ...gerente, ativo: false })
    render(<UserManagement client={client} />)
    expect(await screen.findByText(gerente.email)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Inativar' }))
    expect(screen.getByRole('dialog', { name: 'Inativar usuário?' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(client.update).not.toHaveBeenCalled()

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    await userEvent.click(screen.getByRole('button', { name: 'Inativar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar' }))
    await waitFor(() => expect(client.update).toHaveBeenCalledWith(gerente.id, { ativo: false }))
  })
})

function createClient(page = emptyPage) {
  return {
    list: vi.fn().mockResolvedValue(page),
    create: vi.fn(),
    update: vi.fn(),
    resendSetup: vi.fn(),
  } satisfies {
    [Key in keyof UserManagementClient]: ReturnType<typeof vi.fn>
  }
}
