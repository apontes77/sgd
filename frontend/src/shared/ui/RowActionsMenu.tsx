import { MoreVertRounded } from '@mui/icons-material'
import { IconButton, Menu, MenuItem } from '@mui/material'
import type { MouseEvent } from 'react'
import { useId, useState } from 'react'

export type RowAction = {
  label: string
  onClick: () => void
  color?: 'inherit' | 'primary' | 'secondary' | 'success' | 'error' | 'info' | 'warning'
  disabled?: boolean
}

export function RowActionsMenu({ ariaLabel = 'Ações', actions }: { ariaLabel?: string; actions: RowAction[] }) {
  const menuId = useId()
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null)
  const aberto = Boolean(anchorEl)

  if (actions.length === 0) return null

  function abrir(event: MouseEvent<HTMLElement>) {
    event.stopPropagation()
    event.preventDefault()
    setAnchorEl(event.currentTarget)
  }

  function fechar() {
    setAnchorEl(null)
  }

  return (
    <>
      <IconButton
        aria-label={ariaLabel}
        aria-controls={aberto ? menuId : undefined}
        aria-haspopup="true"
        aria-expanded={aberto ? 'true' : undefined}
        onClick={abrir}
        size="small"
      >
        <MoreVertRounded />
      </IconButton>
      <Menu
        id={menuId}
        anchorEl={anchorEl}
        open={aberto}
        onClose={fechar}
        onClick={(event) => event.stopPropagation()}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        {actions.map((action) => (
          <MenuItem
            key={action.label}
            disabled={action.disabled}
            onClick={() => {
              fechar()
              action.onClick()
            }}
            sx={action.color && action.color !== 'inherit' ? { color: `${action.color}.main` } : undefined}
          >
            {action.label}
          </MenuItem>
        ))}
      </Menu>
    </>
  )
}
