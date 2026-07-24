import './theme.tokens'

import type { ThemeOptions } from '@mui/material/styles'
import { alpha, createTheme } from '@mui/material/styles'

const primary = '#2A3FBB'
const primaryDark = '#1E2D8A'
const primaryLight = '#4F6DFF'
const secondary = '#00A3A5'
const secondaryDark = '#007A7C'
const secondaryLight = '#33E1E3'

const light = {
  background: { default: '#F7F9FE', paper: '#FFFFFF' },
  text: { primary: '#0B1221', secondary: '#4B5563' },
  divider: '#D1D5F2',
  surface: {
    elevated: '#FFFFFF',
    glass: '#FFFFFF',
    sunken: '#F0F3FA',
  },
  border: {
    subtle: alpha(primary, 0.08),
    strong: alpha(primary, 0.14),
  },
}

const dark = {
  background: { default: '#0B1221', paper: '#121B2E' },
  text: { primary: '#F0F4FF', secondary: '#A0AEC0' },
  divider: '#1E293B',
  surface: {
    elevated: '#121B2E',
    glass: '#121B2E',
    sunken: '#080C17',
  },
  border: {
    subtle: alpha('#FFFFFF', 0.08),
    strong: alpha('#FFFFFF', 0.14),
  },
}

export function createAppTheme(mode: 'light' | 'dark' = 'light') {
  const palette = mode === 'light' ? light : dark

  return createTheme({
    palette: {
      mode,
      primary: {
        main: primary,
        dark: primaryDark,
        light: primaryLight,
        contrastText: '#FFFFFF',
      },
      secondary: {
        main: secondary,
        dark: secondaryDark,
        light: secondaryLight,
      },
      background: palette.background,
      text: palette.text,
      divider: palette.divider,
      success: { main: '#0E9F6E' },
      warning: { main: '#F59E0B' },
      error: { main: '#EF4444' },
      info: { main: '#3B82F6' },
    },
    shape: { borderRadius: 12 },
    spacing: 8,
    typography: {
      fontFamily: 'Inter, "Plus Jakarta Sans", Arial, sans-serif',
      h1: { fontSize: '2.25rem', fontWeight: 700, lineHeight: 1.2, letterSpacing: '-0.03em' },
      h2: { fontSize: '1.75rem', fontWeight: 700, lineHeight: 1.25, letterSpacing: '-0.02em' },
      h3: { fontSize: '1.35rem', fontWeight: 650, lineHeight: 1.3 },
      h4: { fontSize: '1.75rem', fontWeight: 700, lineHeight: 1.25, letterSpacing: '-0.02em' },
      h5: { fontSize: '1.25rem', fontWeight: 650, lineHeight: 1.35 },
      h6: { fontSize: '1rem', fontWeight: 650, lineHeight: 1.45 },
      button: { fontWeight: 600, textTransform: 'none' },
    },
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          html: { WebkitTextSizeAdjust: '100%' },
          body: { minWidth: 320 },
          '::selection': { backgroundColor: alpha(primary, 0.2) },
        },
      },
      MuiPaper: { styleOverrides: { root: { backgroundImage: 'none' } } },
      MuiButton: {
        defaultProps: { disableElevation: true },
        styleOverrides: {
          root: { minHeight: 40, borderRadius: 10, paddingInline: 18 },
          sizeSmall: { minHeight: 36 },
          sizeLarge: { minHeight: 48 },
          containedPrimary: {
            background: `linear-gradient(135deg, ${primary} 0%, ${primaryLight} 100%)`,
            '&:hover': { background: `linear-gradient(135deg, ${primaryDark} 0%, ${primary} 100%)` },
          },
        },
      },
      MuiIconButton: {
        styleOverrides: { root: { borderRadius: 10, minWidth: 40, minHeight: 40 } },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            borderRadius: 16,
            border: '1px solid transparent',
            boxShadow: '0 4px 24px rgba(11, 18, 33, 0.04)',
          },
        },
      },
      MuiDialog: { styleOverrides: { paper: { borderRadius: 16 } } },
      MuiTextField: { defaultProps: { size: 'small' } },
      MuiFormControl: { defaultProps: { size: 'small' } },
      MuiOutlinedInput: {
        styleOverrides: {
          root: { borderRadius: 10 },
        },
      },
      MuiTableCell: {
        styleOverrides: {
          head: {
            color: palette.text.secondary,
            fontWeight: 650,
            backgroundColor: palette.surface.sunken,
            whiteSpace: 'nowrap',
          },
          root: { borderColor: palette.divider },
        },
      },
      MuiChip: {
        styleOverrides: {
          root: { fontWeight: 600, borderRadius: 8 },
        },
      },
      MuiAlert: { styleOverrides: { root: { borderRadius: 10 } } },
      MuiTooltip: {
        styleOverrides: { tooltip: { borderRadius: 8, fontSize: '0.75rem' } },
      },
      MuiSkeleton: { defaultProps: { animation: 'wave' } },
    },
    app: {
      glass: alpha(palette.surface.glass, 0.72),
      glow: `0 0 24px ${alpha(primary, 0.18)}`,
      surface: {
        elevated: palette.surface.elevated,
        glass: alpha(palette.surface.glass, 0.72),
        sunken: palette.surface.sunken,
      },
      border: palette.border,
      shadow: {
        card: '0 4px 24px rgba(11, 18, 33, 0.04)',
        cardHover: '0 8px 32px rgba(11, 18, 33, 0.08)',
        drawer: '0 12px 40px rgba(11, 18, 33, 0.12)',
        popover: '0 16px 48px rgba(11, 18, 33, 0.16)',
      },
      gradient: {
        hero: `linear-gradient(145deg, ${primaryDark} 0%, ${primary} 52%, ${secondary} 140%)`,
        primary: `linear-gradient(135deg, ${primary} 0%, ${primaryLight} 100%)`,
        surface: `linear-gradient(180deg, ${palette.surface.elevated} 0%, ${palette.surface.sunken} 100%)`,
      },
    },
  } as ThemeOptions)
}

export const appTheme = createAppTheme('light')
