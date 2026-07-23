export type Perfil = 'ADMIN' | 'GERENTE' | 'DISCIPULADOR' | 'CO_LIDER'

export interface Usuario {
  id: number
  nome: string
  email: string
  ativo?: boolean
  perfis: Perfil[]
}

export interface Pagina<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Gerencia {
  id: number
  nome: string
  gerenteId: number
  ativo?: boolean
}

export interface GerenciaRequest {
  nome: string
  gerenteId: number
}

export interface CriarUsuarioRequest {
  nome: string
  email: string
  senha: string
  perfis: Perfil[]
}

export interface AtualizarUsuarioRequest {
  nome?: string
  perfis?: Perfil[]
  ativo?: boolean
}

export type SexoDiscipulado = 'MASCULINO' | 'FEMININO'

export interface Discipulado {
  id: number
  nome: string
  sexo: SexoDiscipulado
  gerenciaId: number
  discipuladorId: number
  ativo?: boolean
  coLideres: Usuario[]
}

export interface DiscipuladoRequest {
  nome: string
  sexo: SexoDiscipulado
  gerenciaId: number
  discipuladorId: number
  ativo?: boolean
}
