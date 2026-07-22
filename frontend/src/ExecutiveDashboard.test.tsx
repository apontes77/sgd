import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ExecutiveDashboard from './ExecutiveDashboard'

vi.mock('echarts-for-react', () => ({ default: () => <div data-testid="grafico" /> }))

const resposta = {
  dataInicio: '2026-01-01',
  dataFim: '2026-07-01',
  resumo: { encontrosRealizados: 2, presentes: 3, ausentes: 1, visitantes: 7, percentualPresenca: 75 },
  evolucao: [{ referencia: '2026-06', presentes: 3, ausentes: 1, visitantes: 7, percentualPresenca: 75 }],
  gerencias: [{ id: 1, nome: 'Gerência Centro', presentes: 3, ausentes: 1, percentualPresenca: 75 }],
  sexos: [{ sexo: 'MASCULINO', presentes: 2, ausentes: 1, percentualPresenca: 66.67 }, { sexo: 'FEMININO', presentes: 1, ausentes: 0, percentualPresenca: 100 }],
  encontrosNaoRealizados: 1,
  gerenciasMensal: [{ gerenciaId: 1, gerenciaNome: 'Gerência Centro', referencia: '2026-06', presentes: 3, ausentes: 1, percentualPresenca: 75 }],
}

describe('visão executiva', () => {
  afterEach(() => { cleanup(); vi.restoreAllMocks(); sessionStorage.clear() })

  it('carrega a grade de widgets executivos', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(resposta), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    render(<ExecutiveDashboard />)
    expect(await screen.findByRole('heading', { name: 'Visão executiva' })).toBeInTheDocument()
    expect(await screen.findAllByTestId('grafico')).toHaveLength(6)
    expect(screen.getByText('Presença geral')).toBeInTheDocument()
    expect(screen.getByText('Volume mensal')).toBeInTheDocument()
    expect(screen.getByText('Encontros por situação')).toBeInTheDocument()
    expect(screen.getByText('Composição de presença')).toBeInTheDocument()
    expect(screen.getByText('Top gerências por presença')).toBeInTheDocument()
    expect(screen.getByText('Presença por gerência × mês')).toBeInTheDocument()
  })

  it('aplica um novo período', async () => {
    const fetch = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(resposta), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    render(<ExecutiveDashboard />)
    await screen.findAllByTestId('grafico')
    await userEvent.clear(screen.getByLabelText(/Data inicial/)); await userEvent.type(screen.getByLabelText(/Data inicial/), '2026-02-01')
    await userEvent.clear(screen.getByLabelText(/Data final/)); await userEvent.type(screen.getByLabelText(/Data final/), '2026-04-30')
    await userEvent.click(screen.getByRole('button', { name: 'Aplicar' }))
    await waitFor(() => expect(String(fetch.mock.calls.at(-1)?.[0])).toContain('dataInicio=2026-02-01'))
    expect(String(fetch.mock.calls.at(-1)?.[0])).toContain('dataFim=2026-04-30')
  })
})
