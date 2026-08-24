/** Brasil sem horário de verão: America/Sao_Paulo = UTC-3. */

export function ehSexta(dataIso: string): boolean {
  const [y, m, d] = dataIso.split('-').map(Number)
  if (!y || !m || !d) return false
  return new Date(Date.UTC(y, m - 1, d)).getUTCDay() === 5
}

/** Segunda 00:00 America/Sao_Paulo após o domingo subsequente à sexta. */
export function limiteExclusivoPrazo(dataIso: string): Date | null {
  if (!ehSexta(dataIso)) return null
  const [y, m, d] = dataIso.split('-').map(Number)
  // sexta + 3 dias = segunda; 00:00 SP = 03:00 UTC
  return new Date(Date.UTC(y, m - 1, d + 3, 3, 0, 0))
}

export function dentroDoPrazoLancamento(dataIso: string, agora = new Date()): boolean {
  const limite = limiteExclusivoPrazo(dataIso)
  if (!limite) return true
  return agora.getTime() < limite.getTime()
}

export const JUSTIFICATIVA_FECHAMENTO_AUTOMATICO = 'discipulador ou colider não registraram a frequência'
export const AVISO_LANCAMENTO_PENDENTE = 'O discipulador/co-líder não lançou a frequência no prazo.'
