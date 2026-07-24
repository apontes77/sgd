import type { ChipProps } from '@mui/material'
import { Chip } from '@mui/material'

export function StatusChip({
  active,
  activeLabel = 'Ativo',
  inactiveLabel = 'Inativo',
  ...props
}: { active: boolean; activeLabel?: string; inactiveLabel?: string } & Omit<ChipProps, 'label' | 'color'>) {
  return (
    <Chip
      size="small"
      color={active ? 'success' : 'default'}
      variant={active ? 'filled' : 'outlined'}
      label={active ? activeLabel : inactiveLabel}
      {...props}
    />
  )
}
