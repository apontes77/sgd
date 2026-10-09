import { cleanup, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { familiaApi } from '@/features/familia/api'
import FamilyDirectory from '@/features/familia/FamilyDirectory'
import { render } from '@/test/test-utils'

const pagina = {
  content: [
    {
      id: 1,
      adolescenteId: 10,
      adolescenteNome: 'Ana Silva',
      discipuladoId: 2,
      discipuladoNome: 'Alpha',
      situacaoFicha: 'PREENCHIDA',
      situacaoIgreja: 'NAO_CONSTA',
      situacaoPais: 'NAO_CONSTA',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
}

function jsonResponse(body: unknown) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('diretório de famílias', () => {
  beforeEach(() => {
    sessionStorage.setItem('sgd.access-token', 'token')
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    sessionStorage.clear()
    localStorage.clear()
  })

  it('exibe o filtro de situação da ficha na listagem', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/familias')) return jsonResponse(pagina)
      return jsonResponse({})
    })

    render(<FamilyDirectory />)

    expect(await screen.findByText('Ana Silva')).toBeInTheDocument()
    expect(screen.getByLabelText('Situação da ficha')).toBeInTheDocument()
    expect(screen.getByLabelText('Tipo de desafio')).toBeInTheDocument()
    expect(screen.getByLabelText('Intervenção')).toBeInTheDocument()
    expect(screen.getByText('Preenchida')).toBeInTheDocument()
  })

  it('envia situacaoFicha, tipo de desafio e intervenção na consulta de famílias', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(pagina))

    await familiaApi.listar(0, 20, {
      situacaoFicha: 'PREENCHIDA',
      tipoDesafio: 'FINANCEIRO',
      situacaoIntervencao: 'REGISTRADA',
    })

    const url = String(fetchMock.mock.calls[0]?.[0])
    expect(url).toContain('situacaoFicha=PREENCHIDA')
    expect(url).toContain('tipoDesafio=FINANCEIRO')
    expect(url).toContain('situacaoIntervencao=REGISTRADA')
  })

  it('consulta famílias pelo tipo de desafio e pelo preenchimento da intervenção', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/familias')) return jsonResponse(pagina)
      return jsonResponse({})
    })
    const user = userEvent.setup()

    render(<FamilyDirectory />)
    expect(await screen.findByText('Ana Silva')).toBeInTheDocument()

    await user.click(screen.getByLabelText('Tipo de desafio'))
    await user.click(screen.getByRole('option', { name: 'Desafio emocional' }))

    await user.click(screen.getByLabelText('Intervenção'))
    await user.click(screen.getByRole('option', { name: 'Não preenchidas' }))

    const urls = fetchMock.mock.calls.map((call) => String(call[0]))
    expect(urls.some((url) => url.includes('tipoDesafio=EMOCIONAL'))).toBe(true)
    expect(urls.some((url) => url.includes('situacaoIntervencao=NAO_CONSTA'))).toBe(true)
  })
})
