import { Box, Paper, Stack, Typography, useTheme } from '@mui/material'
import { alpha } from '@mui/material/styles'
import type { ReactNode } from 'react'

import type { AppTheme } from '@/app/theme.tokens'

type Tone = 'primary' | 'success' | 'warning' | 'error' | 'secondary'

function useToneColors(tone: Tone) {
  const theme = useTheme<AppTheme>()
  const color = {
    primary: theme.palette.primary.main,
    success: theme.palette.success.main,
    warning: theme.palette.warning.main,
    error: theme.palette.error.main,
    secondary: theme.palette.secondary.main,
  }[tone]
  return { color, background: alpha(color, 0.08) }
}

export function KpiCard({
  label,
  value,
  icon,
  tone = 'primary',
}: {
  label: string
  value: ReactNode
  icon: ReactNode
  tone?: Tone
}) {
  const colors = useToneColors(tone)
  return (
    <Paper
      variant="outlined"
      sx={{
        p: 2.25,
        minWidth: 0,
        position: 'relative',
        overflow: 'hidden',
        borderRadius: '16px',
        borderColor: 'transparent',
        boxShadow: (theme) => theme.app.shadow.card,
        transition: 'box-shadow 0.2s ease',
        '&:hover': {
          boxShadow: (theme) => theme.app.shadow.cardHover,
        },
      }}
    >
      <Box sx={{ position: 'absolute', inset: '0 0 auto 0', height: 4, bgcolor: colors.color }} />
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1.5}>
        <Box minWidth={0}>
          <Typography variant="body2" color="text.secondary" noWrap>
            {label}
          </Typography>
          <Typography variant="h4" sx={{ mt: 0.75, fontSize: { xs: '1.45rem', sm: '1.65rem' } }}>
            {value}
          </Typography>
        </Box>
        <Box
          sx={{
            width: 40,
            height: 40,
            borderRadius: '12px',
            display: 'grid',
            placeItems: 'center',
            color: colors.color,
            bgcolor: colors.background,
            flexShrink: 0,
          }}
        >
          {icon}
        </Box>
      </Stack>
    </Paper>
  )
}
