import { request } from '@/shared/api/httpClient'
import type {
  AtualizarUsuarioRequest,
  CriarUsuarioRequest,
  Discipulado,
  DiscipuladoRequest,
  FaixaEtaria,
  Gerencia,
  GerenciaRequest,
  Pagina,
  SexoOrganizacional,
  Usuario,
} from '@/shared/api/types'

export type {
  AtualizarUsuarioRequest,
  CriarUsuarioRequest,
  Discipulado,
  DiscipuladoRequest,
  FaixaEtaria,
  Gerencia,
  GerenciaRequest,
  Pagina,
  Perfil,
  SexoDiscipulado,
  SexoOrganizacional,
  Usuario,
} from '@/shared/api/types'

export const organizationApi = {
  listarUsuarios: (page = 0, size = 100, ativo?: boolean) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (ativo !== undefined) params.set('ativo', String(ativo))
    return request<Pagina<Usuario>>(`/usuarios?${params}`)
  },
  listarTodosUsuarios: async (ativo?: boolean) => {
    const size = 100
    const first = await organizationApi.listarUsuarios(0, size, ativo)
    const pages = [first.content]
    for (let page = 1; page < first.totalPages; page += 1) {
      const next = await organizationApi.listarUsuarios(page, size, ativo)
      pages.push(next.content)
    }
    return pages.flat()
  },
  criarUsuario: (body: CriarUsuarioRequest) =>
    request<Usuario>('/usuarios', { method: 'POST', body: JSON.stringify(body) }),
  atualizarUsuario: (id: number, body: AtualizarUsuarioRequest) =>
    request<Usuario>(`/usuarios/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  listarGerencias: (filtros?: { sexo?: SexoOrganizacional | ''; faixaEtaria?: FaixaEtaria | '' }) => {
    const params = new URLSearchParams({ page: '0', size: '100' })
    if (filtros?.sexo) params.set('sexo', filtros.sexo)
    if (filtros?.faixaEtaria) params.set('faixaEtaria', filtros.faixaEtaria)
    return request<Pagina<Gerencia>>(`/gerencias?${params}`)
  },
  criarGerencia: (body: GerenciaRequest) =>
    request<Gerencia>('/gerencias', { method: 'POST', body: JSON.stringify(body) }),
  atualizarGerencia: (id: number, body: GerenciaRequest) =>
    request<Gerencia>(`/gerencias/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  listarDiscipulados: (ativo?: boolean) => {
    const params = new URLSearchParams({ page: '0', size: '100' })
    if (ativo !== undefined) params.set('ativo', String(ativo))
    return request<Pagina<Discipulado>>(`/discipulados?${params}`)
  },
  listarDiscipuladosLiderados: (ativo?: boolean) => {
    const params = new URLSearchParams()
    if (ativo !== undefined) params.set('ativo', String(ativo))
    const query = params.size ? `?${params}` : ''
    return request<Discipulado[]>(`/discipulados/liderados${query}`)
  },
  criarDiscipulado: (body: DiscipuladoRequest) =>
    request<Discipulado>('/discipulados', { method: 'POST', body: JSON.stringify(body) }),
  atualizarDiscipulado: (id: number, body: DiscipuladoRequest) =>
    request<Discipulado>(`/discipulados/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  definirCoLideres: (id: number, usuarioIds: number[]) =>
    request<Discipulado>(`/discipulados/${id}/co-lideres`, { method: 'PUT', body: JSON.stringify({ usuarioIds }) }),
}
