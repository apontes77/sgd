import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import AuthenticatedApp from './AuthenticatedApp'
import type { Usuario } from './api'

const emptyPage = { content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }

describe('navegação autenticada', () => {
  beforeEach(() => {
    sessionStorage.setItem('sgd.access-token', 'token')
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(emptyPage), { status: 200, headers: { 'Content-Type': 'application/json' } }))
  })
  afterEach(() => { cleanup(); vi.restoreAllMocks(); sessionStorage.clear() })

  it('oferece todos os módulos administrativos ao ADMIN', () => {
    render(<AuthenticatedApp currentUser={user(['ADMIN'])} onLogout={() => undefined} />)
    expect(screen.getByRole('tab', { name: 'Estrutura' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Usuários' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Adolescentes' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Frequência' })).toBeInTheDocument()
  })

  it('limita a navegação do GERENTE aos adolescentes', () => {
    render(<AuthenticatedApp currentUser={user(['GERENTE'])} onLogout={() => undefined} />)
    expect(screen.getByRole('tab', { name: 'Adolescentes' })).toBeInTheDocument()
    expect(screen.queryByRole('tab', { name: 'Usuários' })).not.toBeInTheDocument()
    expect(screen.queryByRole('tab', { name: 'Frequência' })).not.toBeInTheDocument()
  })

  it('lista usuários já cadastrados na área administrativa', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(async (input) => {
      const url = String(input)
      const page = url.includes('/usuarios') ? { ...emptyPage, content: [user(['GERENTE'])], totalElements: 1, totalPages: 1 } : emptyPage
      return new Response(JSON.stringify(page), { status: 200, headers: { 'Content-Type': 'application/json' } })
    })
    render(<AuthenticatedApp currentUser={user(['ADMIN'])} onLogout={() => undefined} />)

    await userEvent.click(screen.getByRole('tab', { name: 'Usuários' }))

    expect(await screen.findByText('usuario@sgd.local')).toBeInTheDocument()
    expect(screen.getAllByText('GERENTE')).not.toHaveLength(0)
  })
})

function user(perfis: Usuario['perfis']): Usuario {
  return { id: 1, nome: 'Usuário', email: 'usuario@sgd.local', ativo: true, perfis }
}
