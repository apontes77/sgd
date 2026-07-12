import { Alert, Box, Button, Container, Paper, TextField, Typography } from '@mui/material'
import { FormEvent, useEffect, useState } from 'react'
import OrganizationManagement from './OrganizationManagement'
import { authApi, Usuario } from './api'

export default function App() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [currentUser, setCurrentUser] = useState<Usuario>()
  const [checkingSession, setCheckingSession] = useState(authApi.hasSession())
  useEffect(() => {
    const expire = () => { setCurrentUser(undefined); setCheckingSession(false) }
    window.addEventListener('sgd:session-expired', expire)
    if (authApi.hasSession()) authApi.me().then(setCurrentUser).catch(() => authApi.logoutLocal()).finally(() => setCheckingSession(false))
    return () => window.removeEventListener('sgd:session-expired', expire)
  }, [])
  async function login(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setLoading(true)
    try {
      const user = await authApi.login(email, password)
      setCurrentUser(user ?? await authApi.me())
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível entrar.') } finally { setLoading(false) }
  }
  function logout() {
    authApi.logoutLocal()
    setPassword('')
    setCurrentUser(undefined)
  }
  if (checkingSession) return <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Typography>Validando sessão...</Typography></Box>
  if (currentUser) return <OrganizationManagement currentUser={currentUser} onLogout={logout} />
  return <Box component="main" sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', p: 3 }}><Container maxWidth="sm"><Paper component="form" onSubmit={login} elevation={1} sx={{ p: 4, textAlign: 'center' }}>
    <Typography component="h1" variant="h4" color="primary" gutterBottom>SGD</Typography><Typography variant="h6" gutterBottom>Sistema de Gerenciamento de Discipulados</Typography>
    <Typography color="text.secondary" sx={{ mb: 3 }}>Entre com suas credenciais para acessar o sistema.</Typography>
    {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
    <TextField fullWidth required type="email" label="E-mail" value={email} onChange={(event) => setEmail(event.target.value)} sx={{ mb: 2 }} />
    <TextField fullWidth required type="password" label="Senha" value={password} onChange={(event) => setPassword(event.target.value)} sx={{ mb: 3 }} />
    <Button fullWidth type="submit" variant="contained" disabled={loading}>{loading ? 'Entrando...' : 'Entrar'}</Button>
  </Paper></Container></Box>
}
