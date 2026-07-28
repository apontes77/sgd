import { useTheme } from '@mui/material'

const brand = {
  primary: '#2A3FBB',
  primaryLight: '#4F6DFF',
  secondary: '#00A3A5',
  success: '#0E9F6E',
  warning: '#F59E0B',
  error: '#EF4444',
  neutral: '#94A3B8',
  gauge: {
    alert: '#EF4444',
    attention: '#F59E0B',
    ok: '#0E9F6E',
  },
} as const

/** Cores de marca (independentes do tema). Preferir `useChartColors()` para texto/eixos. */
export const chartColors = {
  ...brand,
  text: '#0B1221',
  textSecondary: '#4B5563',
  heatmap: ['#FEF2F2', '#FFF8E6', '#EAF6F0', '#0E9F6E'],
}

export type ChartPalette = {
  primary: string
  primaryLight: string
  secondary: string
  success: string
  warning: string
  error: string
  neutral: string
  text: string
  textSecondary: string
  splitLine: string
  axisLine: string
  surface: string
  heatmap: string[]
  gauge: typeof brand.gauge
}

export function getChartPalette(mode: 'light' | 'dark'): ChartPalette {
  const dark = mode === 'dark'
  return {
    ...brand,
    text: dark ? '#F0F4FF' : '#0B1221',
    textSecondary: dark ? '#C5D0E0' : '#4B5563',
    splitLine: dark ? 'rgba(255,255,255,0.10)' : 'rgba(11,18,33,0.08)',
    axisLine: dark ? 'rgba(255,255,255,0.20)' : 'rgba(11,18,33,0.14)',
    surface: dark ? '#121B2E' : '#FFFFFF',
    heatmap: dark ? ['#1E293B', '#5C4A2A', '#1F4D3A', '#0E9F6E'] : ['#FEF2F2', '#FFF8E6', '#EAF6F0', '#0E9F6E'],
  }
}

export function useChartColors(): ChartPalette {
  const theme = useTheme()
  return getChartPalette(theme.palette.mode)
}

/** Estilo legível para labels de série (evita “fantasma” no dark mode). */
export function seriesLabelStyle(colors: ChartPalette, fontSize = 12) {
  return {
    color: colors.text,
    fontSize,
    textBorderColor: colors.surface,
    textBorderWidth: 2,
    textBorderType: 'solid' as const,
  }
}

export function axisLabelStyle(colors: ChartPalette, fontSize = 12) {
  return {
    color: colors.textSecondary,
    fontSize,
  }
}

export function legendTextStyle(colors: ChartPalette, fontSize = 12) {
  return {
    color: colors.textSecondary,
    fontSize,
  }
}

export const chartBase = {
  textStyle: { fontFamily: 'Inter, "Plus Jakarta Sans", Arial, sans-serif' },
  color: [brand.primary, brand.success, brand.secondary, brand.warning, brand.error, brand.primaryLight, brand.neutral],
}

export const GAUGE_ZONES = { alerta: 0.6, atencao: 0.8 } as const

export function gaugeZoneColors() {
  return [
    [GAUGE_ZONES.alerta, brand.gauge.alert],
    [GAUGE_ZONES.atencao, brand.gauge.attention],
    [1, brand.gauge.ok],
  ] as const
}
