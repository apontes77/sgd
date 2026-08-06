import { describe, expect, it } from 'vitest'

import { contatoMinimoValido, validarContatosPorCategoria } from '@/shared/validation/telefone'

describe('validarContatosPorCategoria', () => {
  it('exige telefone do adolescente para Discípulo GOE', () => {
    expect(validarContatosPorCategoria('DISCIPULO_GOE', {})).toContain('telefone do adolescente')
    expect(validarContatosPorCategoria('DISCIPULO_GOE', { telefone: '(11) 91234-5678' })).toBeNull()
  })

  it('permite Discípulo GOE sem telefone quando marcado que não possui', () => {
    expect(validarContatosPorCategoria('DISCIPULO_GOE', { naoPossuiTelefone: true })).toBeNull()
  })

  it('não exige contato familiar para Discípulo GOE', () => {
    expect(
      validarContatosPorCategoria('DISCIPULO_GOE', {
        telefone: '(11) 91234-5678',
      }),
    ).toBeNull()
  })

  it('exige contato familiar para Discípulo e Visitante', () => {
    expect(validarContatosPorCategoria('DISCIPULO', {})).toContain('mãe, ou do pai, ou do responsável')
    expect(validarContatosPorCategoria('VISITANTE', {})).toContain('mãe, ou do pai, ou do responsável')
    expect(
      validarContatosPorCategoria('DISCIPULO', {
        nomeMae: 'Maria',
        telefoneMae: '(11) 98888-0000',
      }),
    ).toBeNull()
  })

  it('permite Discípulo e Visitante sem contato familiar quando marcado que não possui', () => {
    expect(validarContatosPorCategoria('DISCIPULO', { naoPossuiContatoFamiliar: true })).toBeNull()
    expect(validarContatosPorCategoria('VISITANTE', { naoPossuiContatoFamiliar: true })).toBeNull()
  })

  it('contatoMinimoValido continua validando o par completo', () => {
    expect(contatoMinimoValido({ nomeMae: 'Maria', telefoneMae: '(11) 98888-0000' })).toBe(true)
    expect(contatoMinimoValido({ nomeMae: 'Maria' })).toBe(false)
  })
})
