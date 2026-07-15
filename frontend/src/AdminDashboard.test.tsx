import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AdminDashboard from './AdminDashboard'

vi.mock('echarts-for-react', () => ({ default: () => <div data-testid="grafico" /> }))
const resposta = { dataInicio: '2026-01-01', dataFim: '2026-07-01', resumo: { encontrosRealizados: 2, presentes: 3, ausentes: 1, visitantes: 7, percentualPresenca: 75 }, evolucao: [{ referencia: '2026-06', presentes: 3, ausentes: 1, visitantes: 7, percentualPresenca: 75 }], gerencias: [{ id: 1, nome: 'Gerência Centro', presentes: 3, ausentes: 1, percentualPresenca: 75 }], sexos: [{ sexo: 'MASCULINO', presentes: 2, ausentes: 1, percentualPresenca: 66.67 }, { sexo: 'FEMININO', presentes: 1, ausentes: 0, percentualPresenca: 100 }] }

describe('painel administrativo', () => {
  afterEach(() => { cleanup(); vi.restoreAllMocks(); sessionStorage.clear() })

  it('consulta seis meses e apresenta KPIs, gráficos e tabelas', async () => {
    const fetch = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(resposta), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    render(<AdminDashboard />)
    expect(await screen.findByRole('heading', { name: 'Painel administrativo' })).toBeInTheDocument()
    expect(await screen.findAllByTestId('grafico')).toHaveLength(3)
    await userEvent.click(screen.getAllByRole('button', { name: 'Dados' })[1])
    expect(await screen.findByText('Gerência Centro')).toBeInTheDocument()
    expect(screen.getAllByText('75,00%').length).toBeGreaterThan(0)
    const url = String(fetch.mock.calls[0][0])
    const params = new URL(url, 'http://localhost').searchParams
    const inicio = new Date(`${params.get('dataInicio')}T12:00:00`)
    const fim = new Date(`${params.get('dataFim')}T12:00:00`)
    expect((fim.getFullYear() - inicio.getFullYear()) * 12 + fim.getMonth() - inicio.getMonth()).toBe(6)
  })

  it('aplica um novo período', async () => {
    const fetch = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(resposta), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    render(<AdminDashboard />)
    await screen.findAllByTestId('grafico')
    await userEvent.clear(screen.getByLabelText(/Data inicial/)); await userEvent.type(screen.getByLabelText(/Data inicial/), '2026-02-01')
    await userEvent.clear(screen.getByLabelText(/Data final/)); await userEvent.type(screen.getByLabelText(/Data final/), '2026-04-30')
    await userEvent.click(screen.getByRole('button', { name: 'Aplicar' }))
    await waitFor(() => expect(String(fetch.mock.calls.at(-1)?.[0])).toContain('dataInicio=2026-02-01'))
    expect(String(fetch.mock.calls.at(-1)?.[0])).toContain('dataFim=2026-04-30')
  })

  it('mostra erro padronizado da API', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ title: 'Período inválido', detail: 'O período máximo permitido é de 24 meses.' }), { status: 400, headers: { 'Content-Type': 'application/problem+json' } }))
    render(<AdminDashboard />)
    expect(await screen.findByText('O período máximo permitido é de 24 meses.')).toBeInTheDocument()
  })
})
