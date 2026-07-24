import type { SxProps, Theme } from '@mui/material'
import { Paper } from '@mui/material'
import type { ReactNode } from 'react'

export function DataTableCard({ children, sx }: { children: ReactNode; sx?: SxProps<Theme> }) {
  return (
    <Paper
      variant="outlined"
      sx={{
        overflowX: 'auto',
        borderRadius: '16px',
        borderColor: (theme) => theme.app.border.subtle,
        boxShadow: (theme) => theme.app.shadow.card,
        ...sx,
      }}
    >
      {children}
    </Paper>
  )
}
