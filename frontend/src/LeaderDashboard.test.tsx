import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import LeaderDashboard from './LeaderDashboard'

vi.mock('echarts-for-react', () => ({ default: () => <div data-testid="grafico" /> }))
const resposta = { dataInicio: '2026-01-01', dataFim: '2026-03-31', discipulado: { id: 1, nome: 'Discipulado Esperança', sexo: 'FEMININO', ativo: true }, resumo: { encontrosRealizados: 1, presentes: 3, ausentes: 1, visitantes: 2, percentualPresenca: 75 }, evolucao: [{ referencia: '2026-02', presentes: 3, ausentes: 1, visitantes: 2, percentualPresenca: 75 }] }

describe('painel do líder', () => {
  afterEach(() => { cleanup(); vi.restoreAllMocks() })

  it('mostra KPIs, série contínua e tabela acessível do próprio grupo', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(resposta), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    render(<LeaderDashboard />)
    expect(await screen.findByText('Discipulado Esperança')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Dados' }))
    expect(screen.getAllByText('75,00%')).toHaveLength(2)
    expect(screen.getAllByText('Sem encontros realizados')).toHaveLength(2)
    expect(screen.getByRole('table', { name: 'Histórico mensal do discipulado' })).toBeInTheDocument()
  })

  it('apresenta a mensagem de conflito devolvida pela API', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ detail: 'O usuário possui mais de uma associação de liderança.' }), { status: 409, headers: { 'Content-Type': 'application/problem+json' } }))
    render(<LeaderDashboard />)
    expect(await screen.findByText('O usuário possui mais de uma associação de liderança.')).toBeInTheDocument()
  })
})
