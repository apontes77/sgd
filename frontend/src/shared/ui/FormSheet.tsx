import { CloseRounded } from '@mui/icons-material'
import { Divider, Drawer, IconButton, Stack, Typography, useMediaQuery } from '@mui/material'
import type { FormEventHandler, ReactNode } from 'react'

export function FormSheet({
  open,
  onClose,
  title,
  children,
  actions,
  width = 520,
  icon,
  component,
  onSubmit,
}: {
  open: boolean
  onClose: () => void
  title: ReactNode
  children: ReactNode
  actions: ReactNode
  width?: number
  icon?: ReactNode
  component?: 'div' | 'form'
  onSubmit?: FormEventHandler<HTMLFormElement>
}) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  return (
    <Drawer
      anchor={mobile ? 'bottom' : 'right'}
      open={open}
      onClose={onClose}
      PaperProps={{
        component,
        onSubmit,
        sx: {
          width: mobile ? '100%' : width,
          maxWidth: '100%',
          height: mobile ? '100%' : '100%',
          borderTopLeftRadius: mobile ? '16px' : 0,
          borderTopRightRadius: mobile ? '16px' : 0,
          bgcolor: (theme) => theme.app.surface.elevated,
          backgroundImage: (theme) => theme.app.gradient.surface,
          borderLeft: '1px solid',
          borderColor: (theme) => theme.app.border.subtle,
        },
      }}
    >
      <Stack sx={{ height: '100%' }}>
        <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ px: 3, py: 2.25 }}>
          <Stack direction="row" spacing={1.25} alignItems="center" minWidth={0}>
            {icon}
            <Typography variant="h5" component="h2" noWrap>
              {title}
            </Typography>
          </Stack>
          <IconButton onClick={onClose} aria-label="Fechar">
            <CloseRounded />
          </IconButton>
        </Stack>
        <Divider />
        <Stack spacing={2.25} sx={{ p: 3, flexGrow: 1, overflowY: 'auto' }}>
          {children}
        </Stack>
        <Divider />
        <Stack direction="row" justifyContent="flex-end" spacing={1} sx={{ p: 2.5, flexShrink: 0 }}>
          {actions}
        </Stack>
      </Stack>
    </Drawer>
  )
}
