import type { Theme as MuiTheme, ThemeOptions as MuiThemeOptions } from '@mui/material/styles'

export interface AppThemeExtensions {
  app: {
    glass: string
    glow: string
    surface: {
      elevated: string
      glass: string
      sunken: string
    }
    border: {
      subtle: string
      strong: string
    }
    shadow: {
      card: string
      cardHover: string
      drawer: string
      popover: string
    }
    gradient: {
      hero: string
      primary: string
      surface: string
    }
  }
}

declare module '@mui/material/styles' {
  interface Theme {
    app: AppThemeExtensions['app']
  }
  interface ThemeOptions {
    app: AppThemeExtensions['app']
  }
}

export type AppTheme = MuiTheme & AppThemeExtensions
export type AppThemeOptions = MuiThemeOptions & AppThemeExtensions
