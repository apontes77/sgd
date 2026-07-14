import type { EvolucaoMensal } from './painelApi'

export interface MesVisual extends EvolucaoMensal { possuiEncontro: boolean }

export const percentual = (valor: number) => `${valor.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}%`
export const formatarMes = (referencia: string) => {
  const [ano, mes] = referencia.split('-').map(Number)
  return new Intl.DateTimeFormat('pt-BR', { month: 'short', year: 'numeric', timeZone: 'UTC' }).format(new Date(Date.UTC(ano, mes - 1, 1))).replace(' de ', '/').replace('.', '')
}
const iso = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
export const periodoPadrao = () => {
  const fim = new Date()
  const mes = fim.getMonth() - 6
  const ultimoDia = new Date(fim.getFullYear(), mes + 1, 0).getDate()
  return { inicio: iso(new Date(fim.getFullYear(), mes, Math.min(fim.getDate(), ultimoDia))), fim: iso(fim) }
}

export function normalizarMeses(inicio: string, fim: string, dados: EvolucaoMensal[]): MesVisual[] {
  const existentes = new Map(dados.map((item) => [item.referencia, item]))
  const [anoInicio, mesInicio] = inicio.slice(0, 7).split('-').map(Number)
  const [anoFim, mesFim] = fim.slice(0, 7).split('-').map(Number)
  const cursor = new Date(Date.UTC(anoInicio, mesInicio - 1, 1))
  const limite = new Date(Date.UTC(anoFim, mesFim - 1, 1))
  const meses: MesVisual[] = []
  while (cursor <= limite) {
    const referencia = `${cursor.getUTCFullYear()}-${String(cursor.getUTCMonth() + 1).padStart(2, '0')}`
    const item = existentes.get(referencia)
    meses.push(item ? { ...item, possuiEncontro: true } : { referencia, presentes: 0, ausentes: 0, visitantes: 0, percentualPresenca: 0, possuiEncontro: false })
    cursor.setUTCMonth(cursor.getUTCMonth() + 1)
  }
  return meses
}
