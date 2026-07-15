import { Alert, Box, Button, Container, Paper, TextField, Typography } from '@mui/material'
import { FormEvent, useEffect, useLayoutEffect, useState } from 'react'
import AuthenticatedApp from './AuthenticatedApp'
import { authApi, passwordRecoveryApi, Usuario } from './api'
import { ForgotPassword, ResetPassword } from './PasswordRecovery'

type AuthView = 'login' | 'forgot' | 'reset'

function resetTokenFromHash(hash: string) {
  const match = hash.match(/^#\/redefinir-senha\/([^/?#]+)$/)
  if (!match) return undefined
  try { return decodeURIComponent(match[1]) } catch { return undefined }
}

export default function App() {
  const [resetToken, setResetToken] = useState(() => resetTokenFromHash(window.location.hash))
  const [view, setView] = useState<AuthView>(() => resetTokenFromHash(window.location.hash) ? 'reset' : 'login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [currentUser, setCurrentUser] = useState<Usuario>()
  const [checkingSession, setCheckingSession] = useState(authApi.hasSession() && !resetToken)

  useLayoutEffect(() => {
    if (resetToken) window.history.replaceState(window.history.state, '', `${window.location.pathname}${window.location.search}`)
  }, [resetToken])

  useEffect(() => {
    const expire = () => { setCurrentUser(undefined); setCheckingSession(false) }
    window.addEventListener('sgd:session-expired', expire)
    if (!resetToken && authApi.hasSession()) authApi.me().then(setCurrentUser).catch(() => authApi.logoutLocal()).finally(() => setCheckingSession(false))
    return () => window.removeEventListener('sgd:session-expired', expire)
  }, [resetToken])

  async function login(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setMessage(''); setLoading(true)
    try {
      const user = await authApi.login(email, password)
      setCurrentUser(user ?? await authApi.me())
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível entrar.') } finally { setLoading(false) }
  }

  async function logout() {
    try { await authApi.logout() }
    finally { setPassword(''); setCurrentUser(undefined) }
  }

  function showLogin() { setView('login'); setError(''); setMessage('') }
  function resetSucceeded() {
    setResetToken(undefined); setView('login'); setPassword('')
    setMessage('Senha definida com sucesso. Entre com a sua nova senha.')
  }

  if (checkingSession) return <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Typography>Validando sessão...</Typography></Box>
  if (currentUser && view === 'login') return <AuthenticatedApp currentUser={currentUser} onLogout={() => void logout()} />

  return <Box component="main" sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', p: 3 }}><Container maxWidth="sm">
    {view === 'forgot' && <ForgotPassword client={passwordRecoveryApi} onBack={showLogin} />}
    {view === 'reset' && resetToken && <ResetPassword client={passwordRecoveryApi} token={resetToken} onSuccess={resetSucceeded} />}
    {view === 'login' && <Paper component="form" onSubmit={login} elevation={1} sx={{ p: 4, textAlign: 'center' }}>
      <Typography component="h1" variant="h4" color="primary" gutterBottom>SGD</Typography><Typography variant="h6" gutterBottom>Sistema de Gerenciamento de Discipulados</Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>Entre com suas credenciais para acessar o sistema.</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {message && <Alert severity="success" sx={{ mb: 2 }}>{message}</Alert>}
      <TextField fullWidth required disabled={loading} type="email" label="E-mail" value={email} onChange={(event) => setEmail(event.target.value)} sx={{ mb: 2 }} />
      <TextField fullWidth required disabled={loading} type="password" label="Senha" value={password} onChange={(event) => setPassword(event.target.value)} sx={{ mb: 2 }} />
      <Button fullWidth type="submit" variant="contained" disabled={loading}>{loading ? 'Entrando...' : 'Entrar'}</Button>
      <Button type="button" disabled={loading} onClick={() => { setView('forgot'); setError(''); setMessage('') }} sx={{ mt: 1 }}>Esqueci minha senha</Button>
    </Paper>}
  </Container></Box>
}
