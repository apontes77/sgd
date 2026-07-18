import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import LeaderDashboard from './LeaderDashboard'

vi.mock('echarts-for-react', () => ({ default: () => <div data-testid="grafico" /> }))
const resposta = { dataInicio: '2026-01-01', dataFim: '2026-03-31', discipulado: { id: 1, nome: 'Discipulado Esperança', sexo: 'FEMININO', ativo: true }, resumo: { encontrosRealizados: 1, presentes: 3, ausentes: 1, visitantes: 2, percentualPresenca: 75 }, evolucao: [{ referencia: '2026-02', presentes: 3, ausentes: 1, visitantes: 2, percentualPresenca: 75 }], discipulos: [{ adolescenteId: 10, nome: 'Ana Clara', presentes: 2, ausentes: 1, percentualPresenca: 66.67, evolucao: [{ referencia: '2026-02', presentes: 2, ausentes: 1, percentualPresenca: 66.67 }] }, { adolescenteId: 11, nome: 'Beatriz', presentes: 0, ausentes: 0, percentualPresenca: null, evolucao: [] }] }

describe('painel do líder', () => {
  afterEach(() => { cleanup(); vi.restoreAllMocks() })

  it('mostra KPIs, série contínua e tabela acessível do próprio grupo', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(resposta), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    render(<LeaderDashboard />)
    expect(await screen.findByText('Discipulado Esperança')).toBeInTheDocument()
    await userEvent.click(screen.getAllByRole('button', { name: 'Dados' })[0])
    expect(screen.getAllByText('75,00%')).toHaveLength(2)
    expect(screen.getAllByText('Sem encontros realizados')).toHaveLength(2)
    expect(screen.getByRole('table', { name: 'Histórico mensal do discipulado' })).toBeInTheDocument()
    expect(screen.getByLabelText('Discípulo')).toHaveValue('Ana Clara')
    expect(screen.getByRole('img', { name: 'Gráfico do percentual mensal de presença de Ana Clara.' })).toBeInTheDocument()
    await userEvent.click(screen.getAllByRole('button', { name: 'Dados' })[1])
    expect(screen.getByRole('table', { name: 'Histórico mensal de presença de Ana Clara' })).toBeInTheDocument()
    expect(screen.getAllByText('66,67%')).toHaveLength(2)
    const seletor = screen.getByRole('combobox', { name: 'Discípulo' })
    await userEvent.click(seletor)
    await userEvent.clear(seletor)
    await userEvent.type(seletor, 'Bea')
    await userEvent.click(await screen.findByText('Beatriz'))
    expect(screen.getByText('Não há registros de frequência para Beatriz no período selecionado.')).toBeInTheDocument()
  })

  it('apresenta a mensagem de conflito devolvida pela API', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ detail: 'O usuário possui mais de uma associação de liderança.' }), { status: 409, headers: { 'Content-Type': 'application/problem+json' } }))
    render(<LeaderDashboard />)
    expect(await screen.findByText('O usuário possui mais de uma associação de liderança.')).toBeInTheDocument()
  })

  it('mostra estado vazio quando não há discípulos no período', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ ...resposta, discipulos: [] }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    render(<LeaderDashboard />)
    expect(await screen.findByText('Nenhum discípulo no período')).toBeInTheDocument()
  })
})
