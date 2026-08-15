import { cleanup, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { RelatorioChamadaLiderancaPeriodoResponse } from '@/features/relatorios/api'
import LeadershipAttendanceReport from '@/features/relatorios/LeadershipAttendanceReport'
import { render } from '@/test/test-utils'

const relatorio: RelatorioChamadaLiderancaPeriodoResponse = {
  dataInicio: '2026-08-07',
  dataFim: '2026-08-14',
  emitidoEm: '2026-08-14T22:00:00Z',
  relatorios: [
    {
      chamadaId: 1,
      data: '2026-08-14',
      observacaoGeral: 'Culto tranquilo',
      discipulados: [
        {
          discipuladoId: 2,
          discipuladoNome: 'Alpha',
          sexo: 'MASCULINO',
          gerenciaNome: 'Centro',
          observacao: 'Líder chegou atrasado',
          presencas: [
            { usuarioId: 3, nome: 'Líder Alpha', papel: 'DISCIPULADOR', situacao: 'PRESENTE' },
            { usuarioId: 4, nome: 'Co-líder Alpha', papel: 'CO_LIDER', situacao: 'AUSENTE' },
          ],
        },
        {
          discipuladoId: 7,
          discipuladoNome: 'Beta',
          sexo: 'FEMININO',
          gerenciaNome: 'Centro',
          observacao: null,
          presencas: [{ usuarioId: 8, nome: 'Líder Beta', papel: 'DISCIPULADOR', situacao: 'PRESENTE' }],
        },
      ],
      resumo: { presentes: 2, ausentes: 1, participantes: 3, percentualPresenca: 66.67 },
    },
  ],
}

describe('relatório de chamada de liderança', () => {
  beforeEach(() => {
    sessionStorage.setItem('sgd.access-token', 'token')
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    sessionStorage.clear()
  })

  it('consulta o período, renderiza uma página por data e imprime o resultado', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/discipulados')) {
        return new Response(
          JSON.stringify({
            content: [],
            page: 0,
            size: 100,
            totalElements: 0,
            totalPages: 0,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      return new Response(JSON.stringify(relatorio), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })
    const printMock = vi.spyOn(window, 'print').mockImplementation(() => undefined)
    render(<LeadershipAttendanceReport />)

    const imprimir = screen.getByRole('button', { name: 'Imprimir / salvar como PDF' })
    const exportar = screen.getByRole('button', { name: 'Exportar Excel' })
    expect(imprimir).toBeDisabled()
    expect(exportar).toBeDisabled()
    expect(await screen.findByLabelText(/^Discipulado/)).toBeInTheDocument()
    const dataInicial = screen.getByLabelText(/^Data inicial/)
    const dataFinal = screen.getByLabelText(/^Data final/)
    await userEvent.clear(dataInicial)
    await userEvent.type(dataInicial, '2026-08-07')
    await userEvent.clear(dataFinal)
    await userEvent.type(dataFinal, '2026-08-14')
    await userEvent.click(screen.getByRole('button', { name: 'Consultar' }))

    expect(
      await screen.findByRole('table', { name: 'Chamada de liderança do Alpha em 14/08/2026' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('table', { name: 'Chamada de liderança do Beta em 14/08/2026' })).toBeInTheDocument()
    expect(screen.getByText('Líder Alpha')).toBeInTheDocument()
    expect(screen.getByText('Co-líder Alpha')).toBeInTheDocument()
    expect(screen.getByText('Líder chegou atrasado')).toBeInTheDocument()
    expect(screen.getByText(/Culto tranquilo/)).toBeInTheDocument()
    expect(screen.getByText('Ausente')).toHaveClass('frequencia-ausente')
    expect(screen.getAllByText('Presente')[0]).toHaveClass('frequencia-presente')
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/relatorios/chamadas-lideranca?dataInicio=2026-08-07&dataFim=2026-08-14',
      expect.anything(),
    )

    await userEvent.click(imprimir)
    expect(printMock).toHaveBeenCalledOnce()
    expect(exportar).toBeEnabled()
  })

  it('informa quando não há chamadas e mantém a impressão desabilitada', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/discipulados')) {
        return new Response(
          JSON.stringify({
            content: [],
            page: 0,
            size: 100,
            totalElements: 0,
            totalPages: 0,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      return new Response(JSON.stringify({ ...relatorio, relatorios: [] }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })
    render(<LeadershipAttendanceReport />)

    await userEvent.click(screen.getByRole('button', { name: 'Consultar' }))

    expect(await screen.findByText(/Não há chamadas de liderança no período/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Imprimir / salvar como PDF' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Exportar Excel' })).toBeDisabled()
  })

  it('permite filtrar o relatório por discipulado', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/discipulados')) {
        return new Response(
          JSON.stringify({
            content: [
              {
                id: 2,
                nome: 'Alpha',
                sexo: 'MASCULINO',
                faixaEtaria: 'DE_15_MAIS',
                gerenciaId: 1,
                discipuladorId: 3,
                discipuladorNome: 'Líder Alpha',
                coLideres: [],
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
      return new Response(
        JSON.stringify({
          ...relatorio,
          relatorios: [
            {
              ...relatorio.relatorios[0],
              discipulados: [relatorio.relatorios[0].discipulados[0]],
            },
          ],
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      )
    })

    render(<LeadershipAttendanceReport />)

    const campo = await screen.findByPlaceholderText('Pesquisar discipulado ou discipulador')
    await userEvent.type(campo, 'Líder Alpha')
    await userEvent.click(await screen.findByRole('option', { name: /Líder Alpha/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Consultar' }))

    expect(
      await screen.findByRole('table', { name: 'Chamada de liderança do Alpha em 14/08/2026' }),
    ).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('discipuladoId=2'))).toBe(true)
  })
})
