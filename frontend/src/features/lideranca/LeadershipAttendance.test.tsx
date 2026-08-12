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
      gerenciaNome: 'Centro',
      observacao: null,
      presencas: [
        { usuarioId: 1, nome: 'Líder Alpha', papel: 'DISCIPULADOR', situacao: null },
        { usuarioId: 2, nome: 'Co Alpha', papel: 'CO_LIDER', situacao: null },
      ],
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
    expect(screen.getByText('Líder Alpha')).toBeInTheDocument()
    expect(screen.getByText('Co Alpha')).toBeInTheDocument()

    const presentes = screen.getAllByRole('button', { name: 'Presente' })
    const ausentes = screen.getAllByRole('button', { name: 'Ausente' })
    await user.click(presentes[0])
    await user.click(ausentes[1])
    await user.type(screen.getByLabelText('Observação do discipulado'), 'Chegou atrasado')
    await user.type(screen.getByLabelText('Observação geral'), 'Culto tranquilo')
    await user.click(screen.getByRole('button', { name: /Salvar/i }))

    await waitFor(() => expect(screen.getByText('Chamada de liderança salva.')).toBeInTheDocument())
    const putCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')
    expect(putCall).toBeTruthy()
    const payload = JSON.parse(String(putCall?.[1]?.body))
    expect(payload.observacaoGeral).toBe('Culto tranquilo')
    expect(payload.discipulados[0].observacao).toBe('Chegou atrasado')
    expect(payload.discipulados[0].presencas).toEqual([
      { usuarioId: 1, papel: 'DISCIPULADOR', situacao: 'PRESENTE' },
      { usuarioId: 2, papel: 'CO_LIDER', situacao: 'AUSENTE' },
    ])
  })
})
