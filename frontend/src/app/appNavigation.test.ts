import { describe, expect, it } from 'vitest'

import { pathForSection, resolveInitialSection, sectionFromPath } from './appNavigation'

describe('appNavigation', () => {
  it('monta e interpreta caminhos /app/:secao', () => {
    expect(pathForSection('visao-executiva')).toBe('/app/visao-executiva')
    expect(sectionFromPath('/app/painel')).toBe('painel')
    expect(sectionFromPath('/app/desconhecida')).toBeUndefined()
    expect(sectionFromPath('/')).toBeUndefined()
  })

  it('resolve a seção inicial a partir da URL quando disponível', () => {
    expect(resolveInitialSection(['visao-executiva', 'painel'], '/app/painel')).toBe('painel')
    expect(resolveInitialSection(['visao-executiva', 'painel'], '/app/relatorios')).toBe('visao-executiva')
    expect(resolveInitialSection(['meu-discipulado', 'relatorios'], '/')).toBe('meu-discipulado')
  })
})
