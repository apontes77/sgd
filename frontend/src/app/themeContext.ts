import { createContext } from 'react'

type ColorMode = 'light' | 'dark'

export interface AppThemeContextValue {
  mode: ColorMode
  toggleMode: () => void
  setMode: (mode: ColorMode) => void
}

export const AppThemeContext = createContext<AppThemeContextValue | undefined>(undefined)
