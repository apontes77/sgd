import { cleanup, screen } from '@testing-library/react'
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
    expect(screen.getByText('Preenchida')).toBeInTheDocument()
  })

  it('envia situacaoFicha na consulta de famílias', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(pagina))

    await familiaApi.listar(0, 20, { situacaoFicha: 'PREENCHIDA' })

    expect(String(fetchMock.mock.calls[0]?.[0])).toContain('situacaoFicha=PREENCHIDA')
  })
})
