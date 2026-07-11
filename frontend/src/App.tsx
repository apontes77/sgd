import { Box, Container, Paper, Typography } from '@mui/material'

export default function App() {
  return (
    <Box component="main" sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', p: 3 }}>
      <Container maxWidth="sm">
        <Paper elevation={1} sx={{ p: 4, textAlign: 'center' }}>
          <Typography component="h1" variant="h4" color="primary" gutterBottom>
            SGD
          </Typography>
          <Typography variant="h6" gutterBottom>
            Sistema de Gerenciamento de Discipulados
          </Typography>
          <Typography color="text.secondary">
            A plataforma está preparada para receber os módulos funcionais.
          </Typography>
        </Paper>
      </Container>
    </Box>
  )
}
