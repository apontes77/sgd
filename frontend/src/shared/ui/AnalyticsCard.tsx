import { BarChartRounded, TableRowsRounded } from '@mui/icons-material'
import { Box, ToggleButton, ToggleButtonGroup } from '@mui/material'
import type { ReactNode } from 'react'
import { useState } from 'react'

import { SectionCard } from './SectionCard'

export function AnalyticsCard({
  title,
  description,
  chart,
  table,
  defaultView = 'chart',
}: {
  title: string
  description?: ReactNode
  chart: ReactNode
  table: ReactNode
  /** Preferência inicial; use "table" em charts densos (ex.: heatmap). */
  defaultView?: 'chart' | 'table'
}) {
  const [view, setView] = useState<'chart' | 'table'>(defaultView)
  return (
    <SectionCard
      title={title}
      description={description}
      action={
        <ToggleButtonGroup
          exclusive
          size="small"
          value={view}
          onChange={(_, value: 'chart' | 'table' | null) => value && setView(value)}
          aria-label={`Visualização de ${title}`}
        >
          <ToggleButton value="chart" aria-label="Gráfico">
            <BarChartRounded fontSize="small" />
            <Box component="span" sx={{ ml: 0.75, display: { xs: 'none', sm: 'inline' } }}>
              Gráfico
            </Box>
          </ToggleButton>
          <ToggleButton value="table" aria-label="Dados">
            <TableRowsRounded fontSize="small" />
            <Box component="span" sx={{ ml: 0.75, display: { xs: 'none', sm: 'inline' } }}>
              Dados
            </Box>
          </ToggleButton>
        </ToggleButtonGroup>
      }
    >
      {view === 'chart' ? chart : table}
    </SectionCard>
  )
}
