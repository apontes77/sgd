import { request } from './api'

export interface IndicadorFrequencia { presentes: number; ausentes: number; percentualPresenca: number }
export interface ResumoPainel extends IndicadorFrequencia { encontrosRealizados: number; visitantes: number }
export interface EvolucaoMensal extends IndicadorFrequencia { referencia: string; visitantes: number }
export interface IndicadorGerencia extends IndicadorFrequencia { id: number; nome: string }
export interface IndicadorSexo extends IndicadorFrequencia { sexo: 'MASCULINO' | 'FEMININO' }
export interface PainelAdminResponse { dataInicio: string; dataFim: string; resumo: ResumoPainel; evolucao: EvolucaoMensal[]; gerencias: IndicadorGerencia[]; sexos: IndicadorSexo[] }

export const painelApi = {
  consultar: (dataInicio: string, dataFim: string) => request<PainelAdminResponse>(`/painel/admin?${new URLSearchParams({ dataInicio, dataFim })}`),
}
