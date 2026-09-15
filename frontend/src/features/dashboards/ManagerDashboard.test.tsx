import { cleanup, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'

import ManagerDashboard from '@/features/dashboards/ManagerDashboard'
import { render } from '@/test/test-utils'

vi.mock('echarts-for-react', () => ({ default: () => <div data-testid="grafico" /> }))
const resumo = { encontrosRealizados: 2, presentes: 3, ausentes: 1, visitantes: 5, percentualPresenca: 75 }
const resposta = {
  dataInicio: '2026-01-01',
  dataFim: '2026-07-01',
  gerencia: { id: 1, nome: 'Gerência Centro' },
  resumo,
  evolucao: [{ referencia: '2026-06', presentes: 3, ausentes: 1, visitantes: 5, percentualPresenca: 75 }],
  discipulados: [
    {
      id: 1,
      nome: 'Discipulado A',
      sexo: 'MASCULINO',
      ativo: true,
      resumo,
      evolucao: [{ referencia: '2026-06', presentes: 3, ausentes: 1, visitantes: 5, percentualPresenca: 75 }],
    },
    {
      id: 2,
      nome: 'Discipulado Antigo',
      sexo: 'FEMININO',
      ativo: false,
      resumo: { ...resumo, percentualPresenca: 50 },
      evolucao: [],
    },
  ],
  encontrosNaoRealizados: [
    {
      encontroId: 20,
      discipuladoId: 1,
      discipuladoNome: 'Discipulado A',
      data: '2026-06-15',
      justificativa: 'Líder doente',
    },
  ],
}

const desempenho = {
  dataInicio: '2026-01-01',
  dataFim: '2026-07-01',
  discipulados: [
    {
      id: 1,
      nome: 'Discipulado A',
      sexo: 'MASCULINO',
      faixaEtaria: 'DE_09_A_11',
      gerenciaId: 1,
      gerenciaNome: 'Gerência Centro',
      discipuladorNome: 'Líder A',
      ativo: true,
      frequencia: [
        {
          referencia: '2026-06',
          presentes: 3,
          presentesDiscipulos: 2,
          presentesVisitantes: 1,
          presentesGoe: 0,
          ausentes: 1,
        },
      ],
      discipulos: [{ referencia: '2026-06', quantidade: 4 }],
    },
  ],
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function mockApis(overrides?: (url: string) => Response | undefined) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    const url = String(input)
    const custom = overrides?.(url)
    if (custom) return custom
    if (url.includes('/painel/desempenho-discipulados')) return json(desempenho)
    if (url.includes('/painel/gerencia')) return json(resposta)
    throw new Error(`Requisição inesperada: ${url}`)
  })
}

describe('painel da gerência', () => {
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })
  it('mostra agregado, comparação, detalhe, desempenho e tabelas acessíveis', async () => {
    mockApis()
    render(<ManagerDashboard />)
    expect(await screen.findByText('Gerência Centro')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Desempenho dos discipulados' })).toBeInTheDocument()
    expect(screen.getAllByTestId('grafico').length).toBeGreaterThanOrEqual(3)
    await userEvent.click(screen.getAllByRole('button', { name: 'Dados' })[1])
    expect(screen.getByRole('table', { name: 'Resumo por discipulado' })).toBeInTheDocument()
    expect(screen.getAllByText('Discipulado A').length).toBeGreaterThan(0)
    expect(screen.getByRole('table', { name: 'Encontros não realizados' })).toBeInTheDocument()
    expect(screen.getByText('Líder doente')).toBeInTheDocument()
  }, 15_000)
  it('permite selecionar um discipulado inativo', async () => {
    mockApis()
    render(<ManagerDashboard />)
    await screen.findByText('Gerência Centro')
    const selects = screen.getAllByLabelText('Discipulado')
    await userEvent.click(selects[0])
    await userEvent.click(screen.getByRole('option', { name: /Discipulado Antigo/ }))
    expect(await screen.findByText('Inativo')).toBeInTheDocument()
  })
  it('aplica novo período e mostra erros Problem Details', async () => {
    let gerenciaCalls = 0
    const fetch = mockApis((url) => {
      if (url.includes('/painel/gerencia')) {
        gerenciaCalls += 1
        if (gerenciaCalls === 1) return json(resposta)
        return json({ detail: 'O gerente possui mais de uma gerência ativa.' }, 409)
      }
      return undefined
    })
    render(<ManagerDashboard />)
    await screen.findByText('Gerência Centro')
    const inicios = screen.getAllByLabelText(/Data inicial/)
    await userEvent.clear(inicios[0])
    await userEvent.type(inicios[0], '2026-02-01')
    await userEvent.click(screen.getAllByRole('button', { name: 'Aplicar' })[0])
    await waitFor(() => expect(fetch).toHaveBeenCalled())
    expect(await screen.findByText('O gerente possui mais de uma gerência ativa.')).toBeInTheDocument()
  })
})
