import { describe, expect, it } from 'vitest'

import { mensagemTelefoneInvalido, telefoneValido, validarTelefoneAdolescente } from '@/shared/validation/telefone'

describe('validação de telefone', () => {
  it('aceita telefone brasileiro com DDD', () => {
    expect(telefoneValido('(11) 98888-0000')).toBe(true)
    expect(telefoneValido('11988880000')).toBe(true)
    expect(telefoneValido('')).toBe(true)
  })

  it('rejeita telefone incompleto', () => {
    expect(telefoneValido('98888-0000')).toBe(false)
    expect(mensagemTelefoneInvalido('telefone')).toContain('DDD')
  })

  it('exige telefone para GOE salvo flag', () => {
    expect(validarTelefoneAdolescente('DISCIPULO_GOE', { naoPossuiTelefone: true })).toBeNull()
    expect(validarTelefoneAdolescente('DISCIPULO_GOE', { telefone: '(11) 91234-5678' })).toBeNull()
    expect(validarTelefoneAdolescente('DISCIPULO_GOE', {})).toContain('telefone')
  })
})
