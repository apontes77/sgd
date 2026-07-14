import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ManagerDashboard from './ManagerDashboard'

vi.mock('echarts-for-react', () => ({ default: () => <div data-testid="grafico" /> }))
const resumo = { encontrosRealizados: 2, presentes: 3, ausentes: 1, visitantes: 5, percentualPresenca: 75 }
const resposta = { dataInicio: '2026-01-01', dataFim: '2026-07-01', gerencia: { id: 1, nome: 'Gerência Centro' }, resumo, evolucao: [{ referencia: '2026-06', presentes: 3, ausentes: 1, visitantes: 5, percentualPresenca: 75 }], discipulados: [{ id: 1, nome: 'Discipulado A', sexo: 'MASCULINO', ativo: true, resumo, evolucao: [{ referencia: '2026-06', presentes: 3, ausentes: 1, visitantes: 5, percentualPresenca: 75 }] }, { id: 2, nome: 'Discipulado Antigo', sexo: 'FEMININO', ativo: false, resumo: { ...resumo, percentualPresenca: 50 }, evolucao: [] }] }

describe('painel da gerência', () => {
  afterEach(() => { cleanup(); vi.restoreAllMocks() })
  it('mostra agregado, comparação, detalhe e tabelas acessíveis', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(resposta), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    render(<ManagerDashboard />)
    expect(await screen.findByText('Gerência Centro')).toBeInTheDocument()
    expect(screen.getByText('Resumo por discipulado')).toBeInTheDocument()
    expect(screen.getAllByText('Discipulado A').length).toBeGreaterThan(0)
    expect(screen.getAllByTestId('grafico')).toHaveLength(3)
  })
  it('permite selecionar um discipulado inativo', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(resposta), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    render(<ManagerDashboard />); await screen.findByText('Gerência Centro')
    await userEvent.click(screen.getByLabelText('Discipulado')); await userEvent.click(screen.getByRole('option', { name: /Discipulado Antigo/ }))
    expect(await screen.findByText('Inativo')).toBeInTheDocument()
  })
  it('aplica novo período e mostra erros Problem Details', async () => {
    const fetch = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(new Response(JSON.stringify(resposta), { status: 200, headers: { 'Content-Type': 'application/json' } })).mockResolvedValueOnce(new Response(JSON.stringify({ detail: 'O gerente possui mais de uma gerência ativa.' }), { status: 409, headers: { 'Content-Type': 'application/problem+json' } }))
    render(<ManagerDashboard />); await screen.findByText('Gerência Centro')
    await userEvent.clear(screen.getByLabelText(/Data inicial/)); await userEvent.type(screen.getByLabelText(/Data inicial/), '2026-02-01'); await userEvent.click(screen.getByRole('button', { name: 'Aplicar' }))
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(2)); expect(await screen.findByText('O gerente possui mais de uma gerência ativa.')).toBeInTheDocument()
  })
})
