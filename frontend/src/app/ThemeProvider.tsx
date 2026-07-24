import { CssBaseline, ThemeProvider as MuiThemeProvider } from '@mui/material'
import { type ReactNode, useEffect, useMemo, useState } from 'react'

import { createAppTheme } from '@/app/theme'
import { AppThemeContext } from '@/app/themeContext'

type ColorMode = 'light' | 'dark'

const STORAGE_KEY = 'sgd:color-mode'

function getInitialMode(): ColorMode {
  if (typeof window === 'undefined') return 'light'
  const stored = window.localStorage.getItem(STORAGE_KEY)
  if (stored === 'dark' || stored === 'light') return stored
  return 'light'
}

export function AppThemeProvider({ children }: { children: ReactNode }) {
  const [mode, setModeState] = useState<ColorMode>(getInitialMode)
  const theme = useMemo(() => createAppTheme(mode), [mode])

  useEffect(() => {
    window.localStorage.setItem(STORAGE_KEY, mode)
  }, [mode])

  const toggleMode = () => setModeState((current) => (current === 'light' ? 'dark' : 'light'))
  const setMode = (next: ColorMode) => setModeState(next)

  return (
    <AppThemeContext.Provider value={{ mode, toggleMode, setMode }}>
      <MuiThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </MuiThemeProvider>
    </AppThemeContext.Provider>
  )
}
