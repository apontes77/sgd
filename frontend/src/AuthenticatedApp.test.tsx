import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import AuthenticatedApp from './AuthenticatedApp'
import type { Usuario } from './api'

const emptyPage = { content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }
const emptyDashboard = { dataInicio: '2026-01-01', dataFim: '2026-07-01', resumo: { encontrosRealizados: 0, presentes: 0, ausentes: 0, visitantes: 0, percentualPresenca: 0 }, evolucao: [], gerencias: [], sexos: [{ sexo: 'MASCULINO', presentes: 0, ausentes: 0, percentualPresenca: 0 }, { sexo: 'FEMININO', presentes: 0, ausentes: 0, percentualPresenca: 0 }] }
const emptyManagerDashboard = { dataInicio: '2026-01-01', dataFim: '2026-07-01', gerencia: { id: 1, nome: 'Centro' }, resumo: emptyDashboard.resumo, evolucao: [], discipulados: [] }
const emptyLeaderDashboard = { dataInicio: '2026-01-01', dataFim: '2026-07-01', discipulado: { id: 1, nome: 'Discipulado A', sexo: 'MASCULINO', ativo: true }, resumo: emptyDashboard.resumo, evolucao: [] }

vi.mock('echarts-for-react', () => ({ default: () => <div data-testid="grafico" /> }))

describe('navegação autenticada', () => {
  beforeEach(() => {
    sessionStorage.setItem('sgd.access-token', 'token')
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => { const url = String(input); return new Response(JSON.stringify(url.includes('/painel/admin') ? emptyDashboard : url.includes('/painel/gerencia') ? emptyManagerDashboard : url.includes('/painel/lider') ? emptyLeaderDashboard : emptyPage), { status: 200, headers: { 'Content-Type': 'application/json' } }) })
  })
  afterEach(() => { cleanup(); vi.restoreAllMocks(); sessionStorage.clear() })

  it('oferece todos os módulos administrativos ao ADMIN', () => {
    render(<AuthenticatedApp currentUser={user(['ADMIN'])} onLogout={() => undefined} />)
    expect(screen.getByRole('tab', { name: 'Painel' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Painel' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: 'Estrutura' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Usuários' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Adolescentes' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Registrar frequência' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Relatórios' })).toBeInTheDocument()
  })

  it('usa o gênero correto no botão de criação da estrutura', async () => {
    render(<AuthenticatedApp currentUser={user(['ADMIN'])} onLogout={() => undefined} />)
    await userEvent.click(screen.getByRole('tab', { name: 'Estrutura' }))
    expect(await screen.findByRole('button', { name: 'Nova gerência' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('tab', { name: 'Discipulados' }))
    expect(screen.getByRole('button', { name: 'Novo discipulado' })).toBeInTheDocument()
  })

  it('oferece o painel gerencial ao GERENTE', () => {
    render(<AuthenticatedApp currentUser={user(['GERENTE'])} onLogout={() => undefined} />)
    expect(screen.getByRole('tab', { name: 'Minha gerência' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Minha gerência' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: 'Adolescentes' })).toBeInTheDocument()
    expect(screen.queryByRole('tab', { name: 'Usuários' })).not.toBeInTheDocument()
    expect(screen.queryByRole('tab', { name: 'Frequência' })).not.toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Relatórios' })).toBeInTheDocument()
  })

  it('mantém os painéis administrativo e gerencial para perfil combinado', () => {
    render(<AuthenticatedApp currentUser={user(['ADMIN', 'GERENTE'])} onLogout={() => undefined} />)
    expect(screen.getByRole('tab', { name: 'Painel' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Minha gerência' })).toBeInTheDocument()
  })

  it('oferece Meu discipulado para discipulador e co-líder', async () => {
    const { rerender } = render(<AuthenticatedApp currentUser={user(['DISCIPULADOR'])} onLogout={() => undefined} />)
    expect(screen.getByRole('tab', { name: 'Meu discipulado' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: 'Registrar frequência' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Relatórios' })).toBeInTheDocument()
    rerender(<AuthenticatedApp currentUser={user(['CO_LIDER'])} onLogout={() => undefined} />)
    expect(screen.getByRole('tab', { name: 'Meu discipulado' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Relatórios' })).toBeInTheDocument()
  })

  it('soma Meu discipulado aos painéis de perfis acumulados', () => {
    render(<AuthenticatedApp currentUser={user(['ADMIN', 'GERENTE', 'DISCIPULADOR'])} onLogout={() => undefined} />)
    expect(screen.getByRole('tab', { name: 'Painel' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Minha gerência' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Meu discipulado' })).toBeInTheDocument()
  })

  it('lista usuários já cadastrados na área administrativa', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(async (input) => {
      const url = String(input)
      const page = url.includes('/painel/admin') ? emptyDashboard : url.includes('/painel/gerencia') ? emptyManagerDashboard : url.includes('/usuarios') ? { ...emptyPage, content: [user(['GERENTE'])], totalElements: 1, totalPages: 1 } : emptyPage
      return new Response(JSON.stringify(page), { status: 200, headers: { 'Content-Type': 'application/json' } })
    })
    render(<AuthenticatedApp currentUser={user(['ADMIN'])} onLogout={() => undefined} />)

    await userEvent.click(screen.getByRole('tab', { name: 'Usuários' }))

    expect(await screen.findByText('usuario@sgd.local')).toBeInTheDocument()
    expect(screen.getAllByText('Gerente')).not.toHaveLength(0)
  })

  it('carrega e seleciona somente discipulados liderados para discipulador', async () => {
    const fetchMock = vi.mocked(globalThis.fetch).mockImplementation(async (input) => {
      const url = String(input)
      const body = url.includes('/painel/lider') ? emptyLeaderDashboard
        : url.endsWith('/discipulados/liderados?ativo=true') ? [{ id: 7, nome: 'Meu grupo', sexo: 'MASCULINO', gerenciaId: 1, discipuladorId: 1, ativo: true, coLideres: [] }]
        : url.includes('/encontros?discipuladoId=7') ? []
        : url.includes('/adolescentes?discipuladoId=7') ? emptyPage
        : emptyPage
      return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
    })
    render(<AuthenticatedApp currentUser={user(['DISCIPULADOR'])} onLogout={() => undefined} />)

    await userEvent.click(screen.getByRole('tab', { name: 'Registrar frequência' }))

    expect(await screen.findByText('Meu grupo')).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/discipulados?'))).toBe(false)
    expect(fetchMock.mock.calls.some(([url]) => String(url).endsWith('/discipulados/liderados?ativo=true'))).toBe(true)
  })
})

function user(perfis: Usuario['perfis']): Usuario {
  return { id: 1, nome: 'Usuário', email: 'usuario@sgd.local', ativo: true, perfis }
}
