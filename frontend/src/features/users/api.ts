import { organizationApi } from '@/features/organizacao/api'
import type { AtualizarUsuarioRequest, CriarUsuarioRequest } from '@/shared/api/types'

export type { AtualizarUsuarioRequest, CriarUsuarioRequest, Pagina, Perfil, Usuario } from '@/shared/api/types'

export const userManagementClient = {
  list: (page: number, size: number, active?: boolean) => organizationApi.listarUsuarios(page, size, active),
  create: (body: CriarUsuarioRequest) => organizationApi.criarUsuario(body),
  update: (id: number, body: AtualizarUsuarioRequest) => organizationApi.atualizarUsuario(id, body),
}
