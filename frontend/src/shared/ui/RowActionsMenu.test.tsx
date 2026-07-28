import { cleanup, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { RowActionsMenu } from '@/shared/ui/RowActionsMenu'
import { render } from '@/test/test-utils'

describe('RowActionsMenu', () => {
  afterEach(() => {
    cleanup()
  })

  it('abre o menu e dispara a ação escolhida', async () => {
    const onEdit = vi.fn()
    const onInativar = vi.fn()
    render(
      <RowActionsMenu
        ariaLabel="Ações de Teste"
        actions={[
          { label: 'Editar', onClick: onEdit },
          { label: 'Inativar', onClick: onInativar, color: 'warning' },
        ]}
      />,
    )

    await userEvent.click(screen.getByRole('button', { name: 'Ações de Teste' }))
    expect(await screen.findByRole('menuitem', { name: 'Editar' })).toBeInTheDocument()
    expect(screen.getByRole('menuitem', { name: 'Inativar' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('menuitem', { name: 'Editar' }))
    expect(onEdit).toHaveBeenCalledOnce()
    expect(onInativar).not.toHaveBeenCalled()
    expect(screen.queryByRole('menuitem', { name: 'Editar' })).not.toBeInTheDocument()
  })

  it('não renderiza nada sem ações', () => {
    const { container } = render(<RowActionsMenu actions={[]} />)
    expect(container).toBeEmptyDOMElement()
  })
})
