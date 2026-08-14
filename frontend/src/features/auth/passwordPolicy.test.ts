import { describe, expect, it } from 'vitest'

import { PASSWORD_REQUIREMENTS, passwordMeetsPolicy } from '@/features/auth/passwordPolicy'

describe('politica de senha', () => {
  it('aceita senha com tamanho minimo e os quatro tipos de caractere', () => {
    expect(passwordMeetsPolicy('Ab1!xy')).toBe(true)
  })

  it('rejeita senha curta ou sem algum requisito', () => {
    expect(passwordMeetsPolicy('Ab1!x')).toBe(false)
    expect(passwordMeetsPolicy('abcdef')).toBe(false)
    expect(passwordMeetsPolicy('ABCDEF1!')).toBe(false)
    expect(passwordMeetsPolicy('abcdef1!')).toBe(false)
    expect(passwordMeetsPolicy('Abcdef!')).toBe(false)
    expect(passwordMeetsPolicy('Abcdef1')).toBe(false)
  })

  it('avalia cada requisito de forma independente', () => {
    const password = 'Senha1!'
    for (const requirement of PASSWORD_REQUIREMENTS) {
      expect(requirement.test(password)).toBe(true)
    }
    expect(PASSWORD_REQUIREMENTS.find((item) => item.key === 'uppercase')?.test('senha1!')).toBe(false)
    expect(PASSWORD_REQUIREMENTS.find((item) => item.key === 'lowercase')?.test('SENHA1!')).toBe(false)
    expect(PASSWORD_REQUIREMENTS.find((item) => item.key === 'number')?.test('Senha!')).toBe(false)
    expect(PASSWORD_REQUIREMENTS.find((item) => item.key === 'special')?.test('Senha1')).toBe(false)
  })
})
