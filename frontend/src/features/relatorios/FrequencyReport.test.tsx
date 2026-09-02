import { cleanup, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { RelatorioPeriodoResponse } from '@/features/relatorios/api'
import FrequencyReport from '@/features/relatorios/FrequencyReport'
import type { Usuario } from '@/shared/api/types'
import { render } from '@/test/test-utils'

const adminUser: Usuario = {
  id: 1,
  nome: 'Admin',
  email: 'admin@sgd.local',
  ativo: true,
  perfis: ['ADMIN'],
}

const gerenteUser: Usuario = {
  id: 2,
  nome: 'Gerente',
  email: 'gerente@sgd.local',
  ativo: true,
  perfis: ['GERENTE'],
}

const relatorio: RelatorioPeriodoResponse = {
  dataInicio: '2026-07-21',
  dataFim: '2026-07-21',
  emitidoEm: '2026-07-21T22:00:00Z',
  relatorios: [
    {
      encontroId: 10,
      data: '2026-07-21',
      situacao: 'REALIZADO',
      justificativa: null,
      fechamentoAutomatico: false,
      gerencia: { id: 1, nome: 'Centro' },
      discipulado: { id: 2, nome: 'Alpha', sexo: 'MASCULINO' },
      discipulador: { id: 3, nome: 'Líder Alpha' },
      coLideres: [{ id: 4, nome: 'Co-líder Alpha' }],
      participantes: [
        { adolescenteId: 5, nome: 'Ana', telefone: '(11) 97777-1111', situacao: 'AUSENTE' },
        { adolescenteId: 6, nome: 'Bia', telefone: null, situacao: 'PRESENTE' },
      ],
      visitantes: 3,
      goe: 0,
      resumo: { presentes: 1, ausentes: 1, participantes: 2, visitantes: 3, goe: 0, percentualPresenca: 50 },
    },
    {
      encontroId: 11,
      data: '2026-07-21',
      situacao: 'REALIZADO',
      justificativa: null,
      fechamentoAutomatico: false,
      gerencia: { id: 1, nome: 'Centro' },
      discipulado: { id: 7, nome: 'Beta', sexo: 'FEMININO' },
      discipulador: { id: 8, nome: 'Líder Beta' },
      coLideres: [],
      participantes: [],
      visitantes: 0,
      goe: 0,
      resumo: { presentes: 0, ausentes: 0, participantes: 0, visitantes: 0, goe: 0, percentualPresenca: 0 },
    },
    {
      encontroId: 12,
      data: '2026-07-19',
      situacao: 'NAO_REALIZADO',
      justificativa: 'Problema de saúde',
      fechamentoAutomatico: false,
      gerencia: { id: 1, nome: 'Centro' },
      discipulado: { id: 2, nome: 'Alpha', sexo: 'MASCULINO' },
      discipulador: { id: 3, nome: 'Líder Alpha' },
      coLideres: [],
      participantes: [],
      visitantes: 0,
      goe: 0,
      resumo: { presentes: 0, ausentes: 0, participantes: 0, visitantes: 0, goe: 0, percentualPresenca: 0 },
    },
  ],
}

describe('relatório diário de frequência', () => {
  beforeEach(() => {
    sessionStorage.setItem('sgd.access-token', 'token')
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    sessionStorage.clear()
  })

  it('consulta o período, omite a lista nominal e imprime o resultado', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/discipulados')) {
        return new Response(
          JSON.stringify({
            content: [],
            page: 0,
            size: 100,
            totalElements: 0,
            totalPages: 0,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      return new Response(
        JSON.stringify({
          ...relatorio,
          dataInicio: '2026-07-20',
          dataFim: '2026-07-21',
          relatorios: relatorio.relatorios.map((item) => ({ ...item, participantes: [] })),
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      )
    })
    const printMock = vi.spyOn(window, 'print').mockImplementation(() => undefined)
    render(<FrequencyReport currentUser={adminUser} />)

    const imprimir = screen.getByRole('button', { name: 'Imprimir / salvar como PDF' })
    const exportar = screen.getByRole('button', { name: 'Exportar Excel' })
    expect(imprimir).toBeDisabled()
    expect(exportar).toBeDisabled()
    expect(await screen.findByLabelText(/^Discipulado/)).toBeInTheDocument()
    const dataInicial = screen.getByLabelText(/^Data inicial/)
    const dataFinal = screen.getByLabelText(/^Data final/)
    await userEvent.clear(dataInicial)
    await userEvent.type(dataInicial, '2026-07-20')
    await userEvent.clear(dataFinal)
    await userEvent.type(dataFinal, '2026-07-21')
    await userEvent.click(screen.getByRole('button', { name: 'Consultar' }))

    expect(await screen.findAllByText('Relatório de frequência')).not.toHaveLength(0)
    expect(screen.getAllByText('Lista nominal disponível na consulta de um único dia.')).toHaveLength(2)
    expect(screen.queryByText('Ana')).not.toBeInTheDocument()
    expect(screen.queryByText('Bia')).not.toBeInTheDocument()
    expect(screen.queryByRole('table', { name: 'Frequência do Alpha em 21/07/2026' })).not.toBeInTheDocument()
    expect(screen.getByText('Registro de ausência do discipulado')).toBeInTheDocument()
    expect(screen.getByText(/Problema de saúde/)).toBeInTheDocument()
    expect(screen.getByText('Não houve discipulado')).toBeInTheDocument()
    expect(screen.queryByText(/Encontro:/)).not.toBeInTheDocument()
    expect(screen.queryByText('#10')).not.toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/relatorios/frequencia?dataInicio=2026-07-20&dataFim=2026-07-21',
      expect.anything(),
    )

    await userEvent.click(imprimir)
    expect(printMock).toHaveBeenCalledOnce()
    expect(exportar).toBeEnabled()
  })

  it('lista adolescentes quando o período é um único dia', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/discipulados')) {
        return new Response(
          JSON.stringify({
            content: [],
            page: 0,
            size: 100,
            totalElements: 0,
            totalPages: 0,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      return new Response(JSON.stringify(relatorio), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })
    render(<FrequencyReport currentUser={adminUser} />)

    const dataInicial = screen.getByLabelText(/^Data inicial/)
    const dataFinal = screen.getByLabelText(/^Data final/)
    await userEvent.clear(dataInicial)
    await userEvent.type(dataInicial, '2026-07-21')
    await userEvent.clear(dataFinal)
    await userEvent.type(dataFinal, '2026-07-21')
    await userEvent.click(screen.getByRole('button', { name: 'Consultar' }))

    expect(await screen.findByRole('table', { name: 'Frequência do Alpha em 21/07/2026' })).toBeInTheDocument()
    expect(screen.getByRole('table', { name: 'Frequência do Beta em 21/07/2026' })).toBeInTheDocument()
    expect(screen.getByText('Ana')).toBeInTheDocument()
    expect(screen.getByText('Bia')).toBeInTheDocument()
    expect(screen.getByText('(11) 97777-1111')).toBeInTheDocument()
    expect(screen.getByText('Não informado')).toBeInTheDocument()
    expect(screen.getByText('Ausente')).toHaveClass('frequencia-ausente')
    expect(screen.getByText('Presente')).toHaveClass('frequencia-presente')
    expect(screen.getByText('Nenhuma frequência registrada neste encontro.')).toBeInTheDocument()
    expect(screen.getAllByText('GOE').length).toBeGreaterThan(0)
  })

  it('informa quando não há encontros e mantém a impressão desabilitada', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/discipulados')) {
        return new Response(
          JSON.stringify({
            content: [],
            page: 0,
            size: 100,
            totalElements: 0,
            totalPages: 0,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      return new Response(JSON.stringify({ ...relatorio, relatorios: [] }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })
    render(<FrequencyReport currentUser={adminUser} />)

    await userEvent.click(screen.getByRole('button', { name: 'Consultar' }))

    expect(await screen.findByText(/Não há registros de frequência no seu escopo/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Imprimir / salvar como PDF' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Exportar Excel' })).toBeDisabled()
  })

  it('permite ao gerente filtrar o relatório por discipulado', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/discipulados')) {
        return new Response(
          JSON.stringify({
            content: [
              {
                id: 2,
                nome: 'Alpha',
                sexo: 'MASCULINO',
                faixaEtaria: 'DE_15_MAIS',
                gerenciaId: 1,
                discipuladorId: 3,
                discipuladorNome: 'Líder Alpha',
                coLideres: [],
              },
              {
                id: 7,
                nome: 'Beta',
                sexo: 'FEMININO',
                faixaEtaria: 'DE_13_A_15',
                gerenciaId: 1,
                discipuladorId: 8,
                discipuladorNome: 'Líder Beta',
                coLideres: [],
              },
            ],
            page: 0,
            size: 100,
            totalElements: 2,
            totalPages: 1,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      return new Response(JSON.stringify({ ...relatorio, relatorios: [relatorio.relatorios[0]] }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    render(<FrequencyReport currentUser={gerenteUser} />)

    const campo = await screen.findByPlaceholderText('Pesquisar discipulado ou discipulador')
    await userEvent.clear(campo)
    await userEvent.type(campo, 'Líder Alpha')
    await userEvent.click(await screen.findByRole('option', { name: /Líder Alpha/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Consultar' }))

    expect(await screen.findByRole('table', { name: 'Frequência do Alpha em 21/07/2026' })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/api/v1/relatorios/frequencia?'), expect.anything())
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('discipuladoId=2'))).toBe(true)
  })

  it('permite buscar discipulado por parte do nome do discipulador no relatório', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/discipulados')) {
        return new Response(
          JSON.stringify({
            content: [
              {
                id: 2,
                nome: 'Alpha',
                sexo: 'MASCULINO',
                faixaEtaria: 'DE_15_MAIS',
                gerenciaId: 1,
                discipuladorId: 3,
                discipuladorNome: 'Maria Silva',
                coLideres: [],
              },
              {
                id: 7,
                nome: 'Beta',
                sexo: 'FEMININO',
                faixaEtaria: 'DE_13_A_15',
                gerenciaId: 1,
                discipuladorId: 8,
                discipuladorNome: 'João Costa',
                coLideres: [],
              },
            ],
            page: 0,
            size: 100,
            totalElements: 2,
            totalPages: 1,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      return new Response(JSON.stringify({ ...relatorio, relatorios: [relatorio.relatorios[1]] }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    render(<FrequencyReport currentUser={adminUser} />)

    const campo = await screen.findByPlaceholderText('Pesquisar discipulado ou discipulador')
    await userEvent.type(campo, 'João')
    expect(await screen.findByRole('option', { name: /João Costa/ })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: /Maria Silva/ })).not.toBeInTheDocument()
  })

  it('exibe aviso persistente quando o encontro veio de fechamento automático', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/discipulados')) {
        return new Response(JSON.stringify({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      return new Response(
        JSON.stringify({
          dataInicio: '2026-07-17',
          dataFim: '2026-07-17',
          emitidoEm: '2026-07-21T22:00:00Z',
          relatorios: [
            {
              ...relatorio.relatorios[0],
              encontroId: 13,
              data: '2026-07-17',
              fechamentoAutomatico: true,
              participantes: [{ adolescenteId: 5, nome: 'Ana', telefone: null, situacao: 'PRESENTE' }],
              resumo: { presentes: 1, ausentes: 0, participantes: 1, visitantes: 0, goe: 0, percentualPresenca: 100 },
            },
          ],
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      )
    })

    render(<FrequencyReport currentUser={adminUser} />)
    const dataInicial = await screen.findByLabelText(/^Data inicial/)
    await userEvent.clear(dataInicial)
    await userEvent.type(dataInicial, '2026-07-17')
    const dataFinal = screen.getByLabelText(/^Data final/)
    await userEvent.clear(dataFinal)
    await userEvent.type(dataFinal, '2026-07-17')
    await userEvent.click(screen.getByRole('button', { name: 'Consultar' }))

    expect(await screen.findByText(/não lançou a frequência no prazo/i)).toBeInTheDocument()
    expect(screen.getByText('Ana')).toBeInTheDocument()
  })
})
