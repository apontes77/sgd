import { Alert, Box, Button, Container, Paper, TextField, Typography } from '@mui/material'
import { FormEvent, useState } from 'react'
import OrganizationManagement from './OrganizationManagement'

export default function App() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [authenticated, setAuthenticated] = useState(() => Boolean(sessionStorage.getItem('sgd.access-token')))
  async function login(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setLoading(true)
    try {
      const response = await fetch('/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }) })
      if (!response.ok) throw new Error('Credenciais inválidas ou usuário inativo.')
      const tokens = await response.json() as { accessToken: string; refreshToken: string }
      sessionStorage.setItem('sgd.access-token', tokens.accessToken); sessionStorage.setItem('sgd.refresh-token', tokens.refreshToken)
      setAuthenticated(true)
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível entrar.') } finally { setLoading(false) }
  }
  if (authenticated) return <OrganizationManagement />
  return <Box component="main" sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', p: 3 }}><Container maxWidth="sm"><Paper component="form" onSubmit={login} elevation={1} sx={{ p: 4, textAlign: 'center' }}>
    <Typography component="h1" variant="h4" color="primary" gutterBottom>SGD</Typography><Typography variant="h6" gutterBottom>Sistema de Gerenciamento de Discipulados</Typography>
    <Typography color="text.secondary" sx={{ mb: 3 }}>Entre com suas credenciais para acessar o sistema.</Typography>
    {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
    <TextField fullWidth required type="email" label="E-mail" value={email} onChange={(event) => setEmail(event.target.value)} sx={{ mb: 2 }} />
    <TextField fullWidth required type="password" label="Senha" value={password} onChange={(event) => setPassword(event.target.value)} sx={{ mb: 3 }} />
    <Button fullWidth type="submit" variant="contained" disabled={loading}>{loading ? 'Entrando...' : 'Entrar'}</Button>
  </Paper></Container></Box>
}
