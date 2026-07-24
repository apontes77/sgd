import { useContext } from 'react'

import { AppThemeContext } from '@/app/themeContext'

export function useColorMode() {
  const context = useContext(AppThemeContext)
  if (!context) {
    throw new Error('useColorMode must be used within an AppThemeProvider')
  }
  return context
}
