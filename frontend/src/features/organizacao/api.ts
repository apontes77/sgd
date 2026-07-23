import { request } from '@/shared/api/httpClient'
import type {
  AtualizarUsuarioRequest,
  CriarUsuarioRequest,
  Discipulado,
  DiscipuladoRequest,
  Gerencia,
  GerenciaRequest,
  Pagina,
  Usuario,
} from '@/shared/api/types'

export type {
  AtualizarUsuarioRequest,
  CriarUsuarioRequest,
  Discipulado,
  DiscipuladoRequest,
  Gerencia,
  GerenciaRequest,
  Pagina,
  Perfil,
  SexoDiscipulado,
  Usuario,
} from '@/shared/api/types'

export const organizationApi = {
  listarUsuarios: (page = 0, size = 100, ativo?: boolean) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (ativo !== undefined) params.set('ativo', String(ativo))
    return request<Pagina<Usuario>>(`/usuarios?${params}`)
  },
  criarUsuario: (body: CriarUsuarioRequest) =>
    request<Usuario>('/usuarios', { method: 'POST', body: JSON.stringify(body) }),
  atualizarUsuario: (id: number, body: AtualizarUsuarioRequest) =>
    request<Usuario>(`/usuarios/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  listarGerencias: () => request<Pagina<Gerencia>>('/gerencias?page=0&size=100'),
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
