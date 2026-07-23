import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'

import FrequencyManagement from '@/features/frequencia/FrequencyManagement'

const agora = new Date()
const hoje = `${agora.getFullYear()}-${String(agora.getMonth() + 1).padStart(2, '0')}-${String(agora.getDate()).padStart(2, '0')}`
const encontro = {
  id: 10,
  discipuladoId: 1,
  data: hoje,
  situacao: 'REALIZADO',
  justificativa: null,
  criadoEm: new Date().toISOString(),
}
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

describe('registro de frequência', () => {
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('em data sem registro, "Houve discipulado" abre a lista de presenças e salva', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      const method = init?.method ?? 'GET'
      if (url.includes('/adolescentes?'))
        return json({ content: [{ id: 1, nome: 'Ana' }], page: 0, size: 100, totalElements: 1, totalPages: 1 })
      if (url.endsWith('/encontros') && method === 'POST') return json(encontro, 201)
      if (url.includes('/encontros?')) return json([])
      if (url.endsWith('/encontros/10/frequencias') && method === 'GET') return json([])
      if (url.endsWith('/encontros/10/frequencias') && method === 'PUT') return json([])
      throw new Error(`Requisição inesperada: ${method} ${url}`)
    })

    render(<FrequencyManagement discipuladoId={1} />)
    await userEvent.click(await screen.findByRole('button', { name: /Houve discipulado/i }))

    await userEvent.click(await screen.findByRole('button', { name: /Ana: ausente/i }))
    await userEvent.click(screen.getByRole('button', { name: 'Salvar frequência' }))
    expect(await screen.findByText('Frequência salva.')).toBeInTheDocument()

    const salvamento = fetchMock.mock.calls.find(
      ([url, init]) => String(url).endsWith('/encontros/10/frequencias') && init?.method === 'PUT',
    )
    expect(JSON.parse(String(salvamento?.[1]?.body)).frequencias).toEqual([{ adolescenteId: 1, situacao: 'PRESENTE' }])
  })

  it('"Não houve discipulado" exige justificativa e envia situacao NAO_REALIZADO', async () => {
    const naoRealizado = { ...encontro, situacao: 'NAO_REALIZADO', justificativa: 'Líder doente' }
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      const method = init?.method ?? 'GET'
      if (url.includes('/adolescentes?'))
        return json({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 })
      if (url.endsWith('/encontros') && method === 'POST') return json(naoRealizado, 201)
      if (url.includes('/encontros?')) return json([])
      throw new Error(`Requisição inesperada: ${method} ${url}`)
    })

    render(<FrequencyManagement discipuladoId={1} podeRegistrarNaoRealizacao />)
    await userEvent.click(await screen.findByRole('button', { name: /Não houve discipulado/i }))
    await userEvent.type(await screen.findByLabelText(/Justificativa/), 'Líder doente')
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar' }))

    expect(await screen.findByText('Registro de ausência confirmado.')).toBeInTheDocument()
    const criacao = fetchMock.mock.calls.find(
      ([url, init]) => String(url).endsWith('/encontros') && init?.method === 'POST',
    )
    expect(JSON.parse(String(criacao?.[1]?.body))).toMatchObject({
      discipuladoId: 1,
      situacao: 'NAO_REALIZADO',
      justificativa: 'Líder doente',
    })
    expect(fetchMock.mock.calls.some(([url]) => String(url).endsWith('/encontros/10/frequencias'))).toBe(false)
  })

  it('co-líder também vê a opção "Não houve discipulado"', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/adolescentes?'))
        return json({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 })
      if (url.includes('/encontros?')) return json([])
      throw new Error(`Requisição inesperada: ${url}`)
    })

    render(<FrequencyManagement discipuladoId={1} podeRegistrarNaoRealizacao />)
    expect(await screen.findByRole('button', { name: /^Houve discipulado/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Não houve discipulado/ })).toBeInTheDocument()
  })

  it('sem permissão de não realização, oculta "Não houve discipulado"', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/adolescentes?'))
        return json({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 })
      if (url.includes('/encontros?')) return json([])
      throw new Error(`Requisição inesperada: ${url}`)
    })

    render(<FrequencyManagement discipuladoId={1} />)
    expect(await screen.findByRole('button', { name: /^Houve discipulado/ })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Não houve discipulado/ })).not.toBeInTheDocument()
  })

  it('data com registro existente abre direto no modo correspondente', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      const method = init?.method ?? 'GET'
      if (url.includes('/adolescentes?'))
        return json({ content: [{ id: 1, nome: 'Ana' }], page: 0, size: 100, totalElements: 1, totalPages: 1 })
      if (url.includes('/encontros?')) return json([encontro])
      if (url.endsWith('/encontros/10/frequencias') && method === 'GET')
        return json([
          {
            id: 20,
            encontroId: 10,
            adolescenteId: 1,
            adolescenteNome: 'Ana',
            situacao: 'PRESENTE',
            registradaEm: encontro.criadoEm,
          },
        ])
      throw new Error(`Requisição inesperada: ${method} ${url}`)
    })

    render(<FrequencyManagement discipuladoId={1} />)
    expect(await screen.findByRole('button', { name: /Ana: presente/i })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Houve discipulado/i })).not.toBeInTheDocument()
  })

  it('adiciona um visitante como adolescente presente e o inclui no salvamento', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      const method = init?.method ?? 'GET'
      if (url.includes('/adolescentes?'))
        return json({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 })
      if (url.endsWith('/adolescentes') && method === 'POST')
        return json(
          { id: 99, nome: 'João Visitante', dataNascimento: '2011-05-04', discipuladoId: 1, ativo: true },
          201,
        )
      if (url.endsWith('/encontros') && method === 'POST') return json(encontro, 201)
      if (url.includes('/encontros?')) return json([])
      if (url.endsWith('/encontros/10/frequencias') && method === 'GET') return json([])
      if (url.endsWith('/encontros/10/frequencias') && method === 'PUT') return json([])
      throw new Error(`Requisição inesperada: ${method} ${url}`)
    })

    render(<FrequencyManagement discipuladoId={1} />)
    await userEvent.click(await screen.findByRole('button', { name: /Houve discipulado/i }))
    await userEvent.click(await screen.findByRole('button', { name: 'Adicionar visitante' }))
    await userEvent.type(screen.getByLabelText(/Nome/), 'João Visitante')
    fireEvent.change(screen.getByLabelText(/Data de nascimento/), { target: { value: '2011-05-04' } })
    await userEvent.click(screen.getByRole('button', { name: 'Adicionar' }))

    expect(await screen.findByText('João Visitante')).toBeInTheDocument()
    const criacaoVisitante = fetchMock.mock.calls.find(
      ([url, init]) => String(url).endsWith('/adolescentes') && init?.method === 'POST',
    )
    expect(JSON.parse(String(criacaoVisitante?.[1]?.body))).toMatchObject({
      nome: 'João Visitante',
      discipuladoId: 1,
      dataInicio: hoje,
    })

    await userEvent.click(await screen.findByRole('button', { name: 'Salvar frequência' }))
    expect(await screen.findByText('Frequência salva.')).toBeInTheDocument()
    const salvamento = fetchMock.mock.calls.find(
      ([url, init]) => String(url).endsWith('/encontros/10/frequencias') && init?.method === 'PUT',
    )
    expect(JSON.parse(String(salvamento?.[1]?.body)).frequencias).toEqual([{ adolescenteId: 99, situacao: 'PRESENTE' }])
  })

  it('alterna presença ao tocar na linha do adolescente', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      const method = init?.method ?? 'GET'
      if (url.includes('/adolescentes?'))
        return json({ content: [{ id: 1, nome: 'Ana' }], page: 0, size: 100, totalElements: 1, totalPages: 1 })
      if (url.endsWith('/encontros') && method === 'POST') return json(encontro, 201)
      if (url.includes('/encontros?')) return json([])
      if (url.endsWith('/encontros/10/frequencias') && method === 'GET') return json([])
      throw new Error(`Requisição inesperada: ${method} ${url}`)
    })

    render(<FrequencyManagement discipuladoId={1} />)
    await userEvent.click(await screen.findByRole('button', { name: /Houve discipulado/i }))
    const toggle = await screen.findByRole('button', { name: /Ana: ausente/i })
    expect(toggle).toHaveAttribute('aria-pressed', 'false')
    await userEvent.click(toggle.closest('.MuiPaper-root')!)
    expect(await screen.findByRole('button', { name: /Ana: presente/i })).toHaveAttribute('aria-pressed', 'true')
  })
})
