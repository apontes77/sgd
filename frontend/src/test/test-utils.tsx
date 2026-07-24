/* eslint-disable react-refresh/only-export-components */
import { render as rtlRender, type RenderOptions } from '@testing-library/react'
import type { ReactElement, ReactNode } from 'react'

import { AppThemeProvider } from '@/app/ThemeProvider'

function AllProviders({ children }: { children: ReactNode }) {
  return <AppThemeProvider>{children}</AppThemeProvider>
}

export function render(ui: ReactElement, options?: Omit<RenderOptions, 'wrapper'>) {
  return rtlRender(ui, { wrapper: AllProviders, ...options })
}

export * from '@testing-library/react'
