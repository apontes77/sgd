import { AlternateEmailRounded, GroupsRounded, InsightsRounded, LockRounded, SecurityRounded } from '@mui/icons-material'
import { Alert, Box, Button, Card, CircularProgress, InputAdornment, Stack, TextField, Typography } from '@mui/material'
import { FormEvent, useEffect, useState } from 'react'
import AuthenticatedApp from './AuthenticatedApp'
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
  async function logout() {
    try { await authApi.logout() }
    finally { setPassword(''); setCurrentUser(undefined) }
  }
  if (checkingSession) return <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Stack alignItems="center" spacing={2}><CircularProgress size={32} /><Typography color="text.secondary">Validando sessão...</Typography></Stack></Box>
  if (currentUser) return <AuthenticatedApp currentUser={currentUser} onLogout={() => void logout()} />
  return <Box component="main" sx={{ minHeight: '100vh', display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1.2fr) minmax(460px, .8fr)' }, bgcolor: 'background.paper' }}>
    <Box sx={{ display: { xs: 'none', md: 'flex' }, position: 'relative', overflow: 'hidden', p: { md: 6, lg: 9 }, color: '#fff', background: 'linear-gradient(145deg, #243B80 0%, #3451B2 52%, #0F8B8D 140%)', alignItems: 'center' }}>
      <Box sx={{ position: 'absolute', width: 480, height: 480, borderRadius: '50%', border: '1px solid rgba(255,255,255,.16)', top: -180, right: -120 }} />
      <Box sx={{ position: 'absolute', width: 320, height: 320, borderRadius: '50%', bgcolor: 'rgba(255,255,255,.05)', bottom: -120, left: -80 }} />
      <Stack spacing={5} sx={{ position: 'relative', maxWidth: 620 }}>
        <Stack direction="row" alignItems="center" spacing={1.5}><Box sx={{ width: 48, height: 48, borderRadius: 3, display: 'grid', placeItems: 'center', bgcolor: 'rgba(255,255,255,.16)', backdropFilter: 'blur(12px)' }}><GroupsRounded /></Box><Box><Typography variant="h5">SGD</Typography><Typography variant="body2" sx={{ color: 'rgba(255,255,255,.72)' }}>Gestão de discipulados</Typography></Box></Stack>
        <Box><Typography component="p" variant="h1" sx={{ fontSize: { md: '2.5rem', lg: '3.25rem' }, maxWidth: 560 }}>Cuidado próximo, decisões mais claras.</Typography><Typography sx={{ mt: 2, color: 'rgba(255,255,255,.78)', fontSize: '1.05rem', lineHeight: 1.7, maxWidth: 520 }}>Centralize frequências, acompanhe cada discipulado e transforme registros semanais em uma visão confiável da organização.</Typography></Box>
        <Stack direction="row" spacing={4}><Feature icon={<InsightsRounded />} label="Indicadores em tempo real" /><Feature icon={<SecurityRounded />} label="Acesso por perfil" /></Stack>
      </Stack>
    </Box>
    <Box sx={{ display: 'grid', placeItems: 'center', p: { xs: 2.5, sm: 5, md: 6 }, bgcolor: 'background.default' }}>
      <Card component="form" onSubmit={login} sx={{ width: '100%', maxWidth: 460, p: { xs: 3, sm: 4.5 }, borderRadius: 3, boxShadow: '0 20px 50px rgba(36,59,128,.10)' }}>
        <Stack spacing={3}>
          <Box><Stack direction="row" alignItems="center" spacing={1.25} sx={{ display: { md: 'none' }, mb: 3 }}><Box sx={{ width: 42, height: 42, borderRadius: 2.5, display: 'grid', placeItems: 'center', color: 'primary.main', bgcolor: '#EEF1FF' }}><GroupsRounded /></Box><Typography variant="h5" color="primary.dark">SGD</Typography></Stack><Typography component="h1" variant="h4">Bem-vindo de volta</Typography><Typography color="text.secondary" sx={{ mt: 1 }}>Entre com suas credenciais para acessar o sistema.</Typography></Box>
          {error && <Alert severity="error">{error}</Alert>}
          <Stack spacing={2}>
            <TextField fullWidth required type="email" label="E-mail" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} InputProps={{ startAdornment: <InputAdornment position="start"><AlternateEmailRounded fontSize="small" /></InputAdornment> }} />
            <TextField fullWidth required type="password" label="Senha" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} InputProps={{ startAdornment: <InputAdornment position="start"><LockRounded fontSize="small" /></InputAdornment> }} />
          </Stack>
          <Button fullWidth size="large" type="submit" variant="contained" disabled={loading}>{loading ? 'Entrando...' : 'Entrar no SGD'}</Button>
          <Typography variant="caption" color="text.secondary" textAlign="center">Acesso restrito a usuários autorizados.</Typography>
        </Stack>
      </Card>
    </Box>
  </Box>
}

function Feature({ icon, label }: { icon: React.ReactNode; label: string }) { return <Stack direction="row" spacing={1.25} alignItems="center"><Box sx={{ width: 38, height: 38, borderRadius: 2, display: 'grid', placeItems: 'center', bgcolor: 'rgba(255,255,255,.12)' }}>{icon}</Box><Typography variant="body2" fontWeight={600}>{label}</Typography></Stack> }
