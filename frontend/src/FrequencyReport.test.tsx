import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import FrequencyReport from './FrequencyReport'
import type { RelatorioDiarioResponse } from './relatorioApi'

const relatorio: RelatorioDiarioResponse = {
  data: '2026-07-21',
  emitidoEm: '2026-07-21T22:00:00Z',
  relatorios: [
    {
      encontroId: 10,
      data: '2026-07-21',
      gerencia: { id: 1, nome: 'Centro' },
      discipulado: { id: 2, nome: 'Alpha', sexo: 'MASCULINO' },
      discipulador: { id: 3, nome: 'Líder Alpha' },
      coLideres: [{ id: 4, nome: 'Co-líder Alpha' }],
      participantes: [
        { adolescenteId: 5, nome: 'Ana', telefone: '(11) 97777-1111', situacao: 'AUSENTE' },
        { adolescenteId: 6, nome: 'Bia', telefone: null, situacao: 'PRESENTE' },
      ],
      visitantes: 3,
      resumo: { presentes: 1, ausentes: 1, participantes: 2, visitantes: 3, percentualPresenca: 50 },
    },
    {
      encontroId: 11,
      data: '2026-07-21',
      gerencia: { id: 1, nome: 'Centro' },
      discipulado: { id: 7, nome: 'Beta', sexo: 'FEMININO' },
      discipulador: { id: 8, nome: 'Líder Beta' },
      coLideres: [],
      participantes: [],
      visitantes: 0,
      resumo: { presentes: 0, ausentes: 0, participantes: 0, visitantes: 0, percentualPresenca: 0 },
    },
  ],
}

describe('relatório diário de frequência', () => {
  beforeEach(() => {
    sessionStorage.setItem('sgd.access-token', 'token')
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    sessionStorage.clear()
  })

  it('consulta a data, renderiza uma página por encontro e imprime o resultado', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(relatorio), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }))
    const printMock = vi.spyOn(window, 'print').mockImplementation(() => undefined)
    render(<FrequencyReport />)

    const imprimir = screen.getByRole('button', { name: 'Imprimir / salvar como PDF' })
    expect(imprimir).toBeDisabled()
    const data = screen.getByLabelText(/Data/)
    await userEvent.clear(data)
    await userEvent.type(data, '2026-07-21')
    await userEvent.click(screen.getByRole('button', { name: 'Consultar' }))

    expect(await screen.findByRole('table', { name: 'Frequência do Alpha em 21/07/2026' })).toBeInTheDocument()
    expect(screen.getByRole('table', { name: 'Frequência do Beta em 21/07/2026' })).toBeInTheDocument()
    expect(screen.getByText('Ana')).toBeInTheDocument()
    expect(screen.getByText('Bia')).toBeInTheDocument()
    expect(screen.getByText('(11) 97777-1111')).toBeInTheDocument()
    expect(screen.getByText('Não informado')).toBeInTheDocument()
    expect(screen.getAllByText('21/07/2026')).toHaveLength(4)
    expect(screen.getByText('Ausente')).toBeInTheDocument()
    expect(screen.getByText('Presente')).toBeInTheDocument()
    expect(screen.getByText('Nenhuma frequência registrada neste encontro.')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/relatorios/frequencia-diaria?data=2026-07-21', expect.anything())

    await userEvent.click(imprimir)
    expect(printMock).toHaveBeenCalledOnce()
  })

  it('informa quando não há encontros e mantém a impressão desabilitada', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ ...relatorio, relatorios: [] }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }))
    render(<FrequencyReport />)

    await userEvent.click(screen.getByRole('button', { name: 'Consultar' }))

    expect(await screen.findByText(/Não há encontros realizados no seu escopo/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Imprimir / salvar como PDF' })).toBeDisabled()
  })
})
