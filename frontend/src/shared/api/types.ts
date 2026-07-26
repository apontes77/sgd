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

export type FaixaEtaria = 'DE_09_A_11' | 'DE_11_A_13' | 'DE_13_A_15' | 'DE_15_MAIS'

export type SexoOrganizacional = 'MASCULINO' | 'FEMININO'

export interface Gerencia {
  id: number
  nome: string
  sexo: SexoOrganizacional
  faixasEtarias: FaixaEtaria[]
  gerenteId: number
  ativo?: boolean
}

export interface GerenciaRequest {
  nome: string
  sexo: SexoOrganizacional
  faixasEtarias: FaixaEtaria[]
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

export type SexoDiscipulado = SexoOrganizacional

export interface Discipulado {
  id: number
  nome: string
  sexo: SexoDiscipulado
  faixaEtaria: FaixaEtaria
  gerenciaId: number
  discipuladorId: number
  ativo?: boolean
  coLideres: Usuario[]
}

export interface DiscipuladoRequest {
  nome: string
  sexo: SexoDiscipulado
  faixaEtaria: FaixaEtaria
  gerenciaId: number
  discipuladorId: number
  ativo?: boolean
}
