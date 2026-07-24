import type { SxProps, Theme } from '@mui/material'
import { Box, Paper, Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'

export function SectionCard({
  title,
  description,
  icon,
  action,
  children,
  sx,
}: {
  title?: string
  description?: ReactNode
  icon?: ReactNode
  action?: ReactNode
  children: ReactNode
  sx?: SxProps<Theme>
}) {
  return (
    <Paper
      variant="outlined"
      sx={{
        overflow: 'hidden',
        borderRadius: '16px',
        borderColor: (theme) => theme.app.border.subtle,
        boxShadow: (theme) => theme.app.shadow.card,
        ...sx,
      }}
    >
      {(title || action) && (
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="flex-start"
          gap={2}
          sx={{ px: { xs: 2, sm: 2.5 }, pt: 2.25, pb: description ? 1.5 : 2 }}
        >
          <Stack direction="row" spacing={1.25} alignItems="flex-start">
            <Box sx={{ color: 'primary.main', display: 'grid', placeItems: 'center', mt: 0.15 }}>{icon}</Box>
            <Box>
              {title && (
                <Typography component="h2" variant="h6">
                  {title}
                </Typography>
              )}
              {description && (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.25 }}>
                  {description}
                </Typography>
              )}
            </Box>
          </Stack>
          {action}
        </Stack>
      )}
      <Box sx={{ p: { xs: 2, sm: 2.5 }, pt: title || action ? 0 : { xs: 2, sm: 2.5 } }}>{children}</Box>
    </Paper>
  )
}
