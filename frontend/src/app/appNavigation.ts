export const APP_SECTIONS = [
  'visao-executiva',
  'painel',
  'minha-gerencia',
  'meu-discipulado',
  'estrutura',
  'usuarios',
  'adolescentes',
  'frequencia',
  'relatorios',
] as const

export type AppSection = (typeof APP_SECTIONS)[number]

export function pathForSection(section: AppSection) {
  return `/app/${section}`
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

export function navigateToSection(section: AppSection, replace = false) {
  const url = pathForSection(section)
  if (replace) window.history.replaceState({}, '', url)
  else window.history.pushState({}, '', url)
}
