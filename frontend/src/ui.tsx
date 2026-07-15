import { BarChartRounded, TableRowsRounded } from '@mui/icons-material'
import {
  Box, Chip, ChipProps, Paper, Skeleton, Stack, SxProps, Theme, ToggleButton,
  ToggleButtonGroup, Typography,
} from '@mui/material'
import { FormEventHandler, ReactNode, useState } from 'react'

export function PageHeader({ title, description, action, eyebrow }: { title: string; description?: ReactNode; action?: ReactNode; eyebrow?: string }) {
  return <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={2}>
    <Box>{eyebrow && <Typography variant="overline" color="primary.main" fontWeight={700}>{eyebrow}</Typography>}<Typography component="h1" variant="h4">{title}</Typography>{description && <Typography color="text.secondary" sx={{ mt: 0.5 }}>{description}</Typography>}</Box>
    {action && <Box sx={{ flexShrink: 0 }}>{action}</Box>}
  </Stack>
}

export function SectionCard({ title, description, icon, action, children, sx }: { title?: string; description?: ReactNode; icon?: ReactNode; action?: ReactNode; children: ReactNode; sx?: SxProps<Theme> }) {
  return <Paper variant="outlined" sx={{ overflow: 'hidden', boxShadow: '0 1px 3px rgba(23,32,51,.04)', ...sx }}>
    {(title || action) && <Stack direction="row" justifyContent="space-between" alignItems="flex-start" gap={2} sx={{ px: { xs: 2, sm: 2.5 }, pt: 2.25, pb: description ? 1.5 : 2 }}>
      <Stack direction="row" spacing={1.25} alignItems="flex-start"><Box sx={{ color: 'primary.main', display: 'grid', placeItems: 'center', mt: 0.15 }}>{icon}</Box><Box>{title && <Typography component="h2" variant="h6">{title}</Typography>}{description && <Typography variant="body2" color="text.secondary" sx={{ mt: 0.25 }}>{description}</Typography>}</Box></Stack>
      {action}
    </Stack>}
    <Box sx={{ p: { xs: 2, sm: 2.5 }, pt: title || action ? 0 : { xs: 2, sm: 2.5 } }}>{children}</Box>
  </Paper>
}

const toneMap = {
  primary: { color: '#3451B2', background: '#EEF1FF' },
  success: { color: '#2E7D32', background: '#EAF6EC' },
  warning: { color: '#B76E00', background: '#FFF4DD' },
  error: { color: '#C62828', background: '#FDECEC' },
  secondary: { color: '#0F8B8D', background: '#E7F6F6' },
}

export function KpiCard({ label, value, icon, tone = 'primary' }: { label: string; value: ReactNode; icon: ReactNode; tone?: keyof typeof toneMap }) {
  const colors = toneMap[tone]
  return <Paper variant="outlined" sx={{ p: 2.25, minWidth: 0, position: 'relative', overflow: 'hidden', boxShadow: '0 1px 3px rgba(23,32,51,.04)' }}>
    <Box sx={{ position: 'absolute', inset: '0 auto 0 0', width: 3, bgcolor: colors.color }} />
    <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1.5}><Box minWidth={0}><Typography variant="body2" color="text.secondary" noWrap>{label}</Typography><Typography variant="h4" sx={{ mt: 0.75, fontSize: { xs: '1.45rem', sm: '1.65rem' } }}>{value}</Typography></Box><Box sx={{ width: 40, height: 40, borderRadius: 2.5, display: 'grid', placeItems: 'center', color: colors.color, bgcolor: colors.background, flexShrink: 0 }}>{icon}</Box></Stack>
  </Paper>
}

export function FilterToolbar({ children, component = 'div', onSubmit }: { children: ReactNode; component?: 'div' | 'form'; onSubmit?: FormEventHandler<HTMLFormElement> }) {
  return <Paper component={component} onSubmit={onSubmit} variant="outlined" sx={{ p: 2, bgcolor: '#FBFCFE', boxShadow: 'none' }}>{children}</Paper>
}

export function AnalyticsCard({ title, description, chart, table }: { title: string; description?: ReactNode; chart: ReactNode; table: ReactNode }) {
  const [view, setView] = useState<'chart' | 'table'>('chart')
  return <SectionCard title={title} description={description} action={<ToggleButtonGroup exclusive size="small" value={view} onChange={(_, value: 'chart' | 'table' | null) => value && setView(value)} aria-label={`Visualização de ${title}`}>
    <ToggleButton value="chart" aria-label="Gráfico"><BarChartRounded fontSize="small" /><Box component="span" sx={{ ml: 0.75, display: { xs: 'none', sm: 'inline' } }}>Gráfico</Box></ToggleButton>
    <ToggleButton value="table" aria-label="Dados"><TableRowsRounded fontSize="small" /><Box component="span" sx={{ ml: 0.75, display: { xs: 'none', sm: 'inline' } }}>Dados</Box></ToggleButton>
  </ToggleButtonGroup>}>{view === 'chart' ? chart : table}</SectionCard>
}

export function DataTableCard({ children, sx }: { children: ReactNode; sx?: SxProps<Theme> }) { return <Paper variant="outlined" sx={{ overflowX: 'auto', boxShadow: '0 1px 3px rgba(23,32,51,.04)', ...sx }}>{children}</Paper> }

export function EmptyState({ title, description = 'Não há dados para exibir.' }: { title: string; description?: string }) { return <Stack alignItems="center" textAlign="center" spacing={0.75} sx={{ py: 5, px: 2 }}><Typography variant="h6">{title}</Typography><Typography variant="body2" color="text.secondary">{description}</Typography></Stack> }

export function LoadingState({ label = 'Carregando...' }: { label?: string }) { return <Stack role="status" aria-label={label} spacing={1.25} sx={{ py: 3 }}><Skeleton variant="rounded" height={72} /><Skeleton variant="rounded" height={72} /><Typography variant="caption" color="text.secondary">{label}</Typography></Stack> }

export function StatusChip({ active, activeLabel = 'Ativo', inactiveLabel = 'Inativo', ...props }: { active: boolean; activeLabel?: string; inactiveLabel?: string } & Omit<ChipProps, 'label' | 'color'>) { return <Chip size="small" color={active ? 'success' : 'default'} variant={active ? 'filled' : 'outlined'} label={active ? activeLabel : inactiveLabel} {...props} /> }
