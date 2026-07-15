import { AddRounded } from '@mui/icons-material'
import {
  Alert, Box, Button, Checkbox, Chip, Dialog, DialogActions, DialogContent,
  DialogTitle, FormControl, FormControlLabel, InputLabel, List, ListItem, ListItemText,
  MenuItem, Paper, Select, Stack, Tab, Tabs, TextField, Typography,
} from '@mui/material'
import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react'
import {
  CriarUsuarioRequest, Discipulado, DiscipuladoRequest, Gerencia, GerenciaRequest, Perfil, Usuario, organizationApi,
} from './api'
import { LoadingState, PageHeader } from './ui'

type Modal = { kind: 'gerencia'; item?: Gerencia } | { kind: 'discipulado'; item?: Discipulado } | undefined

const roleLabel: Record<Perfil, string> = {
  ADMIN: 'Administrador', GERENTE: 'Gerente', DISCIPULADOR: 'Discipulador', CO_LIDER: 'Co-líder',
}

function userName(users: Usuario[], id: number) {
  return users.find((user) => user.id === id)?.nome ?? `Usuário #${id}`
}

export default function OrganizationManagement() {
  const [tab, setTab] = useState(0)
  const [users, setUsers] = useState<Usuario[]>([])
  const [gerencias, setGerencias] = useState<Gerencia[]>([])
  const [discipulados, setDiscipulados] = useState<Discipulado[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modal, setModal] = useState<Modal>()
  const [saving, setSaving] = useState(false)
  const [pendingDiscipuladoId, setPendingDiscipuladoId] = useState<number>()
  const [pendingDeactivate, setPendingDeactivate] = useState<Discipulado>()

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const [userPage, gerenciaPage, discipuladoPage] = await Promise.all([
        organizationApi.listarUsuarios(), organizationApi.listarGerencias(), organizationApi.listarDiscipulados(),
      ])
      setUsers(userPage.content); setGerencias(gerenciaPage.content); setDiscipulados(discipuladoPage.content)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível carregar a estrutura organizacional.')
    } finally { setLoading(false) }
  }, [])

  useEffect(() => { void load() }, [load])

  const gerentes = useMemo(() => users.filter((user) => user.ativo !== false && (user.perfis.includes('GERENTE') || user.perfis.includes('ADMIN'))), [users])
  const discipuladores = useMemo(() => users.filter((user) => user.ativo !== false && (user.perfis.includes('DISCIPULADOR') || user.perfis.includes('ADMIN'))), [users])
  const coLideres = useMemo(() => users.filter((user) => user.ativo !== false && (user.perfis.includes('CO_LIDER') || user.perfis.includes('ADMIN'))), [users])

  async function saveGerencia(body: GerenciaRequest) {
    setSaving(true); setError('')
    try {
      if (modal?.item) await organizationApi.atualizarGerencia(modal.item.id, body)
      else await organizationApi.criarGerencia(body)
      setModal(undefined); await load()
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível salvar a gerência.') } finally { setSaving(false) }
  }

  async function createUser(body: CriarUsuarioRequest) {
    const created = await organizationApi.criarUsuario(body)
    setUsers((current) => [...current.filter((user) => user.id !== created.id), created])
    return created
  }

  async function saveDiscipulado(body: DiscipuladoRequest, coLiderIds: number[]) {
    setSaving(true); setError('')
    try {
      const existingId = modal?.item?.id ?? pendingDiscipuladoId
      const saved = existingId
        ? await organizationApi.atualizarDiscipulado(existingId, body)
        : await organizationApi.criarDiscipulado(body)
      setPendingDiscipuladoId(saved.id)
      await organizationApi.definirCoLideres(saved.id, coLiderIds)
      setPendingDiscipuladoId(undefined); setModal(undefined); await load()
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível salvar o discipulado.') } finally { setSaving(false) }
  }

  async function deactivate() {
    if (!pendingDeactivate) return
    const item = pendingDeactivate
    setSaving(true); setError('')
    try {
      await organizationApi.atualizarDiscipulado(item.id, { nome: item.nome, sexo: item.sexo, gerenciaId: item.gerenciaId, discipuladorId: item.discipuladorId, ativo: false })
      setPendingDeactivate(undefined); await load()
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível inativar o discipulado.') } finally { setSaving(false) }
  }

  return <Stack spacing={3}>
    <PageHeader title="Estrutura organizacional" description="Gerencie gerências, discipulados e suas lideranças." eyebrow="Gestão" action={<Button variant="contained" startIcon={<AddRounded />} onClick={() => { setPendingDiscipuladoId(undefined); setModal(tab === 0 ? { kind: 'gerencia' } : { kind: 'discipulado' }) }}>{tab === 0 ? 'Nova gerência' : 'Novo discipulado'}</Button>} />
    {error && <Alert severity="error" onClose={() => setError('')} sx={{ mb: 2 }}>{error}</Alert>}
    <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
      <Tabs value={tab} onChange={(_, value: number) => setTab(value)} aria-label="Seções da estrutura organizacional"><Tab label="Gerências" /><Tab label="Discipulados" /></Tabs>
      {loading ? <Box sx={{ p: 3 }}><LoadingState label="Carregando estrutura..." /></Box> : tab === 0
        ? <GerenciaList items={gerencias} users={users} onEdit={(item) => setModal({ kind: 'gerencia', item })} />
        : <DiscipuladoList items={discipulados} users={users} gerencias={gerencias} onEdit={(item) => setModal({ kind: 'discipulado', item })} onDeactivate={setPendingDeactivate} />}
    </Paper>
    {modal?.kind === 'gerencia' && <GerenciaDialog item={modal.item} users={gerentes} saving={saving} onCreateUser={createUser} onClose={() => setModal(undefined)} onSave={(body) => void saveGerencia(body)} />}
    {modal?.kind === 'discipulado' && <DiscipuladoDialog item={modal.item} gerencias={gerencias.filter((item) => item.ativo !== false)} discipuladores={discipuladores} coLideres={coLideres} saving={saving} onCreateUser={createUser} onClose={() => { setPendingDiscipuladoId(undefined); setModal(undefined) }} onSave={(body, ids) => void saveDiscipulado(body, ids)} />}
    <Dialog open={Boolean(pendingDeactivate)} onClose={() => setPendingDeactivate(undefined)} fullWidth maxWidth="xs"><DialogTitle>Inativar discipulado?</DialogTitle><DialogContent><Typography color="text.secondary">O discipulado “{pendingDeactivate?.nome}” será inativado, mas seus dados históricos serão preservados.</Typography></DialogContent><DialogActions><Button onClick={() => setPendingDeactivate(undefined)}>Cancelar</Button><Button color="warning" variant="contained" disabled={saving} onClick={() => void deactivate()}>Inativar</Button></DialogActions></Dialog>
  </Stack>
}

function GerenciaList({ items, users, onEdit }: { items: Gerencia[]; users: Usuario[]; onEdit: (item: Gerencia) => void }) {
  return <List disablePadding>{items.length === 0 ? <EmptyState label="Nenhuma gerência cadastrada." /> : items.map((item) => <ListItem key={item.id} divider secondaryAction={<Button onClick={() => onEdit(item)}>Editar</Button>}><ListItemText primary={item.nome} secondary={`Gerente: ${userName(users, item.gerenteId)}`} /></ListItem>)}</List>
}

function DiscipuladoList({ items, users, gerencias, onEdit, onDeactivate }: { items: Discipulado[]; users: Usuario[]; gerencias: Gerencia[]; onEdit: (item: Discipulado) => void; onDeactivate: (item: Discipulado) => void }) {
  return <List disablePadding>{items.length === 0 ? <EmptyState label="Nenhum discipulado cadastrado." /> : items.map((item) => <ListItem key={item.id} divider alignItems="flex-start" secondaryAction={<Stack direction="row" spacing={1}><Button onClick={() => onEdit(item)}>Editar</Button>{item.ativo !== false && <Button color="warning" onClick={() => onDeactivate(item)}>Inativar</Button>}</Stack>}><ListItemText primary={<Stack direction="row" spacing={1} alignItems="center"><span>{item.nome}</span><Chip size="small" label={item.sexo === 'MASCULINO' ? 'Masculino' : 'Feminino'} color={item.ativo === false ? 'default' : 'primary'} /></Stack>} secondary={<><span>Gerência: {gerencias.find((gerencia) => gerencia.id === item.gerenciaId)?.nome ?? `#${item.gerenciaId}`}</span><br /><span>Discipulador: {userName(users, item.discipuladorId)}</span><br /><span>Co-líderes: {item.coLideres.length ? item.coLideres.map((user) => user.nome).join(', ') : 'Nenhum'}</span></>} /></ListItem>)}</List>
}

function EmptyState({ label }: { label: string }) { return <Box sx={{ p: 4 }}><Typography color="text.secondary">{label}</Typography></Box> }

function GerenciaDialog({ item, users, saving, onCreateUser, onClose, onSave }: { item?: Gerencia; users: Usuario[]; saving: boolean; onCreateUser: (body: CriarUsuarioRequest) => Promise<Usuario>; onClose: () => void; onSave: (body: GerenciaRequest) => void }) {
  const [nome, setNome] = useState(item?.nome ?? '')
  const [gerenteId, setGerenteId] = useState(String(item?.gerenteId ?? ''))
  const [creatingUser, setCreatingUser] = useState(false)
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); onSave({ nome, gerenteId: Number(gerenteId) }) }
  return <><Dialog open onClose={onClose} fullWidth maxWidth="sm" PaperProps={{ component: 'form', onSubmit: submit }}><DialogTitle>{item ? 'Editar gerência' : 'Nova gerência'}</DialogTitle><DialogContent><Stack spacing={2} sx={{ pt: 1 }}><TextField required autoFocus label="Nome" value={nome} onChange={(event) => setNome(event.target.value)} inputProps={{ maxLength: 120 }} /><UserSelect required label="Gerente" value={gerenteId} users={users} onChange={setGerenteId} /><Button type="button" onClick={() => setCreatingUser(true)}>Cadastrar novo gerente</Button></Stack></DialogContent><DialogActions><Button onClick={onClose}>Cancelar</Button><Button type="submit" variant="contained" disabled={saving}>{saving ? 'Salvando...' : 'Salvar'}</Button></DialogActions></Dialog>{creatingUser && <QuickUserDialog role="GERENTE" onClose={() => setCreatingUser(false)} onCreate={async (body) => { const user = await onCreateUser(body); setGerenteId(String(user.id)); setCreatingUser(false) }} />}</>
}

function DiscipuladoDialog({ item, gerencias, discipuladores, coLideres, saving, onCreateUser, onClose, onSave }: { item?: Discipulado; gerencias: Gerencia[]; discipuladores: Usuario[]; coLideres: Usuario[]; saving: boolean; onCreateUser: (body: CriarUsuarioRequest) => Promise<Usuario>; onClose: () => void; onSave: (body: DiscipuladoRequest, coLiderIds: number[]) => void }) {
  const [nome, setNome] = useState(item?.nome ?? '')
  const [sexo, setSexo] = useState(item?.sexo ?? 'MASCULINO')
  const [gerenciaId, setGerenciaId] = useState(String(item?.gerenciaId ?? ''))
  const [discipuladorId, setDiscipuladorId] = useState(String(item?.discipuladorId ?? ''))
  const [coLiderIds, setCoLiderIds] = useState<number[]>(item?.coLideres.map((user) => user.id) ?? [])
  const [creatingRole, setCreatingRole] = useState<'DISCIPULADOR' | 'CO_LIDER'>()
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); onSave({ nome, sexo, gerenciaId: Number(gerenciaId), discipuladorId: Number(discipuladorId), ativo: item?.ativo ?? true }, coLiderIds) }
  function toggleCoLider(id: number) { setCoLiderIds((ids) => ids.includes(id) ? ids.filter((value) => value !== id) : ids.length < 2 ? [...ids, id] : ids) }
  return <><Dialog open onClose={onClose} fullWidth maxWidth="sm" PaperProps={{ component: 'form', onSubmit: submit }}><DialogTitle>{item ? 'Editar discipulado' : 'Novo discipulado'}</DialogTitle><DialogContent><Stack spacing={2} sx={{ pt: 1 }}><TextField required autoFocus label="Nome" value={nome} onChange={(event) => setNome(event.target.value)} inputProps={{ maxLength: 120 }} /><FormControl required><InputLabel id="sexo-label">Sexo</InputLabel><Select labelId="sexo-label" label="Sexo" value={sexo} onChange={(event) => setSexo(event.target.value as DiscipuladoRequest['sexo'])}><MenuItem value="MASCULINO">Masculino</MenuItem><MenuItem value="FEMININO">Feminino</MenuItem></Select></FormControl><FormControl required><InputLabel id="gerencia-label">Gerência</InputLabel><Select labelId="gerencia-label" label="Gerência" value={gerenciaId} onChange={(event) => setGerenciaId(event.target.value)}>{gerencias.map((gerencia) => <MenuItem key={gerencia.id} value={String(gerencia.id)}>{gerencia.nome}</MenuItem>)}</Select></FormControl><UserSelect required label="Discipulador" value={discipuladorId} users={discipuladores} onChange={setDiscipuladorId} /><Button type="button" onClick={() => setCreatingRole('DISCIPULADOR')}>Cadastrar novo discipulador</Button><Box><Typography variant="subtitle2">Co-líderes (até 2)</Typography>{coLideres.length === 0 ? <Typography variant="body2" color="text.secondary">Não há usuários com o perfil de co-líder.</Typography> : coLideres.map((user) => <FormControlLabel key={user.id} control={<Checkbox checked={coLiderIds.includes(user.id)} disabled={user.id === Number(discipuladorId) || (!coLiderIds.includes(user.id) && coLiderIds.length >= 2)} onChange={() => toggleCoLider(user.id)} />} label={user.nome} />)}<Button type="button" disabled={coLiderIds.length >= 2} onClick={() => setCreatingRole('CO_LIDER')}>Cadastrar novo co-líder</Button></Box></Stack></DialogContent><DialogActions><Button onClick={onClose}>Cancelar</Button><Button type="submit" variant="contained" disabled={saving}>{saving ? 'Salvando...' : 'Salvar'}</Button></DialogActions></Dialog>{creatingRole && <QuickUserDialog role={creatingRole} onClose={() => setCreatingRole(undefined)} onCreate={async (body) => { const user = await onCreateUser(body); if (creatingRole === 'DISCIPULADOR') setDiscipuladorId(String(user.id)); else setCoLiderIds((ids) => [...ids, user.id]); setCreatingRole(undefined) }} />}</>
}

function QuickUserDialog({ role, onClose, onCreate }: { role: 'GERENTE' | 'DISCIPULADOR' | 'CO_LIDER'; onClose: () => void; onCreate: (body: CriarUsuarioRequest) => Promise<void> }) {
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [perfis, setPerfis] = useState<Perfil[]>([role])
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSaving(true); setError('')
    try { await onCreate({ nome, email, senha, perfis }) }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível cadastrar o usuário.') }
    finally { setSaving(false) }
  }
  return <Dialog open onClose={onClose} fullWidth maxWidth="sm" PaperProps={{ component: 'form', onSubmit: submit }}><DialogTitle>Cadastrar {roleLabel[role].toLowerCase()}</DialogTitle><DialogContent><Stack spacing={2} sx={{ pt: 1 }}>{error && <Alert severity="error">{error}</Alert>}<TextField required autoFocus label="Nome" value={nome} inputProps={{ maxLength: 120 }} onChange={(event) => setNome(event.target.value)} /><TextField required type="email" label="E-mail" value={email} onChange={(event) => setEmail(event.target.value)} /><TextField required type="password" label="Senha inicial" helperText="Use pelo menos 12 caracteres." value={senha} inputProps={{ minLength: 12 }} onChange={(event) => setSenha(event.target.value)} /><Box><Typography variant="subtitle2">Perfis adicionais</Typography>{(Object.keys(roleLabel) as Perfil[]).map((perfil) => <FormControlLabel key={perfil} control={<Checkbox checked={perfis.includes(perfil)} disabled={perfil === role} onChange={() => setPerfis((current) => current.includes(perfil) ? current.filter((item) => item !== perfil) : [...current, perfil])} />} label={roleLabel[perfil]} />)}</Box></Stack></DialogContent><DialogActions><Button onClick={onClose}>Cancelar</Button><Button type="submit" variant="contained" disabled={saving}>{saving ? 'Cadastrando...' : 'Cadastrar'}</Button></DialogActions></Dialog>
}

function UserSelect({ label, value, users, required, onChange }: { label: string; value: string; users: Usuario[]; required?: boolean; onChange: (value: string) => void }) {
  const labelId = `${label.toLowerCase().replaceAll(' ', '-')}-label`
  return <FormControl required={required}><InputLabel id={labelId}>{label}</InputLabel><Select labelId={labelId} label={label} value={value} onChange={(event) => onChange(event.target.value)}>{users.map((user) => <MenuItem key={user.id} value={String(user.id)}>{user.nome} · {user.perfis.map((perfil) => roleLabel[perfil]).join(', ')}</MenuItem>)}</Select></FormControl>
}
