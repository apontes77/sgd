export const chartColors = {
  primary: '#2A3FBB',
  primaryLight: '#4F6DFF',
  secondary: '#00A3A5',
  success: '#0E9F6E',
  warning: '#F59E0B',
  error: '#EF4444',
  neutral: '#94A3B8',
  text: '#0B1221',
  textSecondary: '#4B5563',
  heatmap: ['#FEF2F2', '#FFF8E6', '#EAF6F0', '#0E9F6E'],
  gauge: {
    alert: '#EF4444',
    attention: '#F59E0B',
    ok: '#0E9F6E',
  },
}

export const chartBase = {
  textStyle: { fontFamily: 'Inter, "Plus Jakarta Sans", Arial, sans-serif' },
  color: [
    chartColors.primary,
    chartColors.success,
    chartColors.secondary,
    chartColors.warning,
    chartColors.error,
    chartColors.primaryLight,
    chartColors.neutral,
  ],
}

export const GAUGE_ZONES = { alerta: 0.6, atencao: 0.8 } as const

export function gaugeZoneColors() {
  return [
    [GAUGE_ZONES.alerta, chartColors.gauge.alert],
    [GAUGE_ZONES.atencao, chartColors.gauge.attention],
    [1, chartColors.gauge.ok],
  ] as const
}
