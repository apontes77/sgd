import { Alert, Button, Paper, TextField, Typography } from '@mui/material'
import { FormEvent, useState } from 'react'

export interface PasswordRecoveryClient {
  request(email: string): Promise<void>
  reset(token: string, newPassword: string): Promise<void>
}

export function ForgotPassword({ client, onBack }: { client: PasswordRecoveryClient; onBack(): void }) {
  const [email, setEmail] = useState(''); const [sent, setSent] = useState(false); const [error, setError] = useState(''); const [loading, setLoading] = useState(false)
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (loading) return
    setError(''); setSent(false); setLoading(true)
    try { await client.request(email); setSent(true) }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível enviar a solicitação.') }
    finally { setLoading(false) }
  }
  return <Paper component="form" onSubmit={submit} sx={{ p: 4, display: 'grid', gap: 2 }}><Typography variant="h5">Recuperar senha</Typography>{sent && <Alert severity="success">Se o e-mail estiver cadastrado e ativo, você receberá as instruções.</Alert>}{error && <Alert severity="error">{error}</Alert>}<TextField required disabled={loading} type="email" label="E-mail" value={email} onChange={(e) => setEmail(e.target.value)} /><Button type="submit" variant="contained" disabled={loading}>{loading ? 'Enviando...' : 'Solicitar redefinição'}</Button><Button type="button" disabled={loading} onClick={onBack}>Voltar ao login</Button></Paper>
}

export function ResetPassword({ client, token, onSuccess }: { client: PasswordRecoveryClient; token: string; onSuccess(): void }) {
  const [password, setPassword] = useState(''); const [confirmation, setConfirmation] = useState(''); const [error, setError] = useState(''); const [loading, setLoading] = useState(false)
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (loading) return
    setError('')
    if (password !== confirmation) { setError('As senhas não coincidem.'); return }
    const passwordBytes = new TextEncoder().encode(password).length
    if (passwordBytes < 12 || passwordBytes > 72) { setError('A senha deve ter entre 12 e 72 bytes.'); return }
    setLoading(true)
    try { await client.reset(token, password); onSuccess() }
    catch { setError('O link de redefinição é inválido ou expirou. Solicite um novo link.') }
    finally { setLoading(false) }
  }
  return <Paper component="form" onSubmit={submit} sx={{ p: 4, display: 'grid', gap: 2 }}><Typography variant="h5">Definir nova senha</Typography><Typography color="text.secondary">Use uma senha com 12 a 72 bytes.</Typography>{error && <Alert severity="error">{error}</Alert>}<TextField required disabled={loading} type="password" label="Nova senha" value={password} onChange={(e) => setPassword(e.target.value)} /><TextField required disabled={loading} type="password" label="Confirmar senha" value={confirmation} onChange={(e) => setConfirmation(e.target.value)} /><Button type="submit" variant="contained" disabled={loading}>{loading ? 'Redefinindo...' : 'Redefinir senha'}</Button></Paper>
}
