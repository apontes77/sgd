import { ArrowBackRounded, LockResetRounded } from '@mui/icons-material'
import { Alert, Button, CircularProgress, Paper, Stack, TextField, Typography } from '@mui/material'
import { FormEvent, useState } from 'react'

export interface PasswordRecoveryClient {
  request(email: string): Promise<void>
  reset(token: string, newPassword: string): Promise<void>
}

const cardSx = {
  width: '100%',
  maxWidth: 480,
  p: { xs: 3, sm: 4 },
  borderRadius: 3,
  boxShadow: '0 20px 50px rgba(23, 32, 51, 0.10)',
}

export function ForgotPassword({ client, onBack }: { client: PasswordRecoveryClient; onBack(): void }) {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      await client.request(email)
      setSent(true)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível enviar a solicitação.')
    } finally {
      setLoading(false)
    }
  }

  return <Paper component="form" onSubmit={submit} variant="outlined" sx={cardSx}>
    <Stack spacing={2.5}>
      <Stack spacing={0.75}>
        <LockResetRounded color="primary" sx={{ fontSize: 36 }} />
        <Typography component="h1" variant="h4">Recuperar senha</Typography>
        <Typography color="text.secondary">Informe o e-mail da sua conta para receber as instruções de redefinição.</Typography>
      </Stack>
      {sent && <Alert severity="success">Se o e-mail estiver cadastrado e ativo, você receberá as instruções.</Alert>}
      {error && <Alert severity="error">{error}</Alert>}
      <TextField required autoFocus type="email" label="E-mail" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} />
      <Button type="submit" variant="contained" size="large" disabled={loading} startIcon={loading ? <CircularProgress size={18} color="inherit" /> : undefined}>
        {loading ? 'Enviando...' : 'Solicitar redefinição'}
      </Button>
      <Button onClick={onBack} startIcon={<ArrowBackRounded />}>Voltar ao login</Button>
    </Stack>
  </Paper>
}

export function ResetPassword({ client, token, onSuccess }: { client: PasswordRecoveryClient; token: string; onSuccess(): void }) {
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    if (password !== confirmation) {
      setError('As senhas não coincidem.')
      return
    }
    setLoading(true)
    try {
      await client.reset(token, password)
      onSuccess()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Token inválido ou expirado.')
    } finally {
      setLoading(false)
    }
  }

  return <Paper component="form" onSubmit={submit} variant="outlined" sx={cardSx}>
    <Stack spacing={2.5}>
      <Stack spacing={0.75}>
        <LockResetRounded color="primary" sx={{ fontSize: 36 }} />
        <Typography component="h1" variant="h4">Definir nova senha</Typography>
        <Typography color="text.secondary">Crie uma senha segura com pelo menos 12 caracteres.</Typography>
      </Stack>
      {error && <Alert severity="error">{error}</Alert>}
      <TextField required autoFocus type="password" inputProps={{ minLength: 12 }} label="Nova senha" autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} />
      <TextField required type="password" inputProps={{ minLength: 12 }} label="Confirmar senha" autoComplete="new-password" value={confirmation} onChange={(event) => setConfirmation(event.target.value)} />
      <Button type="submit" variant="contained" size="large" disabled={loading} startIcon={loading ? <CircularProgress size={18} color="inherit" /> : undefined}>
        {loading ? 'Redefinindo...' : 'Redefinir senha'}
      </Button>
    </Stack>
  </Paper>
}
