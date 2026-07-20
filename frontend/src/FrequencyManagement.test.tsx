import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import FrequencyManagement from './FrequencyManagement'

const encontro = { id: 10, discipuladoId: 1, data: '2026-06-01', situacao: 'REALIZADO', justificativa: null, criadoEm: new Date().toISOString() }
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

describe('registro de frequência', () => {
  afterEach(() => { cleanup(); vi.restoreAllMocks() })

  it('cria uma vez, abre sem erro e preserva participante de registro anterior', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      const method = init?.method ?? 'GET'
      if (url.includes('/adolescentes?')) return json({ content: [{ id: 1, nome: 'Ana' }], page: 0, size: 100, totalElements: 1, totalPages: 1 })
      if (url.endsWith('/encontros') && method === 'POST') return json(encontro, 201)
      if (url.includes('/encontros?')) return json([encontro])
      if (url.endsWith('/encontros/10/frequencias') && method === 'GET') return json([{ id: 20, encontroId: 10, adolescenteId: 2, adolescenteNome: 'Bia', situacao: 'PRESENTE', registradaEm: encontro.criadoEm }])
      if (url.endsWith('/encontros/10/frequencias') && method === 'PUT') return json([])
      if (url.endsWith('/encontros/10/visitantes') && method === 'PUT') return json({ quantidade: 0 })
      throw new Error(`Requisição inesperada: ${method} ${url}`)
    })

    render(<FrequencyManagement discipuladoId={1} />)
    await screen.findByRole('button', { name: 'Criar encontro' })
    expect(screen.queryByText('Registrar encontro não realizado')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Criar encontro' }))

    expect(await screen.findByText('Encontro criado.')).toBeInTheDocument()
    expect(await screen.findByText('Bia')).toBeInTheDocument()
    expect(screen.getByText('Registro anterior')).toBeInTheDocument()
    expect(screen.queryByText('Erro interno inesperado.')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Salvar chamada' }))
    expect(await screen.findByText('Chamada salva.')).toBeInTheDocument()
    const criacoes = fetchMock.mock.calls.filter(([url, init]) => String(url).endsWith('/encontros') && init?.method === 'POST')
    expect(criacoes).toHaveLength(1)
    const salvamento = fetchMock.mock.calls.find(([url, init]) => String(url).endsWith('/encontros/10/frequencias') && init?.method === 'PUT')
    expect(JSON.parse(String(salvamento?.[1]?.body)).frequencias.map((item: { adolescenteId:number }) => item.adolescenteId)).toEqual([1, 2])
    await waitFor(() => expect(fetchMock).toHaveBeenCalled())
  })

  it('permite ao administrador ou discipulador registrar encontro não realizado com justificativa', async () => {
    const cancelado = { ...encontro, situacao: 'CANCELADO', justificativa: 'Líder doente' }
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      const method = init?.method ?? 'GET'
      if (url.includes('/adolescentes?')) return json({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 })
      if (url.endsWith('/encontros') && method === 'POST') return json(cancelado, 201)
      if (url.includes('/encontros?')) return json([cancelado])
      throw new Error(`Requisição inesperada: ${method} ${url}`)
    })

    render(<FrequencyManagement discipuladoId={1} podeRegistrarNaoRealizacao />)
    expect(await screen.findByText('Registrar encontro não realizado')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText(/Justificativa da não realização/), 'Líder doente')
    await userEvent.click(screen.getByRole('button', { name: 'Registrar não realização' }))

    expect(await screen.findByText('Encontro não realizado registrado.')).toBeInTheDocument()
    expect(
      screen.getAllByRole('alert').some((alerta) => alerta.textContent?.includes('Líder doente')),
    ).toBe(true)
    const criacao = fetchMock.mock.calls.find(([url, init]) => String(url).endsWith('/encontros') && init?.method === 'POST')
    expect(JSON.parse(String(criacao?.[1]?.body))).toMatchObject({
      discipuladoId: 1,
      situacao: 'CANCELADO',
      justificativa: 'Líder doente',
    })
    expect(fetchMock.mock.calls.some(([url]) => String(url).endsWith('/encontros/10/frequencias'))).toBe(false)
  })
})
