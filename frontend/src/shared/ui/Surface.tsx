import { Box, type BoxProps } from '@mui/material'
import { forwardRef } from 'react'

export const Surface = forwardRef<HTMLDivElement, BoxProps>(function Surface({ sx, ...props }, ref) {
  return (
    <Box
      ref={ref}
      sx={{
        bgcolor: (theme) => theme.app.surface.glass,
        backdropFilter: 'blur(20px)',
        border: '1px solid',
        borderColor: (theme) => theme.app.border.subtle,
        ...sx,
      }}
      {...props}
    />
  )
})
