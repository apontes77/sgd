import { Alert, Button, Paper, TextField, Typography } from '@mui/material'
import { FormEvent, useState } from 'react'

export interface PasswordRecoveryClient {
  request(email: string): Promise<void>
  reset(token: string, newPassword: string): Promise<void>
}

export function ForgotPassword({ client, onBack }: { client: PasswordRecoveryClient; onBack(): void }) {
  const [email, setEmail] = useState(''); const [sent, setSent] = useState(false); const [error, setError] = useState('')
  async function submit(event: FormEvent) { event.preventDefault(); setError(''); try { await client.request(email); setSent(true) } catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível enviar a solicitação.') } }
  return <Paper component="form" onSubmit={submit} sx={{ p: 4, display: 'grid', gap: 2 }}><Typography variant="h5">Recuperar senha</Typography>{sent && <Alert severity="success">Se o e-mail estiver cadastrado e ativo, você receberá as instruções.</Alert>}{error && <Alert severity="error">{error}</Alert>}<TextField required type="email" label="E-mail" value={email} onChange={(e) => setEmail(e.target.value)} /><Button type="submit" variant="contained">Solicitar redefinição</Button><Button onClick={onBack}>Voltar ao login</Button></Paper>
}

export function ResetPassword({ client, token, onSuccess }: { client: PasswordRecoveryClient; token: string; onSuccess(): void }) {
  const [password, setPassword] = useState(''); const [confirmation, setConfirmation] = useState(''); const [error, setError] = useState('')
  async function submit(event: FormEvent) { event.preventDefault(); setError(''); if (password !== confirmation) { setError('As senhas não coincidem.'); return } try { await client.reset(token, password); onSuccess() } catch (reason) { setError(reason instanceof Error ? reason.message : 'Token inválido ou expirado.') } }
  return <Paper component="form" onSubmit={submit} sx={{ p: 4, display: 'grid', gap: 2 }}><Typography variant="h5">Definir nova senha</Typography>{error && <Alert severity="error">{error}</Alert>}<TextField required type="password" inputProps={{ minLength: 12 }} label="Nova senha" value={password} onChange={(e) => setPassword(e.target.value)} /><TextField required type="password" label="Confirmar senha" value={confirmation} onChange={(e) => setConfirmation(e.target.value)} /><Button type="submit" variant="contained">Redefinir senha</Button></Paper>
}
