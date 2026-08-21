import { cleanup, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'

import UserManagement, { type UserManagementClient } from '@/features/users/UserManagement'
import type { Pagina, Usuario } from '@/shared/api/types'
import { render } from '@/test/test-utils'

const emptyPage: Pagina<Usuario> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
const gerente: Usuario = { id: 7, nome: 'Maria Gestora', email: 'maria@sgd.local', ativo: true, perfis: ['GERENTE'] }
const discipulador: Usuario = {
  id: 11,
  nome: 'João Líder',
  email: 'joao@sgd.local',
  ativo: true,
  perfis: ['DISCIPULADOR'],
}

describe('gestão de usuários', () => {
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('abre e cancela o Drawer sem cadastrar', async () => {
    const client = createClient()
    render(<UserManagement client={client} />)
    await waitFor(() => expect(client.list).toHaveBeenCalled())

    await userEvent.click(screen.getByRole('button', { name: 'Novo usuário' }))
    expect(screen.getByRole('heading', { name: 'Novo usuário' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }))

    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Novo usuário' })).not.toBeInTheDocument())
    await waitFor(() => expect(document.querySelector('.MuiDrawer-root')).not.toBeInTheDocument())
    expect(client.create).not.toHaveBeenCalled()
  })

  it('submete um novo usuário pelo Drawer', async () => {
    const client = createClient()
    client.create.mockResolvedValue(gerente)
    render(<UserManagement client={client} />)
    await waitFor(() => expect(client.list).toHaveBeenCalled())

    await userEvent.click(screen.getByRole('button', { name: 'Novo usuário' }))
    await userEvent.type(await screen.findByLabelText(/Nome/), gerente.nome)
    await userEvent.type(screen.getByLabelText(/E-mail/), gerente.email)
    await userEvent.type(screen.getByLabelText(/Senha inicial/), 'senha-segura-123')
    await userEvent.click(screen.getByRole('checkbox', { name: 'Gerente' }))
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar usuário' }))

    await waitFor(() =>
      expect(client.create).toHaveBeenCalledWith({
        nome: gerente.nome,
        email: gerente.email,
        senha: 'senha-segura-123',
        perfis: ['GERENTE'],
      }),
    )
    expect(await screen.findByText('Usuário criado com sucesso.')).toBeInTheDocument()
  })

  it('cadastra um novo usuário já com perfil de discipulador e administrador', async () => {
    const client = createClient()
    client.create.mockResolvedValue({ ...discipulador, perfis: ['DISCIPULADOR', 'ADMIN'] })
    render(<UserManagement client={client} />)
    await waitFor(() => expect(client.list).toHaveBeenCalled())

    await userEvent.click(screen.getByRole('button', { name: 'Novo usuário' }))
    await userEvent.type(await screen.findByLabelText(/Nome/), discipulador.nome)
    await userEvent.type(screen.getByLabelText(/E-mail/), discipulador.email)
    await userEvent.type(screen.getByLabelText(/Senha inicial/), 'senha-segura-123')
    await userEvent.click(screen.getByRole('checkbox', { name: 'Discipulador' }))
    await userEvent.click(screen.getByRole('checkbox', { name: 'Administrador' }))
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar usuário' }))

    await waitFor(() =>
      expect(client.create).toHaveBeenCalledWith({
        nome: discipulador.nome,
        email: discipulador.email,
        senha: 'senha-segura-123',
        perfis: ['DISCIPULADOR', 'ADMIN'],
      }),
    )
  })

  it('exige confirmação antes de inativar uma conta', async () => {
    const client = createClient({ ...emptyPage, content: [gerente], totalElements: 1, totalPages: 1 })
    client.update.mockResolvedValue({ ...gerente, ativo: false })
    render(<UserManagement client={client} />)
    expect(await screen.findByText(gerente.email)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: `Ações de ${gerente.nome}` }))
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Inativar' }))
    expect(screen.getByRole('dialog', { name: 'Inativar usuário?' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(client.update).not.toHaveBeenCalled()

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    await userEvent.click(screen.getByRole('button', { name: `Ações de ${gerente.nome}` }))
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Inativar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar' }))
    await waitFor(() => expect(client.update).toHaveBeenCalledWith(gerente.id, { ativo: false }))
  })

  it('acumula o perfil de administrador em um discipulador existente', async () => {
    const client = createClient({ ...emptyPage, content: [discipulador], totalElements: 1, totalPages: 1 })
    client.update.mockResolvedValue({ ...discipulador, perfis: ['DISCIPULADOR', 'ADMIN'] })
    render(<UserManagement client={client} />)
    expect(await screen.findByText(discipulador.email)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: `Ações de ${discipulador.nome}` }))
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Editar' }))
    expect(await screen.findByRole('heading', { name: 'Editar usuário' })).toBeInTheDocument()
    expect(screen.getByLabelText(/E-mail/)).toBeDisabled()
    expect(screen.queryByLabelText(/Senha inicial/)).not.toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Discipulador' })).toBeChecked()

    await userEvent.click(screen.getByRole('checkbox', { name: 'Administrador' }))
    await userEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    await waitFor(() =>
      expect(client.update).toHaveBeenCalledWith(discipulador.id, {
        nome: discipulador.nome,
        perfis: ['DISCIPULADOR', 'ADMIN'],
      }),
    )
    expect(await screen.findByText('Usuário atualizado com sucesso.')).toBeInTheDocument()
  })

  it('busca usuários por trecho do nome', async () => {
    const client = createClient({ ...emptyPage, content: [discipulador], totalElements: 1, totalPages: 1 })
    render(<UserManagement client={client} />)
    await waitFor(() => expect(client.list).toHaveBeenCalledWith(0, 20, undefined, undefined))

    await userEvent.type(screen.getByLabelText('Busca'), 'João')
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    await waitFor(() => expect(client.list).toHaveBeenCalledWith(0, 20, undefined, 'João'))
  })
})

function createClient(page = emptyPage) {
  return {
    list: vi.fn().mockResolvedValue(page),
    create: vi.fn(),
    update: vi.fn(),
  } satisfies {
    [Key in keyof UserManagementClient]: ReturnType<typeof vi.fn>
  }
}
