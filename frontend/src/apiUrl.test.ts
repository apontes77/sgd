import { describe, expect, it } from 'vitest'
import { buildApiBaseUrl } from './api'

describe('configuracao da API', () => {
  it('usa caminho relativo no ambiente local', () => {
    expect(buildApiBaseUrl()).toBe('/api/v1')
  })

  it('normaliza a origem configurada em producao', () => {
    expect(buildApiBaseUrl('https://sgd-api.onrender.com/'))
      .toBe('https://sgd-api.onrender.com/api/v1')
  })
})
