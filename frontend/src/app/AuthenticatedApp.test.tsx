import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import AuthenticatedApp from '@/app/AuthenticatedApp'
import type { Usuario } from '@/shared/api/types'

const emptyPage = { content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }
const emptyDashboard = {
  dataInicio: '2026-01-01',
  dataFim: '2026-07-01',
  resumo: { encontrosRealizados: 0, presentes: 0, ausentes: 0, visitantes: 0, percentualPresenca: 0 },
  evolucao: [],
  gerencias: [],
  sexos: [
    { sexo: 'MASCULINO', presentes: 0, ausentes: 0, percentualPresenca: 0 },
    { sexo: 'FEMININO', presentes: 0, ausentes: 0, percentualPresenca: 0 },
  ],
  encontrosNaoRealizados: 0,
  gerenciasMensal: [],
}
const emptyManagerDashboard = {
  dataInicio: '2026-01-01',
  dataFim: '2026-07-01',
  gerencia: { id: 1, nome: 'Centro' },
  resumo: emptyDashboard.resumo,
  evolucao: [],
  discipulados: [],
  encontrosNaoRealizados: [],
}
const emptyLeaderDashboard = {
  dataInicio: '2026-01-01',
  dataFim: '2026-07-01',
  discipulado: { id: 1, nome: 'Discipulado A', sexo: 'MASCULINO', ativo: true },
  resumo: emptyDashboard.resumo,
  evolucao: [],
  discipulos: [],
}

vi.mock('echarts-for-react', () => ({ default: () => <div data-testid="grafico" /> }))

function mockViewport(width: number) {
  Object.defineProperty(window, 'innerWidth', { configurable: true, writable: true, value: width })
  window.matchMedia = vi.fn().mockImplementation((query: string) => {
    const min = /min-width:\s*(\d+)/.exec(query)
    const max = /max-width:\s*([\d.]+)/.exec(query)
    let matches = false
    if (min) matches = width >= Number(min[1])
    if (max) matches = width <= Number(max[1])
    return {
      matches,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }
  })
}

describe('navegação autenticada', () => {
  beforeEach(() => {
    mockViewport(1200)
    window.history.replaceState({}, '', '/')
    sessionStorage.setItem('sgd.access-token', 'token')
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      return new Response(
        JSON.stringify(
          url.includes('/painel/admin')
            ? emptyDashboard
            : url.includes('/painel/gerencia')
              ? emptyManagerDashboard
              : url.includes('/painel/lider')
                ? emptyLeaderDashboard
                : emptyPage,
        ),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      )
    })
  })
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    sessionStorage.clear()
    window.history.replaceState({}, '', '/')
  })

  it('oferece todos os módulos administrativos ao ADMIN', () => {
    render(<AuthenticatedApp currentUser={user(['ADMIN'])} onLogout={() => undefined} />)
    expect(screen.getByRole('tab', { name: 'Visão executiva' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Visão executiva' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: 'Painel' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Estrutura' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Usuários' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Adolescentes' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Encontros e frequência' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Relatórios' })).toBeInTheDocument()
  })

  it('usa o gênero correto no botão de criação da estrutura', async () => {
    render(<AuthenticatedApp currentUser={user(['ADMIN'])} onLogout={() => undefined} />)
    await userEvent.click(screen.getByRole('tab', { name: 'Estrutura' }))
    expect(await screen.findByRole('button', { name: 'Nova gerência' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('tab', { name: 'Discipulados' }))
    expect(screen.getByRole('button', { name: 'Novo discipulado' })).toBeInTheDocument()
  })

  it('oferece visão executiva e painel gerencial ao GERENTE', () => {
    render(<AuthenticatedApp currentUser={user(['GERENTE'])} onLogout={() => undefined} />)
    expect(screen.getByRole('tab', { name: 'Visão executiva' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Visão executiva' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: 'Minha gerência' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Adolescentes' })).toBeInTheDocument()
    expect(screen.queryByRole('tab', { name: 'Usuários' })).not.toBeInTheDocument()
    expect(screen.queryByRole('tab', { name: 'Frequência' })).not.toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Relatórios' })).toBeInTheDocument()
  })

  it('mantém os painéis administrativo e gerencial para perfil combinado', () => {
    render(<AuthenticatedApp currentUser={user(['ADMIN', 'GERENTE'])} onLogout={() => undefined} />)
    expect(screen.getByRole('tab', { name: 'Visão executiva' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Painel' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Minha gerência' })).toBeInTheDocument()
  })

  it('sincroniza a seção com deep-link /app/:secao', async () => {
    window.history.pushState({}, '', '/app/adolescentes')
    render(<AuthenticatedApp currentUser={user(['ADMIN'])} onLogout={() => undefined} />)
    expect(screen.getByRole('tab', { name: 'Adolescentes' })).toHaveAttribute('aria-selected', 'true')
    await userEvent.click(screen.getByRole('tab', { name: 'Relatórios' }))
    expect(window.location.pathname).toBe('/app/relatorios')
  })

  it('mantém Sair na navegação lateral e remove o menu superior', async () => {
    const onLogout = vi.fn()
    render(<AuthenticatedApp currentUser={user(['ADMIN'])} onLogout={onLogout} />)

    expect(screen.queryByRole('button', { name: 'Menu do usuário' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Abrir menu' })).not.toBeInTheDocument()
    const sair = screen.getByRole('button', { name: 'Sair' })
    expect(sair).toBeInTheDocument()
    await userEvent.click(sair)

    expect(onLogout).toHaveBeenCalledOnce()
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
    expect(screen.getByRole('tab', { name: 'Visão executiva' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Painel' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Minha gerência' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Meu discipulado' })).toBeInTheDocument()
  })

  it('lista usuários já cadastrados na área administrativa', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(async (input) => {
      const url = String(input)
      const page = url.includes('/painel/admin')
        ? emptyDashboard
        : url.includes('/painel/gerencia')
          ? emptyManagerDashboard
          : url.includes('/usuarios')
            ? { ...emptyPage, content: [user(['GERENTE'])], totalElements: 1, totalPages: 1 }
            : emptyPage
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
      const body = url.includes('/painel/lider')
        ? emptyLeaderDashboard
        : url.endsWith('/discipulados/liderados?ativo=true')
          ? [
              {
                id: 7,
                nome: 'Meu grupo',
                sexo: 'MASCULINO',
                gerenciaId: 1,
                discipuladorId: 1,
                ativo: true,
                coLideres: [],
              },
            ]
          : url.includes('/encontros?discipuladoId=7')
            ? []
            : url.includes('/adolescentes?discipuladoId=7')
              ? emptyPage
              : emptyPage
      return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
    })
    render(<AuthenticatedApp currentUser={user(['DISCIPULADOR'])} onLogout={() => undefined} />)

    await userEvent.click(screen.getByRole('tab', { name: 'Registrar frequência' }))

    expect(await screen.findByRole('button', { name: /^Houve discipulado/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Não houve discipulado/ })).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/encontros?discipuladoId=7'))).toBe(true)
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/discipulados?'))).toBe(false)
    expect(fetchMock.mock.calls.some(([url]) => String(url).endsWith('/discipulados/liderados?ativo=true'))).toBe(true)
  })

  it('co-líder também pode registrar que não houve discipulado', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(async (input) => {
      const url = String(input)
      const body = url.includes('/painel/lider')
        ? emptyLeaderDashboard
        : url.endsWith('/discipulados/liderados?ativo=true')
          ? [
              {
                id: 7,
                nome: 'Meu grupo',
                sexo: 'MASCULINO',
                gerenciaId: 1,
                discipuladorId: 1,
                ativo: true,
                coLideres: [],
              },
            ]
          : url.includes('/encontros?discipuladoId=7')
            ? []
            : url.includes('/adolescentes?discipuladoId=7')
              ? emptyPage
              : emptyPage
      return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
    })
    render(<AuthenticatedApp currentUser={user(['CO_LIDER'])} onLogout={() => undefined} />)

    await userEvent.click(screen.getByRole('tab', { name: 'Registrar frequência' }))

    expect(await screen.findByRole('button', { name: /^Houve discipulado/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Não houve discipulado/ })).toBeInTheDocument()
  })

  it('no mobile, usa bottom navigation sem hamburger e abre Mais', async () => {
    mockViewport(390)
    const onLogout = vi.fn()
    render(<AuthenticatedApp currentUser={user(['ADMIN'])} onLogout={onLogout} />)

    expect(screen.queryByRole('button', { name: 'Abrir menu' })).not.toBeInTheDocument()
    const mobileNav = screen.getByRole('navigation', { name: 'Navegação móvel' })
    expect(within(mobileNav).getByRole('button', { name: 'Encontros e frequência' })).toBeInTheDocument()
    expect(within(mobileNav).getByRole('button', { name: 'Mais opções' })).toBeInTheDocument()

    await userEvent.click(within(mobileNav).getByRole('button', { name: 'Mais opções' }))
    const moreSheet = await screen.findByRole('presentation')
    expect(within(moreSheet).getByRole('button', { name: 'Estrutura' })).toBeInTheDocument()
    await userEvent.click(within(moreSheet).getByRole('button', { name: 'Sair' }))
    expect(onLogout).toHaveBeenCalledOnce()
  })
})

function user(perfis: Usuario['perfis']): Usuario {
  return { id: 1, nome: 'Usuário', email: 'usuario@sgd.local', ativo: true, perfis }
}
