import { cleanup, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import OrganizationManagement from '@/features/organizacao/OrganizationManagement'
import type { Discipulado, Gerencia, Pagina, Usuario } from '@/shared/api/types'
import { render } from '@/test/test-utils'

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
const coLider: Usuario = {
  id: 3,
  nome: 'Maria Co-líder',
  email: 'maria@sgd.local',
  ativo: true,
  perfis: ['CO_LIDER'],
}
const outroCoLider: Usuario = {
  id: 4,
  nome: 'Ana Co-líder',
  email: 'ana@sgd.local',
  ativo: true,
  perfis: ['CO_LIDER'],
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

describe('estrutura organizacional — co-líderes', () => {
  beforeEach(() => {
    sessionStorage.setItem('sgd.access-token', 'token')
  })

  afterEach(() => {
    cleanup()
    sessionStorage.clear()
    vi.restoreAllMocks()
  })

  it('na edição, associa um co-líder já existente via PUT', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      const method = init?.method ?? 'GET'
      if (url.includes('/usuarios?')) {
        const pagina = Number(new URL(url, 'http://local').searchParams.get('page') ?? 0)
        if (pagina === 0) {
          return json(page([admin, discipulador], { totalElements: 4, totalPages: 2 }))
        }
        return json(page([coLider, outroCoLider], { page: 1, totalElements: 4, totalPages: 2 }))
      }
      if (url.includes('/gerencias?')) return json(page([gerencia]))
      if (url.includes('/discipulados?')) return json(page([discipulado]))
      if (url.includes('/discipulados/20/co-lideres') && method === 'PUT') {
        return json({ ...discipulado, coLideres: [coLider] })
      }
      if (url.includes('/discipulados/20') && method === 'PATCH') return json(discipulado)
      throw new Error(`Requisição inesperada: ${method} ${url}`)
    })

    render(<OrganizationManagement />)
    await user.click(await screen.findByRole('tab', { name: 'Discipulados' }))
    await user.click(await screen.findByRole('button', { name: 'Ações de Luz do mundo' }))
    await user.click(await screen.findByRole('menuitem', { name: 'Editar' }))

    expect(await screen.findByRole('heading', { name: 'Editar discipulado' })).toBeInTheDocument()

    await user.click(screen.getByRole('combobox', { name: /^Co-líder$/ }))
    const listbox = await screen.findByRole('listbox')
    expect(within(listbox).queryByRole('option', { name: /Administrador/ })).not.toBeInTheDocument()
    await user.click(within(listbox).getByRole('option', { name: /Maria Co-líder/ }))
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() => {
      const put = fetchMock.mock.calls.find(
        ([requestUrl, requestInit]) =>
          String(requestUrl).includes('/discipulados/20/co-lideres') && requestInit?.method === 'PUT',
      )
      expect(put).toBeTruthy()
      expect(JSON.parse(String(put?.[1]?.body))).toEqual({ usuarioIds: [3] })
    })
  })
})
