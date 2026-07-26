import { describe, expect, it } from 'vitest'

import { dentroDoPrazoLancamento, ehSexta, limiteExclusivoPrazo } from '@/features/frequencia/prazoLancamento'

describe('prazoLancamento', () => {
  it('reconhece sexta-feira', () => {
    expect(ehSexta('2026-07-17')).toBe(true)
    expect(ehSexta('2026-07-18')).toBe(false)
  })

  it('inclui domingo 23:59 e exclui segunda 00:00 em São Paulo', () => {
    const limite = limiteExclusivoPrazo('2026-07-17')!
    expect(dentroDoPrazoLancamento('2026-07-17', new Date(limite.getTime() - 1))).toBe(true)
    expect(dentroDoPrazoLancamento('2026-07-17', limite)).toBe(false)
  })

  it('não aplica prazo especial a outros dias', () => {
    expect(dentroDoPrazoLancamento('2026-07-16', new Date('2026-08-01T12:00:00Z'))).toBe(true)
  })
})
