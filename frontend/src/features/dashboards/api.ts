import { request } from '@/shared/api/httpClient'

export interface IndicadorFrequencia {
  presentes: number
  ausentes: number
  percentualPresenca: number
}
export interface ResumoPainel extends IndicadorFrequencia {
  encontrosRealizados: number
  visitantes: number
}
export interface EvolucaoMensal extends IndicadorFrequencia {
  referencia: string
  visitantes: number
}
export interface EvolucaoDiscipulo extends IndicadorFrequencia {
  referencia: string
}
export interface DiscipuloPainel extends Omit<IndicadorFrequencia, 'percentualPresenca'> {
  adolescenteId: number
  nome: string
  percentualPresenca: number | null
  evolucao: EvolucaoDiscipulo[]
}
export interface IndicadorGerencia extends IndicadorFrequencia {
  id: number
  nome: string
}
export interface IndicadorGerenciaMensal extends IndicadorFrequencia {
  gerenciaId: number
  gerenciaNome: string
  referencia: string
}
export interface IndicadorSexo extends IndicadorFrequencia {
  sexo: 'MASCULINO' | 'FEMININO'
}
export interface PainelAdminResponse {
  dataInicio: string
  dataFim: string
  resumo: ResumoPainel
  evolucao: EvolucaoMensal[]
  gerencias: IndicadorGerencia[]
  sexos: IndicadorSexo[]
  encontrosNaoRealizados: number
  gerenciasMensal: IndicadorGerenciaMensal[]
}
export interface DiscipuladoPainel {
  id: number
  nome: string
  sexo: 'MASCULINO' | 'FEMININO'
  ativo: boolean
  resumo: ResumoPainel
  evolucao: EvolucaoMensal[]
}
export interface EncontroNaoRealizado {
  encontroId: number
  discipuladoId: number
  discipuladoNome: string
  data: string
  justificativa: string
}
export interface PainelGerenciaResponse {
  dataInicio: string
  dataFim: string
  gerencia: { id: number; nome: string }
  resumo: ResumoPainel
  evolucao: EvolucaoMensal[]
  discipulados: DiscipuladoPainel[]
  encontrosNaoRealizados: EncontroNaoRealizado[]
}
export interface PainelLiderResponse {
  dataInicio: string
  dataFim: string
  discipulado: { id: number; nome: string; sexo: 'MASCULINO' | 'FEMININO'; ativo: boolean }
  resumo: ResumoPainel
  evolucao: EvolucaoMensal[]
  discipulos: DiscipuloPainel[]
}

export const painelApi = {
  consultar: (dataInicio: string, dataFim: string) =>
    request<PainelAdminResponse>(`/painel/admin?${new URLSearchParams({ dataInicio, dataFim })}`),
  consultarGerencia: (dataInicio: string, dataFim: string) =>
    request<PainelGerenciaResponse>(`/painel/gerencia?${new URLSearchParams({ dataInicio, dataFim })}`),
  consultarLider: (dataInicio: string, dataFim: string) =>
    request<PainelLiderResponse>(`/painel/lider?${new URLSearchParams({ dataInicio, dataFim })}`),
}
