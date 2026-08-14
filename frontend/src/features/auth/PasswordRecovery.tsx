import { ArrowBackRounded, CheckRounded, CircleOutlined, LockResetRounded } from '@mui/icons-material'
import { Alert, Button, CircularProgress, Paper, Stack, TextField, Typography } from '@mui/material'
import type { FormEvent } from 'react'
import { useState } from 'react'

import { PASSWORD_MIN_LENGTH, PASSWORD_REQUIREMENTS, passwordMeetsPolicy } from '@/features/auth/passwordPolicy'
import { ApiError } from '@/shared/api/httpClient'

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

export function ForgotPassword({
  client,
  onBack,
  onSuccess,
}: {
  client: PasswordRecoveryClient
  onBack(): void
  onSuccess(): void
}) {
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      await client.request(email)
      onSuccess()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível enviar a solicitação.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Paper component="form" onSubmit={submit} variant="outlined" sx={cardSx}>
      <Stack spacing={2.5}>
        <Stack spacing={0.75}>
          <LockResetRounded color="primary" sx={{ fontSize: 36 }} />
          <Typography component="h1" variant="h4">
            Recuperar senha
          </Typography>
          <Typography color="text.secondary">
            Informe o e-mail da sua conta para receber as instruções de redefinição.
          </Typography>
        </Stack>
        {error && <Alert severity="error">{error}</Alert>}
        <TextField
          required
          autoFocus
          type="email"
          label="E-mail"
          autoComplete="email"
          value={email}
          onChange={(event) => {
            setEmail(event.target.value)
            setError('')
          }}
        />
        <Button
          type="submit"
          variant="contained"
          size="large"
          disabled={loading}
          startIcon={loading ? <CircularProgress size={18} color="inherit" /> : undefined}
        >
          {loading ? 'Enviando...' : 'Solicitar redefinição'}
        </Button>
        <Button onClick={onBack} startIcon={<ArrowBackRounded />}>
          Voltar ao login
        </Button>
      </Stack>
    </Paper>
  )
}

function resetErrorMessage(reason: unknown): string {
  if (reason instanceof ApiError) {
    if (reason.status === 401) return 'Token inválido ou expirado.'
    return reason.message
  }
  if (reason instanceof Error) return reason.message
  return 'Não foi possível redefinir a senha.'
}

function PasswordPolicyHints({ password }: { password: string }) {
  return (
    <Stack spacing={0.75} aria-live="polite">
      <Typography variant="caption" color="text.secondary">
        A senha deve conter:
      </Typography>
      {PASSWORD_REQUIREMENTS.map((requirement) => {
        const met = requirement.test(password)
        return (
          <Stack key={requirement.key} direction="row" spacing={1} alignItems="center">
            {met ? (
              <CheckRounded color="success" sx={{ fontSize: 16 }} />
            ) : (
              <CircleOutlined color="warning" sx={{ fontSize: 16 }} />
            )}
            <Typography variant="caption" color={met ? 'success.main' : 'warning.main'}>
              {requirement.label}
            </Typography>
          </Stack>
        )
      })}
    </Stack>
  )
}

export function ResetPassword({
  client,
  token,
  onSuccess,
}: {
  client: PasswordRecoveryClient
  token: string
  onSuccess(): void
}) {
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const policyOk = passwordMeetsPolicy(password)
  const confirmationOk = password.length > 0 && password === confirmation

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    if (!passwordMeetsPolicy(password)) {
      setError('A senha não atende aos requisitos.')
      return
    }
    if (password !== confirmation) {
      setError('As senhas não coincidem.')
      return
    }
    setLoading(true)
    try {
      await client.reset(token, password)
      onSuccess()
    } catch (reason) {
      setError(resetErrorMessage(reason))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Paper component="form" onSubmit={submit} variant="outlined" sx={cardSx}>
      <Stack spacing={2.5}>
        <Stack spacing={0.75}>
          <LockResetRounded color="primary" sx={{ fontSize: 36 }} />
          <Typography component="h1" variant="h4">
            Definir nova senha
          </Typography>
          <Typography color="text.secondary">
            Crie uma senha segura com pelo menos {PASSWORD_MIN_LENGTH} caracteres.
          </Typography>
        </Stack>
        {error && <Alert severity="error">{error}</Alert>}
        <TextField
          required
          autoFocus
          type="password"
          inputProps={{ minLength: PASSWORD_MIN_LENGTH }}
          label="Nova senha"
          autoComplete="new-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
        <PasswordPolicyHints password={password} />
        <TextField
          required
          type="password"
          inputProps={{ minLength: PASSWORD_MIN_LENGTH }}
          label="Confirmar senha"
          autoComplete="new-password"
          value={confirmation}
          onChange={(event) => setConfirmation(event.target.value)}
        />
        {confirmation.length > 0 && !confirmationOk && (
          <Typography variant="caption" color="error">
            As senhas não coincidem.
          </Typography>
        )}
        <Button
          type="submit"
          variant="contained"
          size="large"
          disabled={loading || !policyOk || !confirmationOk}
          startIcon={loading ? <CircularProgress size={18} color="inherit" /> : undefined}
        >
          {loading ? 'Redefinindo...' : 'Redefinir senha'}
        </Button>
      </Stack>
    </Paper>
  )
}
