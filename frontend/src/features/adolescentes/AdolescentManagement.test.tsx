import { cleanup, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import AdolescentManagement from '@/features/adolescentes/AdolescentManagement'
import { render } from '@/test/test-utils'

const emptyPage = { content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }

describe('gestão de discípulos', () => {
  beforeEach(() => {
    sessionStorage.setItem('sgd.access-token', 'token')
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    sessionStorage.clear()
  })

  it('exibe o total de adolescentes ativos quando nenhum discipulado está selecionado', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/discipulados')) {
        return new Response(
          JSON.stringify({
            ...emptyPage,
            content: [
              {
                id: 1,
                nome: 'Alpha',
                discipuladorNome: 'Maria',
                faixaEtaria: 'DE_15_MAIS',
                ativo: true,
                coLideres: [],
              },
              {
                id: 2,
                nome: 'Beta',
                discipuladorNome: 'João',
                faixaEtaria: 'DE_13_A_15',
                ativo: true,
                coLideres: [],
              },
            ],
            totalElements: 2,
            totalPages: 1,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      if (url.includes('/adolescentes?')) {
        return new Response(
          JSON.stringify({
            content: Array.from({ length: 100 }, (_, i) => ({
              id: i + 1,
              nome: `Adolescente ${i + 1}`,
              dataNascimento: '2010-01-01',
              categoria: 'DISCIPULO',
              anonimizado: false,
              discipuladoId: 1,
              ativo: true,
            })),
            page: 0,
            size: 100,
            totalElements: 142,
            totalPages: 2,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      throw new Error(`Requisição inesperada: ${url}`)
    })

    render(<AdolescentManagement />)

    expect(await screen.findByText('142 adolescentes')).toBeInTheDocument()
    expect(screen.getByText('Selecione um discipulado')).toBeInTheDocument()

    const listagem = fetchMock.mock.calls.find(([url]) => String(url).includes('/adolescentes?'))
    expect(String(listagem?.[0])).toContain('ativo=true')
    expect(String(listagem?.[0])).not.toContain('discipuladoId=')
  })

  it('pré-seleciona o discipulado inicial e lista filtrada', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/discipulados')) {
        return new Response(
          JSON.stringify({
            ...emptyPage,
            content: [
              {
                id: 1,
                nome: 'Alpha',
                discipuladorNome: 'Maria',
                faixaEtaria: 'DE_15_MAIS',
                ativo: true,
                coLideres: [],
              },
              {
                id: 2,
                nome: 'Beta',
                discipuladorNome: 'João',
                faixaEtaria: 'DE_13_A_15',
                ativo: true,
                coLideres: [],
              },
            ],
            totalElements: 2,
            totalPages: 1,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      if (url.includes('/adolescentes?')) {
        return new Response(
          JSON.stringify({
            content: [
              {
                id: 10,
                nome: 'Ana',
                dataNascimento: '2010-01-01',
                categoria: 'DISCIPULO',
                anonimizado: false,
                discipuladoId: 1,
                ativo: true,
              },
            ],
            page: 0,
            size: 100,
            totalElements: 1,
            totalPages: 1,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      if (url.includes('/alertas-goe')) {
        return new Response(JSON.stringify([]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      throw new Error(`Requisição inesperada: ${url}`)
    })

    render(<AdolescentManagement discipuladoInicial={1} />)

    expect(await screen.findByText('1 adolescente')).toBeInTheDocument()
    expect(screen.queryByText('Selecione um discipulado')).not.toBeInTheDocument()
    expect(await screen.findByText('Ana')).toBeInTheDocument()

    const listagem = fetchMock.mock.calls.find(([url]) => String(url).includes('/adolescentes?'))
    expect(String(listagem?.[0])).toContain('discipuladoId=1')
    expect(String(listagem?.[0])).toContain('ativo=true')
  })
})
