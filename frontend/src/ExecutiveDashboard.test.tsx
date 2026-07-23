import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ExecutiveDashboard from './ExecutiveDashboard'

vi.mock('echarts-for-react', () => ({ default: () => <div data-testid="grafico" /> }))

const respostaAdmin = {
  dataInicio: '2026-01-01',
  dataFim: '2026-07-01',
  resumo: { encontrosRealizados: 2, presentes: 3, ausentes: 1, visitantes: 7, percentualPresenca: 75 },
  evolucao: [{ referencia: '2026-06', presentes: 3, ausentes: 1, visitantes: 7, percentualPresenca: 75 }],
  gerencias: [{ id: 1, nome: 'Gerência Centro', presentes: 3, ausentes: 1, percentualPresenca: 75 }],
  sexos: [{ sexo: 'MASCULINO', presentes: 2, ausentes: 1, percentualPresenca: 66.67 }, { sexo: 'FEMININO', presentes: 1, ausentes: 0, percentualPresenca: 100 }],
  encontrosNaoRealizados: 1,
  gerenciasMensal: [{ gerenciaId: 1, gerenciaNome: 'Gerência Centro', referencia: '2026-06', presentes: 3, ausentes: 1, percentualPresenca: 75 }],
}

const respostaGerencia = {
  dataInicio: '2026-01-01',
  dataFim: '2026-07-01',
  gerencia: { id: 1, nome: 'Centro' },
  resumo: respostaAdmin.resumo,
  evolucao: respostaAdmin.evolucao,
  discipulados: [{
    id: 9,
    nome: 'Discipulado A',
    sexo: 'MASCULINO',
    ativo: true,
    resumo: respostaAdmin.resumo,
    evolucao: respostaAdmin.evolucao,
  }],
  encontrosNaoRealizados: [{ encontroId: 1, discipuladoId: 9, discipuladoNome: 'Discipulado A', data: '2026-06-01', justificativa: 'Chuva' }],
}

describe('visão executiva', () => {
  afterEach(() => { cleanup(); vi.restoreAllMocks(); sessionStorage.clear() })

  it('carrega a grade de widgets executivos do admin', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(respostaAdmin), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    render(<ExecutiveDashboard escopo="admin" />)
    expect(await screen.findByRole('heading', { name: 'Visão executiva' })).toBeInTheDocument()
    expect(await screen.findByTestId('grade-executiva')).toBeInTheDocument()
    expect(await screen.findAllByTestId('grafico')).toHaveLength(6)
    expect(screen.getByText('Presença geral')).toBeInTheDocument()
    expect(screen.getByText('Top gerências por presença')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Imprimir / PDF' })).toBeInTheDocument()
  })

  it('usa o escopo de gerência e dispara drill-down', async () => {
    const onAbrirDetalhe = vi.fn()
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(respostaGerencia), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    render(<ExecutiveDashboard escopo="gerencia" onAbrirDetalhe={onAbrirDetalhe} />)
    expect(await screen.findByText('Top discipulados por presença')).toBeInTheDocument()
    expect(screen.getByText('Presença por discipulado × mês')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Abrir minha gerência' }))
    expect(onAbrirDetalhe).toHaveBeenCalledOnce()
  })

  it('mostra empty states quando não há dados', async () => {
    const vazio = {
      ...respostaAdmin,
      resumo: { encontrosRealizados: 0, presentes: 0, ausentes: 0, visitantes: 0, percentualPresenca: 0 },
      evolucao: [],
      gerencias: [],
      gerenciasMensal: [],
      encontrosNaoRealizados: 0,
    }
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(vazio), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    render(<ExecutiveDashboard />)
    expect(await screen.findByText('Sem registros de presença')).toBeInTheDocument()
    expect(screen.getByText('Sem ranking')).toBeInTheDocument()
    expect(screen.getByText('Sem mapa de calor')).toBeInTheDocument()
  })

  it('aplica um novo período', async () => {
    const fetch = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(respostaAdmin), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    render(<ExecutiveDashboard />)
    await screen.findByTestId('grade-executiva')
    await userEvent.clear(screen.getByLabelText(/Data inicial/)); await userEvent.type(screen.getByLabelText(/Data inicial/), '2026-02-01')
    await userEvent.clear(screen.getByLabelText(/Data final/)); await userEvent.type(screen.getByLabelText(/Data final/), '2026-04-30')
    await userEvent.click(screen.getByRole('button', { name: 'Aplicar' }))
    await waitFor(() => expect(String(fetch.mock.calls.at(-1)?.[0])).toContain('dataInicio=2026-02-01'))
    expect(String(fetch.mock.calls.at(-1)?.[0])).toContain('dataFim=2026-04-30')
  })
})
