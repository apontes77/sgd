import { cleanup, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import AdolescentExportReport from '@/features/relatorios/AdolescentExportReport'
import ReportsPage from '@/features/relatorios/ReportsPage'
import type { Usuario } from '@/shared/api/types'
import { render } from '@/test/test-utils'

const adminUser: Usuario = {
  id: 1,
  nome: 'Admin',
  email: 'admin@sgd.local',
  ativo: true,
  perfis: ['ADMIN'],
}

const gerenteUser: Usuario = {
  id: 2,
  nome: 'Gerente',
  email: 'gerente@sgd.local',
  ativo: true,
  perfis: ['GERENTE'],
}

describe('exportação de adolescentes', () => {
  beforeEach(() => {
    sessionStorage.setItem('sgd.access-token', 'token')
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    sessionStorage.clear()
  })

  it('admin baixa planilha com filtros de discipulado e status', async () => {
    const user = userEvent.setup()
    const createObjectURL = vi.fn(() => 'blob:csv')
    const revokeObjectURL = vi.fn()
    vi.stubGlobal('URL', {
      ...URL,
      createObjectURL,
      revokeObjectURL,
    })

    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/discipulados')) {
        return new Response(
          JSON.stringify({
            content: [
              {
                id: 7,
                nome: 'Alpha',
                sexo: 'MASCULINO',
                faixaEtaria: 'DE_15_MAIS',
                gerenciaId: 1,
                discipuladorId: 3,
                discipuladorNome: 'Líder Alpha',
                coLideres: [],
                ativo: true,
              },
            ],
            page: 0,
            size: 100,
            totalElements: 1,
            totalPages: 1,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      if (url.includes('/relatorios/adolescentes/export')) {
        return new Response('Nome,Data\nBia,2010-02-01\n', {
          status: 200,
          headers: {
            'Content-Type': 'text/csv; charset=UTF-8',
            'Content-Disposition': 'attachment; filename="adolescentes-2026-08-12.csv"',
          },
        })
      }
      return new Response('not found', { status: 404 })
    })

    render(<AdolescentExportReport />)

    expect(await screen.findByRole('heading', { name: 'Relatório de adolescentes' })).toBeInTheDocument()
    await user.click(screen.getByRole('combobox', { name: /Discipulado/i }))
    await user.click(await screen.findByRole('option', { name: /Alpha/i }))
    await user.click(screen.getByRole('button', { name: /Baixar planilha/i }))

    expect(await screen.findByText(/Planilha gerada/i)).toBeInTheDocument()
    const exportCall = fetchMock.mock.calls.find(([url]) => String(url).includes('/relatorios/adolescentes/export'))
    expect(exportCall).toBeTruthy()
    expect(String(exportCall?.[0])).toContain('discipuladoId=7')
    expect(String(exportCall?.[0])).toContain('ativo=true')
    expect(createObjectURL).toHaveBeenCalled()
    expect(revokeObjectURL).toHaveBeenCalled()
  })

  it('exibe abas de frequência, adolescentes e liderança apenas para admin', async () => {
    const user = userEvent.setup()
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/discipulados')) {
        return new Response(JSON.stringify({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      return new Response(JSON.stringify({ dataInicio: '', dataFim: '', emitidoEm: '', relatorios: [] }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    const { unmount } = render(<ReportsPage currentUser={adminUser} />)
    expect(screen.getByRole('tab', { name: 'Frequência' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Frequência em formação' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Adolescentes' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Liderança' })).toBeInTheDocument()
    await user.click(screen.getByRole('tab', { name: 'Frequência em formação' }))
    expect(await screen.findByRole('heading', { name: 'Relatórios de frequência em formação' })).toBeInTheDocument()
    await user.click(screen.getByRole('tab', { name: 'Adolescentes' }))
    expect(await screen.findByRole('heading', { name: 'Relatório de adolescentes' })).toBeInTheDocument()
    await user.click(screen.getByRole('tab', { name: 'Liderança' }))
    expect(await screen.findByRole('heading', { name: 'Relatório de chamada de liderança' })).toBeInTheDocument()
    unmount()

    render(<ReportsPage currentUser={gerenteUser} />)
    expect(screen.queryByRole('tab', { name: 'Frequência' })).not.toBeInTheDocument()
    expect(screen.queryByRole('tab', { name: 'Frequência em formação' })).not.toBeInTheDocument()
    expect(screen.queryByRole('tab', { name: 'Adolescentes' })).not.toBeInTheDocument()
    expect(screen.queryByRole('tab', { name: 'Liderança' })).not.toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Relatórios de frequência' })).toBeInTheDocument()
  })

  it('oferece ao discipulador as abas de frequência padrão e em formação', async () => {
    const user = userEvent.setup()
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/discipulados')) {
        return new Response(JSON.stringify({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      return new Response(JSON.stringify({ dataInicio: '', dataFim: '', emitidoEm: '', relatorios: [] }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    const discipuladorUser: Usuario = {
      id: 3,
      nome: 'Líder',
      email: 'lider@sgd.local',
      ativo: true,
      perfis: ['DISCIPULADOR'],
    }
    render(<ReportsPage currentUser={discipuladorUser} />)
    expect(screen.getByRole('tab', { name: 'Frequência' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Frequência em formação' })).toBeInTheDocument()
    expect(screen.queryByRole('tab', { name: 'Adolescentes' })).not.toBeInTheDocument()
    expect(screen.queryByRole('tab', { name: 'Liderança' })).not.toBeInTheDocument()
    await user.click(screen.getByRole('tab', { name: 'Frequência em formação' }))
    expect(await screen.findByRole('heading', { name: 'Relatórios de frequência em formação' })).toBeInTheDocument()
  })
})
