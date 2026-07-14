import { describe, expect, it } from 'vitest'
import { formatarMes, normalizarMeses } from './dashboardUtils'

describe('componentes do histórico', () => {
  it('completa meses ausentes sem confundir com presença zero', () => {
    const meses = normalizarMeses('2026-01-10', '2026-03-20', [{ referencia: '2026-02', presentes: 0, ausentes: 2, visitantes: 0, percentualPresenca: 0 }])
    expect(meses.map((item) => item.referencia)).toEqual(['2026-01', '2026-02', '2026-03'])
    expect(meses[0].possuiEncontro).toBe(false)
    expect(meses[1]).toMatchObject({ possuiEncontro: true, percentualPresenca: 0 })
  })

  it('formata o mês em português', () => {
    expect(formatarMes('2026-06')).toBe('jun/2026')
  })
})
