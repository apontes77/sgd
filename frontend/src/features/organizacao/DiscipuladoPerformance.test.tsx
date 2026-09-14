import { cleanup, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import OrganizationManagement from '@/features/organizacao/OrganizationManagement'
import type { Discipulado, Gerencia, Pagina, Usuario } from '@/shared/api/types'
import { render } from '@/test/test-utils'

vi.mock('echarts-for-react', () => ({ default: () => <div data-testid="grafico" /> }))

const admin: Usuario = {
  id: 1,
  nome: 'Administrador',
  email: 'admin@sgd.local',
  ativo: true,
  perfis: ['ADMIN'],
}
const discipulador: Usuario = {
  id: 2,
  nome: 'Andressa Eliza',
  email: 'andressa@sgd.local',
  ativo: true,
  perfis: ['DISCIPULADOR'],
}
const gerencia: Gerencia = {
  id: 10,
  nome: 'Gerência Beatriz Ferreira',
  sexo: 'FEMININO',
  faixasEtarias: ['DE_09_A_11'],
  gerenteId: 1,
  ativo: true,
}
const discipulado: Discipulado = {
  id: 20,
  nome: 'Luz do mundo',
  sexo: 'FEMININO',
  faixaEtaria: 'DE_09_A_11',
  gerenciaId: 10,
  discipuladorId: 2,
  discipuladorNome: 'Andressa Eliza',
  ativo: true,
  coLideres: [],
}

const desempenho = {
  dataInicio: '2026-01-01',
  dataFim: '2026-06-30',
  discipulados: [
    {
      id: 20,
      nome: 'Luz do mundo',
      sexo: 'FEMININO',
      faixaEtaria: 'DE_09_A_11',
      gerenciaId: 10,
      gerenciaNome: 'Gerência Beatriz Ferreira',
      ativo: true,
      frequencia: [{ referencia: '2026-03', presentes: 4, ausentes: 1 }],
      discipulos: [
        { referencia: '2026-01', quantidade: 5 },
        { referencia: '2026-02', quantidade: 6 },
        { referencia: '2026-03', quantidade: 7 },
      ],
    },
    {
      id: 21,
      nome: 'Fonte de vida',
      sexo: 'MASCULINO',
      faixaEtaria: 'DE_15_MAIS',
      gerenciaId: 11,
      gerenciaNome: 'Gerência Norte',
      ativo: true,
      frequencia: [{ referencia: '2026-03', presentes: 2, ausentes: 2 }],
      discipulos: [{ referencia: '2026-03', quantidade: 3 }],
    },
  ],
}

function page<T>(content: T[], extras?: Partial<Pagina<T>>): Pagina<T> {
  return {
    content,
    page: 0,
    size: 100,
    totalElements: content.length,
    totalPages: 1,
    ...extras,
  }
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

describe('estrutura — desempenho dos discipulados', () => {
  beforeEach(() => {
    sessionStorage.setItem('sgd.access-token', 'token')
  })

  afterEach(() => {
    cleanup()
    sessionStorage.clear()
    vi.restoreAllMocks()
  })

  it('exibe a aba, oculta o botão de criar e permite trocar o discipulado', async () => {
    const user = userEvent.setup()
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/usuarios?')) return json(page([admin, discipulador]))
      if (url.includes('/gerencias?')) return json(page([gerencia]))
      if (url.includes('/discipulados?')) return json(page([discipulado]))
      if (url.includes('/painel/desempenho-discipulados')) return json(desempenho)
      throw new Error(`Requisição inesperada: ${url}`)
    })

    render(<OrganizationManagement />)
    expect(await screen.findByRole('button', { name: 'Nova gerência' })).toBeInTheDocument()

    await user.click(screen.getByRole('tab', { name: 'Desempenho dos discipulados' }))
    expect(await screen.findByRole('heading', { name: 'Desempenho dos discipulados' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Nova/ })).not.toBeInTheDocument()
    expect(screen.getAllByTestId('grafico').length).toBeGreaterThan(0)

    await user.click(screen.getByLabelText('Discipulado'))
    await user.click(screen.getByRole('option', { name: 'Fonte de vida' }))
    expect(await screen.findByText(/Presentes e ausentes — Fonte de vida/)).toBeInTheDocument()
  })

  it('mostra recortes por sexo, faixa etária e gerência', async () => {
    const user = userEvent.setup()
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/usuarios?')) return json(page([admin, discipulador]))
      if (url.includes('/gerencias?')) return json(page([gerencia]))
      if (url.includes('/discipulados?')) return json(page([discipulado]))
      if (url.includes('/painel/desempenho-discipulados')) return json(desempenho)
      throw new Error(`Requisição inesperada: ${url}`)
    })

    render(<OrganizationManagement />)
    await user.click(await screen.findByRole('tab', { name: 'Desempenho dos discipulados' }))
    await screen.findByRole('heading', { name: 'Desempenho dos discipulados' })

    await user.click(screen.getByRole('button', { name: 'Sexo' }))
    expect(screen.getByText(/Presentes e ausentes — Por sexo do discipulado/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Faixa etária' }))
    expect(screen.getByText(/Quantidade de pessoas — Por faixa etária/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Gerência' }))
    expect(screen.getByText('Totais do período por gerência')).toBeInTheDocument()
    expect(screen.getByText('Detalhe da gerência')).toBeInTheDocument()
    await user.click(screen.getAllByRole('button', { name: 'Dados' })[0])
    expect(screen.getByRole('table', { name: 'Totais por gerência' })).toBeInTheDocument()
    expect(
      within(screen.getByRole('table', { name: 'Totais por gerência' })).getByText('Gerência Norte'),
    ).toBeInTheDocument()
  })
})
