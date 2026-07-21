import { alpha, createTheme } from '@mui/material/styles'

const primary = '#3451B2'
const primaryDark = '#243B80'
const secondary = '#0F8B8D'

export const appTheme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: primary, dark: primaryDark, light: '#6478D3', contrastText: '#FFFFFF' },
    secondary: { main: secondary, dark: '#0B6668', light: '#4EA8AA' },
    background: { default: '#F5F7FB', paper: '#FFFFFF' },
    text: { primary: '#172033', secondary: '#667085' },
    divider: '#E2E8F0',
    success: { main: '#2E7D32' },
    warning: { main: '#B76E00' },
    error: { main: '#C62828' },
    info: { main: '#2563A9' },
  },
  shape: { borderRadius: 12 },
  spacing: 8,
  typography: {
    fontFamily: 'Inter, Arial, sans-serif',
    h1: { fontSize: '2rem', fontWeight: 700, lineHeight: 1.2, letterSpacing: '-0.025em' },
    h2: { fontSize: '1.5rem', fontWeight: 700, lineHeight: 1.3, letterSpacing: '-0.02em' },
    h3: { fontSize: '1.25rem', fontWeight: 650, lineHeight: 1.35 },
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
        root: { minHeight: 44, borderRadius: 9, paddingInline: 16 },
        sizeSmall: { minHeight: 44 },
      },
    },
    MuiIconButton: { styleOverrides: { root: { borderRadius: 9, minWidth: 44, minHeight: 44 } } },
    MuiCard: { styleOverrides: { root: { border: '1px solid #E2E8F0', boxShadow: '0 1px 3px rgba(23, 32, 51, 0.06)' } } },
    MuiDialog: { styleOverrides: { paper: { borderRadius: 16 } } },
    MuiTextField: { defaultProps: { size: 'small' } },
    MuiFormControl: { defaultProps: { size: 'small' } },
    MuiTableCell: {
      styleOverrides: {
        head: { color: '#475467', fontWeight: 650, backgroundColor: '#F8FAFC', whiteSpace: 'nowrap' },
        root: { borderColor: '#E2E8F0' },
      },
    },
    MuiChip: { styleOverrides: { root: { fontWeight: 600 } } },
    MuiAlert: { styleOverrides: { root: { borderRadius: 10 } } },
    MuiTooltip: { styleOverrides: { tooltip: { borderRadius: 8, fontSize: '0.75rem' } } },
    MuiSkeleton: { defaultProps: { animation: 'wave' } },
  },
})
