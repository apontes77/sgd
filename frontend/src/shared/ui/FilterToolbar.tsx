import { Paper } from '@mui/material'
import type { FormEventHandler, ReactNode } from 'react'

export function FilterToolbar({
  children,
  component = 'div',
  onSubmit,
}: {
  children: ReactNode
  component?: 'div' | 'form'
  onSubmit?: FormEventHandler<HTMLFormElement>
}) {
  return (
    <Paper
      component={component}
      onSubmit={onSubmit}
      variant="outlined"
      sx={{
        p: 2,
        bgcolor: (theme) => theme.app.surface.sunken,
        borderColor: (theme) => theme.app.border.subtle,
        boxShadow: 'none',
        borderRadius: '12px',
      }}
    >
      {children}
    </Paper>
  )
}
