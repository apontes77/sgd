import { Box, Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'

export function PageHeader({
  title,
  description,
  action,
  eyebrow,
}: {
  title: string
  description?: ReactNode
  action?: ReactNode
  eyebrow?: string
}) {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      justifyContent="space-between"
      alignItems={{ sm: 'center' }}
      spacing={2}
    >
      <Box>
        {eyebrow && (
          <Typography variant="overline" color="primary.main" fontWeight={700} sx={{ letterSpacing: '0.05em' }}>
            {eyebrow}
          </Typography>
        )}
        <Typography component="h1" variant="h4">
          {title}
        </Typography>
        {description && (
          <Typography color="text.secondary" sx={{ mt: 0.5 }}>
            {description}
          </Typography>
        )}
      </Box>
      {action && <Box sx={{ flexShrink: 0 }}>{action}</Box>}
    </Stack>
  )
}
