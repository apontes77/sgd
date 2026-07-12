import { Alert, Box, Button, Checkbox, Chip, FormControlLabel, MenuItem, Paper, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography } from '@mui/material'
import { FormEvent, useEffect, useState } from 'react'
import type { Pagina, Perfil, Usuario } from './api'

export interface UserManagementClient {
  list(page: number, size: number, active?: boolean): Promise<Pagina<Usuario>>
  create(body: { nome: string; email: string; senha: string; perfis: Perfil[] }): Promise<Usuario>
  update(id: number, body: { nome?: string; perfis?: Perfil[]; ativo?: boolean }): Promise<Usuario>
}

const roles: Perfil[] = ['ADMIN', 'GERENTE', 'DISCIPULADOR', 'CO_LIDER']

export default function UserManagement({ client }: { client: UserManagementClient }) {
  const [result, setResult] = useState<Pagina<Usuario>>({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
  const [activeFilter, setActiveFilter] = useState('')
  const [form, setForm] = useState({ nome: '', email: '', senha: '', perfis: [] as Perfil[] })
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  async function load(page = result.page) {
    setLoading(true); setError('')
    try { setResult(await client.list(page, result.size, activeFilter === '' ? undefined : activeFilter === 'true')) }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível carregar os usuários.') }
    finally { setLoading(false) }
  }
  useEffect(() => { void load(0) }, [activeFilter]) // eslint-disable-line react-hooks/exhaustive-deps

  async function submit(event: FormEvent) {
    event.preventDefault(); setError(''); setMessage(''); setLoading(true)
    try {
      await client.create(form); setForm({ nome: '', email: '', senha: '', perfis: [] }); setMessage('Usuário criado com sucesso.'); await load(0)
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível criar o usuário.') }
    finally { setLoading(false) }
  }
  async function toggleActive(user: Usuario) {
    setError(''); setMessage('')
    try { await client.update(user.id, { ativo: !user.ativo }); setMessage(user.ativo ? 'Usuário inativado e sessões revogadas.' : 'Usuário reativado.'); await load() }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível atualizar o usuário.') }
  }

  return <Box sx={{ display: 'grid', gap: 3 }}>
    <Typography variant="h5">Gestão de usuários</Typography>
    {error && <Alert severity="error">{error}</Alert>}{message && <Alert severity="success">{message}</Alert>}
    <Paper component="form" onSubmit={submit} sx={{ p: 3, display: 'grid', gap: 2 }}>
      <Typography variant="h6">Novo usuário</Typography>
      <TextField required label="Nome" value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} />
      <TextField required type="email" label="E-mail" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
      <TextField required type="password" inputProps={{ minLength: 12 }} label="Senha inicial" helperText="Mínimo de 12 caracteres" value={form.senha} onChange={(e) => setForm({ ...form, senha: e.target.value })} />
      <Box>{roles.map((role) => <FormControlLabel key={role} control={<Checkbox checked={form.perfis.includes(role)} onChange={(_, checked) => setForm({ ...form, perfis: checked ? [...form.perfis, role] : form.perfis.filter((value) => value !== role) })} />} label={role} />)}</Box>
      <Button type="submit" variant="contained" disabled={loading || form.perfis.length === 0}>Cadastrar usuário</Button>
    </Paper>
    <TextField select label="Status" value={activeFilter} onChange={(e) => setActiveFilter(e.target.value)} sx={{ maxWidth: 240 }}>
      <MenuItem value="">Todos</MenuItem><MenuItem value="true">Ativos</MenuItem><MenuItem value="false">Inativos</MenuItem>
    </TextField>
    <Paper><Table><TableHead><TableRow><TableCell>Nome</TableCell><TableCell>E-mail</TableCell><TableCell>Perfis</TableCell><TableCell>Status</TableCell><TableCell /></TableRow></TableHead>
      <TableBody>{result.content.map((user) => <TableRow key={user.id}><TableCell>{user.nome}</TableCell><TableCell>{user.email}</TableCell><TableCell>{user.perfis.map((role) => <Chip key={role} label={role} size="small" sx={{ mr: 0.5 }} />)}</TableCell><TableCell>{user.ativo ? 'Ativo' : 'Inativo'}</TableCell><TableCell><Button onClick={() => void toggleActive(user)}>{user.ativo ? 'Inativar' : 'Reativar'}</Button></TableCell></TableRow>)}</TableBody>
    </Table></Paper>
    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}><Button disabled={result.page === 0 || loading} onClick={() => void load(result.page - 1)}>Anterior</Button><Typography>Página {result.page + 1} de {Math.max(1, result.totalPages)}</Typography><Button disabled={result.page + 1 >= result.totalPages || loading} onClick={() => void load(result.page + 1)}>Próxima</Button></Box>
  </Box>
}
