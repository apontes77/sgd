import { cleanup, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import LeadershipAttendance from '@/features/lideranca/LeadershipAttendance'
import { render } from '@/test/test-utils'

const grade = {
  id: null,
  data: '2026-08-14',
  observacaoGeral: null,
  discipulados: [
    {
      discipuladoId: 10,
      discipuladoNome: 'Alpha',
      sexo: 'MASCULINO',
      gerenciaNome: 'Centro',
      observacao: null,
      presencas: [
        { usuarioId: 1, nome: 'Líder Alpha', papel: 'DISCIPULADOR', situacao: null },
        { usuarioId: 2, nome: 'Co Alpha', papel: 'CO_LIDER', situacao: null },
      ],
    },
    {
      discipuladoId: 11,
      discipuladoNome: 'Beta',
      sexo: 'FEMININO',
      gerenciaNome: 'Centro',
      observacao: null,
      presencas: [{ usuarioId: 3, nome: 'Líder Beta', papel: 'DISCIPULADOR', situacao: null }],
    },
  ],
}

describe('chamada de liderança', () => {
  beforeEach(() => {
    sessionStorage.setItem('sgd.access-token', 'token')
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.setSystemTime(new Date('2026-08-14T15:00:00'))
  })

  afterEach(() => {
    cleanup()
    vi.useRealTimers()
    vi.restoreAllMocks()
    sessionStorage.clear()
  })

  it('carrega a grade e salva presenças com observações', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      if (url.includes('/chamadas-lideranca') && (!init?.method || init.method === 'GET')) {
        return new Response(JSON.stringify(grade), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      if (url.includes('/chamadas-lideranca') && init?.method === 'PUT') {
        const body = JSON.parse(String(init.body)) as {
          observacaoGeral: string
          discipulados: { observacao: string; presencas: { situacao: string }[] }[]
        }
        return new Response(
          JSON.stringify({
            id: 1,
            data: '2026-08-14',
            observacaoGeral: body.observacaoGeral,
            discipulados: [
              {
                ...grade.discipulados[0],
                observacao: body.discipulados[0].observacao,
                presencas: grade.discipulados[0].presencas.map((p, index) => ({
                  ...p,
                  situacao: body.discipulados[0].presencas[index].situacao,
                })),
              },
            ],
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      return new Response('not found', { status: 404 })
    })

    render(<LeadershipAttendance />)

    expect(await screen.findByRole('heading', { name: 'Chamada de liderança' })).toBeInTheDocument()
    expect(await screen.findByText('Alpha')).toBeInTheDocument()
    expect(screen.getByText('Beta')).toBeInTheDocument()

    await user.click(screen.getByLabelText('Sexo'))
    await user.click(await screen.findByRole('option', { name: 'Masculino' }))
    expect(screen.getByText('Alpha')).toBeInTheDocument()
    expect(screen.queryByText('Beta')).not.toBeInTheDocument()

    expect(screen.getByText('Líder Alpha')).toBeInTheDocument()
    expect(screen.getByText('Co Alpha')).toBeInTheDocument()

    const presentes = screen.getAllByRole('button', { name: 'Presente' })
    const ausentes = screen.getAllByRole('button', { name: 'Ausente' })
    await user.click(presentes[0])
    await user.click(ausentes[1])
    await user.type(screen.getByLabelText('Observação do discipulado'), 'Chegou atrasado')
    await user.type(screen.getByLabelText('Observação geral'), 'Culto tranquilo')
    await user.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    await waitFor(() => expect(screen.getByText('Chamada de liderança salva.')).toBeInTheDocument())
    const putCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')
    expect(putCall).toBeTruthy()
    const payload = JSON.parse(String(putCall?.[1]?.body))
    expect(payload.observacaoGeral).toBe('Culto tranquilo')
    expect(payload.discipulados).toHaveLength(1)
    expect(payload.discipulados[0].discipuladoId).toBe(10)
    expect(payload.discipulados[0].observacao).toBe('Chegou atrasado')
    expect(payload.discipulados[0].presencas).toEqual([
      { usuarioId: 1, papel: 'DISCIPULADOR', situacao: 'PRESENTE' },
      { usuarioId: 2, papel: 'CO_LIDER', situacao: 'AUSENTE' },
    ])
  })

  it('salva somente os líderes já marcados', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      if (url.includes('/chamadas-lideranca') && (!init?.method || init.method === 'GET')) {
        return new Response(JSON.stringify(grade), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      if (url.includes('/chamadas-lideranca') && init?.method === 'PUT') {
        return new Response(JSON.stringify({ ...grade, id: 1 }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      return new Response('not found', { status: 404 })
    })

    render(<LeadershipAttendance />)
    expect(await screen.findByText('Líder Alpha')).toBeInTheDocument()

    await user.click(screen.getAllByRole('button', { name: 'Presente' })[0])
    await user.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    await waitFor(() => expect(screen.getByText('Chamada de liderança salva.')).toBeInTheDocument())
    const putCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')
    const payload = JSON.parse(String(putCall?.[1]?.body))
    expect(payload.discipulados).toEqual([
      {
        discipuladoId: 10,
        observacao: null,
        presencas: [{ usuarioId: 1, papel: 'DISCIPULADOR', situacao: 'PRESENTE' }],
      },
    ])
  })

  it('filtra a grade por parte do nome do discipulador ou co-líder', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      if (url.includes('/chamadas-lideranca') && (!init?.method || init.method === 'GET')) {
        return new Response(JSON.stringify(grade), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      return new Response('not found', { status: 404 })
    })

    render(<LeadershipAttendance />)
    expect(await screen.findByText('Alpha')).toBeInTheDocument()
    expect(screen.getByText('Beta')).toBeInTheDocument()

    await user.type(screen.getByPlaceholderText('Pesquisar discipulador ou co-líder'), 'Co Alp')
    expect(screen.getByText('Alpha')).toBeInTheDocument()
    expect(screen.getByText('Co Alpha')).toBeInTheDocument()
    expect(screen.queryByText('Beta')).not.toBeInTheDocument()
    expect(screen.queryByText('Líder Beta')).not.toBeInTheDocument()
  })

  it('pede confirmação para atualizar chamada já salva e envia o flag ao confirmar', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    const gradeComRegistro = {
      id: 1,
      data: '2026-08-14',
      observacaoGeral: null,
      discipulados: [
        {
          discipuladoId: 10,
          discipuladoNome: 'Alpha',
          sexo: 'MASCULINO',
          gerenciaNome: 'Centro',
          observacao: null,
          presencas: [
            {
              usuarioId: 1,
              nome: 'Líder Alpha',
              papel: 'DISCIPULADOR',
              situacao: 'PRESENTE',
              registroDoDia: { discipuladoId: 10, discipuladoNome: 'Alpha', situacao: 'PRESENTE' },
            },
          ],
        },
      ],
    }
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      if (url.includes('/chamadas-lideranca') && (!init?.method || init.method === 'GET')) {
        return new Response(JSON.stringify(gradeComRegistro), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      if (url.includes('/chamadas-lideranca') && init?.method === 'PUT') {
        return new Response(
          JSON.stringify({
            ...gradeComRegistro,
            discipulados: [
              {
                ...gradeComRegistro.discipulados[0],
                presencas: [
                  {
                    ...gradeComRegistro.discipulados[0].presencas[0],
                    situacao: 'AUSENTE',
                    registroDoDia: { discipuladoId: 10, discipuladoNome: 'Alpha', situacao: 'AUSENTE' },
                  },
                ],
              },
            ],
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      return new Response('not found', { status: 404 })
    })

    render(<LeadershipAttendance />)
    expect(await screen.findByText('Chamada já salva nesta sexta como Presente')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Ausente' }))
    await user.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    expect(await screen.findByRole('dialog', { name: 'Chamada já salva' })).toBeInTheDocument()
    expect(
      screen.getByText(
        'Este discipulador/co-líder já teve chamada salva. Tem certeza que quer atualizar essa chamada?',
      ),
    ).toBeInTheDocument()
    expect(screen.getByText(/Líder Alpha — Presente no discipulado Alpha/)).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false)

    await user.click(screen.getByRole('button', { name: 'Atualizar chamada' }))
    await waitFor(() => expect(screen.getByText('Chamada de liderança salva.')).toBeInTheDocument())
    const putCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')
    const payload = JSON.parse(String(putCall?.[1]?.body))
    expect(payload.confirmarAtualizacao).toBe(true)
    expect(payload.discipulados[0].presencas).toEqual([{ usuarioId: 1, papel: 'DISCIPULADOR', situacao: 'AUSENTE' }])
  })

  it('mantém o registro anterior quando o admin recusa a atualização', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    const gradeComRegistro = {
      id: 1,
      data: '2026-08-14',
      observacaoGeral: null,
      discipulados: [
        {
          discipuladoId: 10,
          discipuladoNome: 'Alpha',
          sexo: 'MASCULINO',
          gerenciaNome: 'Centro',
          observacao: null,
          presencas: [
            {
              usuarioId: 1,
              nome: 'Líder Alpha',
              papel: 'DISCIPULADOR',
              situacao: 'PRESENTE',
              registroDoDia: { discipuladoId: 10, discipuladoNome: 'Alpha', situacao: 'PRESENTE' },
            },
            { usuarioId: 2, nome: 'Co Alpha', papel: 'CO_LIDER', situacao: null },
          ],
        },
      ],
    }
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      if (url.includes('/chamadas-lideranca') && (!init?.method || init.method === 'GET')) {
        return new Response(JSON.stringify(gradeComRegistro), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      if (url.includes('/chamadas-lideranca') && init?.method === 'PUT') {
        return new Response(JSON.stringify(gradeComRegistro), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      return new Response('not found', { status: 404 })
    })

    render(<LeadershipAttendance />)
    expect(await screen.findByText('Líder Alpha')).toBeInTheDocument()

    await user.click(screen.getAllByRole('button', { name: 'Ausente' })[0])
    await user.click(screen.getAllByRole('button', { name: 'Presente' })[1])
    await user.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    expect(await screen.findByRole('dialog', { name: 'Chamada já salva' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Manter o que já estava' }))
    await waitFor(() => expect(screen.getByText('Chamada de liderança salva.')).toBeInTheDocument())

    const putCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')
    const payload = JSON.parse(String(putCall?.[1]?.body))
    expect(payload.confirmarAtualizacao).toBeUndefined()
    expect(payload.discipulados[0].presencas).toEqual([{ usuarioId: 2, papel: 'CO_LIDER', situacao: 'PRESENTE' }])
  })

  it('desabilita o salvar do card até haver alteração naquele discipulado', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      if (url.includes('/chamadas-lideranca') && (!init?.method || init.method === 'GET')) {
        return new Response(JSON.stringify(grade), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      return new Response('not found', { status: 404 })
    })

    render(<LeadershipAttendance />)
    expect(await screen.findByText('Líder Alpha')).toBeInTheDocument()

    expect(screen.getByRole('button', { name: 'Salvar Alpha' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Salvar Beta' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Salvar' })).toBeDisabled()
  })

  it('não deixa o salvar global ativo depois de gravar só um card', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      if (url.includes('/chamadas-lideranca') && (!init?.method || init.method === 'GET')) {
        return new Response(JSON.stringify(grade), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      if (url.includes('/chamadas-lideranca') && init?.method === 'PUT') {
        return new Response(JSON.stringify({ ...grade, id: 1 }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      return new Response('not found', { status: 404 })
    })

    render(<LeadershipAttendance />)
    expect(await screen.findByText('Líder Alpha')).toBeInTheDocument()

    await user.click(screen.getAllByRole('button', { name: 'Presente' })[0])
    await user.click(screen.getByRole('button', { name: 'Salvar Alpha' }))

    await waitFor(() => expect(screen.getByText('Chamada de liderança salva.')).toBeInTheDocument())
    expect(screen.getByRole('button', { name: 'Salvar Alpha' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Salvar' })).toBeDisabled()
  })

  it('salva somente o discipulado do card e preserva marcações locais dos outros', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      if (url.includes('/chamadas-lideranca') && (!init?.method || init.method === 'GET')) {
        return new Response(JSON.stringify(grade), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      if (url.includes('/chamadas-lideranca') && init?.method === 'PUT') {
        const body = JSON.parse(String(init.body)) as {
          observacaoGeral: string | null
          discipulados: { discipuladoId: number; observacao: string | null; presencas: { situacao: string }[] }[]
        }
        return new Response(
          JSON.stringify({
            id: 1,
            data: '2026-08-14',
            observacaoGeral: body.observacaoGeral,
            discipulados: grade.discipulados.map((d) => {
              const enviado = body.discipulados.find((item) => item.discipuladoId === d.discipuladoId)
              if (!enviado) return d
              return {
                ...d,
                observacao: enviado.observacao,
                presencas: d.presencas.map((p, index) => ({
                  ...p,
                  situacao: enviado.presencas[index]?.situacao ?? p.situacao,
                  registroDoDia: enviado.presencas[index]
                    ? {
                        discipuladoId: d.discipuladoId,
                        discipuladoNome: d.discipuladoNome,
                        situacao: enviado.presencas[index].situacao,
                      }
                    : p.registroDoDia,
                })),
              }
            }),
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      return new Response('not found', { status: 404 })
    })

    render(<LeadershipAttendance />)
    expect(await screen.findByText('Líder Alpha')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Observação geral'), 'Rascunho geral')
    await user.click(screen.getAllByRole('button', { name: 'Presente' })[0])
    await user.click(screen.getAllByRole('button', { name: 'Ausente' })[2])
    await user.click(screen.getByRole('button', { name: 'Salvar Alpha' }))

    await waitFor(() => expect(screen.getByText('Chamada de liderança salva.')).toBeInTheDocument())
    const putCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')
    const payload = JSON.parse(String(putCall?.[1]?.body))
    expect(payload.observacaoGeral).toBeNull()
    expect(payload.discipulados).toEqual([
      {
        discipuladoId: 10,
        observacao: null,
        presencas: [{ usuarioId: 1, papel: 'DISCIPULADOR', situacao: 'PRESENTE' }],
      },
    ])

    expect(screen.getByRole('button', { name: 'Salvar Alpha' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Salvar Beta' })).toBeEnabled()
    expect(screen.getByLabelText('Observação geral')).toHaveValue('Rascunho geral')
    expect(screen.getByRole('button', { name: 'Salvar alterações' })).toBeEnabled()
  })

  it('pede confirmação ao salvar pelo card quando a chamada já estava salva', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    const gradeComRegistro = {
      id: 1,
      data: '2026-08-14',
      observacaoGeral: null,
      discipulados: [
        {
          discipuladoId: 10,
          discipuladoNome: 'Alpha',
          sexo: 'MASCULINO',
          gerenciaNome: 'Centro',
          observacao: null,
          presencas: [
            {
              usuarioId: 1,
              nome: 'Líder Alpha',
              papel: 'DISCIPULADOR',
              situacao: 'PRESENTE',
              registroDoDia: { discipuladoId: 10, discipuladoNome: 'Alpha', situacao: 'PRESENTE' },
            },
          ],
        },
      ],
    }
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      if (url.includes('/chamadas-lideranca') && (!init?.method || init.method === 'GET')) {
        return new Response(JSON.stringify(gradeComRegistro), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      if (url.includes('/chamadas-lideranca') && init?.method === 'PUT') {
        return new Response(
          JSON.stringify({
            ...gradeComRegistro,
            discipulados: [
              {
                ...gradeComRegistro.discipulados[0],
                presencas: [
                  {
                    ...gradeComRegistro.discipulados[0].presencas[0],
                    situacao: 'AUSENTE',
                    registroDoDia: { discipuladoId: 10, discipuladoNome: 'Alpha', situacao: 'AUSENTE' },
                  },
                ],
              },
            ],
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }
      return new Response('not found', { status: 404 })
    })

    render(<LeadershipAttendance />)
    expect(await screen.findByText('Chamada já salva nesta sexta como Presente')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Ausente' }))
    await user.click(screen.getByRole('button', { name: 'Salvar Alpha' }))

    expect(await screen.findByRole('dialog', { name: 'Chamada já salva' })).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false)

    await user.click(screen.getByRole('button', { name: 'Atualizar chamada' }))
    await waitFor(() => expect(screen.getByText('Chamada de liderança salva.')).toBeInTheDocument())
    const putCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')
    const payload = JSON.parse(String(putCall?.[1]?.body))
    expect(payload.confirmarAtualizacao).toBe(true)
    expect(payload.discipulados[0].presencas).toEqual([{ usuarioId: 1, papel: 'DISCIPULADOR', situacao: 'AUSENTE' }])
  })
})
