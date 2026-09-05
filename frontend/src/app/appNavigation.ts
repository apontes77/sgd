export const APP_SECTIONS = [
  'visao-executiva',
  'painel',
  'minha-gerencia',
  'meu-discipulado',
  'estrutura',
  'usuarios',
  'adolescentes',
  'familias',
  'frequencia',
  'frequencia-formacao',
  'chamada-lideranca',
  'relatorios',
] as const

export type AppSection = (typeof APP_SECTIONS)[number]

export type SectionSearch = { discipuladoId?: number }

export function pathForSection(section: AppSection, search?: SectionSearch) {
  const base = `/app/${section}`
  if (!search?.discipuladoId) return base
  const params = new URLSearchParams({ discipuladoId: String(search.discipuladoId) })
  return `${base}?${params}`
}

export function sectionFromPath(pathname: string): AppSection | undefined {
  const match = /^\/app\/([^/]+)\/?$/.exec(pathname)
  if (!match) return undefined
  return (APP_SECTIONS as readonly string[]).includes(match[1]) ? (match[1] as AppSection) : undefined
}

export function resolveInitialSection(available: AppSection[], pathname = window.location.pathname): AppSection {
  const fromPath = sectionFromPath(pathname)
  if (fromPath && available.includes(fromPath)) return fromPath
  return available[0]
}

export function discipuladoIdFromSearch(search = window.location.search): number | undefined {
  const raw = new URLSearchParams(search).get('discipuladoId')
  if (!raw) return undefined
  const id = Number(raw)
  return Number.isInteger(id) && id > 0 ? id : undefined
}

export function navigateToSection(section: AppSection, replace = false, search?: SectionSearch) {
  const url = pathForSection(section, search)
  if (replace) window.history.replaceState({}, '', url)
  else window.history.pushState({}, '', url)
}
