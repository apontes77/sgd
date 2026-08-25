import { cleanup, fireEvent, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'

import FrequencyManagement from '@/features/frequencia/FrequencyManagement'
import { render } from '@/test/test-utils'

const agora = new Date()
const hoje = `${agora.getFullYear()}-${String(agora.getMonth() + 1).padStart(2, '0')}-${String(agora.getDate()).padStart(2, '0')}`
const encontro = {
  id: 10,
  discipuladoId: 1,
  data: hoje,
  situacao: 'REALIZADO',
  justificativa: null,
  observacao: null as string | null,
  criadoEm: new Date().toISOString(),
  atualizadoEm: new Date().toISOString(),
  chamadaSalvaEm: null as string | null,
  fechamentoAutomatico: false,
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
      if (url.endsWith('/encontros/10') && method === 'PATCH') {
        const body = JSON.parse(String(init?.body ?? '{}')) as { observacao?: string | null }
        return json({ ...encontro, observacao: body.observacao ?? null })
      }
      if (url.endsWith('/encontros/10/frequencias') && method === 'GET') return json([])
      if (url.endsWith('/encontros/10/frequencias') && method === 'PUT') return json([])
      throw new Error(`Requisição inesperada: ${method} ${url}`)
    })

    const user = userEvent.setup({ delay: null })
    render(
      <FrequencyManagement
        discipuladoId={1}
        discipulado={{
          id: 1,
          nome: 'Discipulado Teste',
          sexo: 'MASCULINO',
          faixaEtaria: 'DE_15_MAIS',
          gerenciaId: 1,
          discipuladorId: 2,
          discipuladorNome: 'João Discipulador',
          coLideres: [{ id: 3, nome: 'Maria Co-líder', email: 'm@sgd.local', perfis: ['CO_LIDER'] }],
        }}
      />,
    )
    await user.click(await screen.findByRole('button', { name: /Houve discipulado/i }))

    expect(await screen.findByText(/Discipulador:/i)).toBeInTheDocument()
    expect(screen.getByText(/João Discipulador/)).toBeInTheDocument()
    await user.type(screen.getByLabelText(/Observação/), 'Chegou só no culto')
    await user.click(await screen.findByRole('button', { name: /Ana: ausente/i }))
    await user.click(screen.getByRole('button', { name: 'Salvar frequência' }))
    expect(await screen.findByText('Frequência salva.')).toBeInTheDocument()

    const listagem = fetchMock.mock.calls.find(([url]) => String(url).includes('/adolescentes?'))
    expect(String(listagem?.[0])).toContain('categoria=DISCIPULO')
    expect(String(listagem?.[0])).toContain('categoria=VISITANTE')
    expect(String(listagem?.[0])).toContain('DISCIPULO_GOE')

    const observacaoPatch = fetchMock.mock.calls.find(
      ([url, init]) => String(url).endsWith('/encontros/10') && init?.method === 'PATCH',
    )
    expect(JSON.parse(String(observacaoPatch?.[1]?.body))).toMatchObject({ observacao: 'Chegou só no culto' })

    const salvamento = fetchMock.mock.calls.find(
      ([url, init]) => String(url).endsWith('/encontros/10/frequencias') && init?.method === 'PUT',
    )
    expect(JSON.parse(String(salvamento?.[1]?.body)).frequencias).toEqual([{ adolescenteId: 1, situacao: 'PRESENTE' }])
  }, 15000)

  it('mostra GOE e visitantes em seção separada e salva só os marcados como presentes', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      const method = init?.method ?? 'GET'
      if (url.includes('/adolescentes?'))
        return json({
          content: [
            { id: 1, nome: 'Ana', categoria: 'DISCIPULO' },
            { id: 2, nome: 'Goe', categoria: 'DISCIPULO_GOE' },
            { id: 3, nome: 'Vis', categoria: 'VISITANTE' },
          ],
          page: 0,
          size: 100,
          totalElements: 3,
          totalPages: 1,
        })
      if (url.endsWith('/encontros') && method === 'POST') return json(encontro, 201)
      if (url.includes('/encontros?')) return json([])
      if (url.endsWith('/encontros/10') && method === 'PATCH') return json(encontro)
      if (url.endsWith('/encontros/10/frequencias') && method === 'GET') return json([])
      if (url.endsWith('/encontros/10/frequencias') && method === 'PUT') return json([])
      throw new Error(`Requisição inesperada: ${method} ${url}`)
    })

    const user = userEvent.setup({ delay: null })
    render(<FrequencyManagement discipuladoId={1} />)
    await user.click(await screen.findByRole('button', { name: /Houve discipulado/i }))
    expect(await screen.findByText('GOE e visitantes')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Ana: ausente/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Goe: não marcado/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Vis: não marcado/i })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Goe: não marcado/i }))
    expect(await screen.findByText(/Há alterações ainda não salvas/i)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Salvar frequência' }))
    expect(await screen.findByText('Frequência salva.')).toBeInTheDocument()
    const salvamento = fetchMock.mock.calls.find(
      ([url, init]) => String(url).endsWith('/encontros/10/frequencias') && init?.method === 'PUT',
    )
    expect(JSON.parse(String(salvamento?.[1]?.body)).frequencias).toEqual([
      { adolescenteId: 1, situacao: 'AUSENTE' },
      { adolescenteId: 2, situacao: 'PRESENTE' },
    ])
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

  it('após o prazo da sexta, bloqueia lançamento para não-admin', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/adolescentes?'))
        return json({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 })
      if (url.includes('/encontros?')) return json([])
      throw new Error(`Requisição inesperada: ${url}`)
    })

    render(<FrequencyManagement discipuladoId={1} />)
    const campo = await screen.findByLabelText('Data')
    fireEvent.change(campo, { target: { value: '2026-07-17' } })

    expect(await screen.findByText(/prazo para lançar a frequência desta sexta encerrou/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Houve discipulado/i })).not.toBeInTheDocument()
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
    expect(screen.getByText(/Registrado\/alterado em/i)).toBeInTheDocument()
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
      if (url.endsWith('/encontros/10') && method === 'PATCH') return json(encontro)
      if (url.endsWith('/encontros/10/frequencias') && method === 'GET') return json([])
      if (url.endsWith('/encontros/10/frequencias') && method === 'PUT') return json([])
      throw new Error(`Requisição inesperada: ${method} ${url}`)
    })

    const user = userEvent.setup({ delay: null })
    render(<FrequencyManagement discipuladoId={1} />)
    await user.click(await screen.findByRole('button', { name: /Houve discipulado/i }))
    await user.click(await screen.findByRole('button', { name: 'Adicionar visitante' }))
    fireEvent.change(screen.getByRole('textbox', { name: 'Nome' }), { target: { value: 'João Visitante' } })
    const dataNascimentoAdolescente = screen
      .getAllByLabelText(/Data de nascimento/)
      .find((el) => el.hasAttribute('required'))
    expect(dataNascimentoAdolescente).toBeTruthy()
    fireEvent.change(dataNascimentoAdolescente!, { target: { value: '2011-05-04' } })
    await user.click(screen.getByRole('button', { name: 'Adicionar' }))

    expect(await screen.findByText('João Visitante')).toBeInTheDocument()
    const criacaoVisitante = fetchMock.mock.calls.find(
      ([url, init]) => String(url).endsWith('/adolescentes') && init?.method === 'POST',
    )
    expect(JSON.parse(String(criacaoVisitante?.[1]?.body))).toMatchObject({
      nome: 'João Visitante',
      discipuladoId: 1,
      dataInicio: hoje,
      categoria: 'VISITANTE',
    })
    expect(JSON.parse(String(criacaoVisitante?.[1]?.body)).familia).toBeUndefined()

    await user.click(await screen.findByRole('button', { name: 'Salvar frequência' }))
    expect(await screen.findByText('Frequência salva.')).toBeInTheDocument()
    const salvamento = fetchMock.mock.calls.find(
      ([url, init]) => String(url).endsWith('/encontros/10/frequencias') && init?.method === 'PUT',
    )
    expect(JSON.parse(String(salvamento?.[1]?.body)).frequencias).toEqual([{ adolescenteId: 99, situacao: 'PRESENTE' }])
  }, 15000)

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

  it('ADMIN vê banner e botão Modificar frequência em fechamento automático', async () => {
    const fechado = {
      ...encontro,
      situacao: 'NAO_REALIZADO' as const,
      justificativa: 'discipulador ou colider não registraram a frequência',
      fechamentoAutomatico: true,
    }
    const realizado = { ...fechado, situacao: 'REALIZADO' as const, justificativa: null }
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      const method = init?.method ?? 'GET'
      if (url.includes('/adolescentes?'))
        return json({ content: [{ id: 1, nome: 'Ana' }], page: 0, size: 100, totalElements: 1, totalPages: 1 })
      if (url.includes('/encontros?')) return json([fechado])
      if (url.endsWith('/encontros/10') && method === 'PATCH') return json(realizado)
      if (url.endsWith('/encontros/10/frequencias') && method === 'GET') return json([])
      throw new Error(`Requisição inesperada: ${method} ${url}`)
    })

    render(<FrequencyManagement discipuladoId={1} podeAdministrar podeRegistrarNaoRealizacao />)
    expect(await screen.findByText(/não lançou a frequência no prazo/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Modificar frequência' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Excluir frequência' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Modificar frequência' }))
    expect(await screen.findByText(/Frequência liberada para preenchimento/i)).toBeInTheDocument()
    expect(screen.getByText(/não lançou a frequência no prazo/i)).toBeInTheDocument()
  })

  it('ADMIN exclui frequência após confirmação e libera a data', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      const method = init?.method ?? 'GET'
      if (url.includes('/adolescentes?'))
        return json({ content: [{ id: 1, nome: 'Ana' }], page: 0, size: 100, totalElements: 1, totalPages: 1 })
      if (url.includes('/encontros?')) {
        const jaExcluiu = fetchMock.mock.calls.some(
          ([u, i]) => String(u).endsWith('/encontros/10') && i?.method === 'DELETE',
        )
        return json(jaExcluiu ? [] : [encontro])
      }
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
      if (url.endsWith('/encontros/10') && method === 'DELETE') return new Response(null, { status: 204 })
      throw new Error(`Requisição inesperada: ${method} ${url}`)
    })

    render(<FrequencyManagement discipuladoId={1} podeAdministrar />)
    expect(await screen.findByRole('button', { name: /Ana: presente/i })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Excluir frequência' }))
    expect(await screen.findByRole('heading', { name: 'Excluir frequência?' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }))
    expect(await screen.findByText(/Frequência excluída/i)).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: /Houve discipulado/i })).toBeInTheDocument()
  })

  it('líder não vê Excluir frequência nem Modificar frequência', async () => {
    const fechado = {
      ...encontro,
      situacao: 'NAO_REALIZADO' as const,
      justificativa: 'discipulador ou colider não registraram a frequência',
      fechamentoAutomatico: true,
    }
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/adolescentes?'))
        return json({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 })
      if (url.includes('/encontros?')) return json([fechado])
      throw new Error(`Requisição inesperada: ${url}`)
    })

    render(<FrequencyManagement discipuladoId={1} podeRegistrarNaoRealizacao />)
    expect(await screen.findByText(/não lançou a frequência no prazo/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Modificar frequência' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Excluir frequência' })).not.toBeInTheDocument()
  })
})
