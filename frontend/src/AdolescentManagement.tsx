import { Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, InputLabel, MenuItem, Paper, Select, Stack, Switch, TextField, Typography } from '@mui/material'
import { FormEvent, useCallback, useEffect, useState } from 'react'
import { Adolescente, AdolescenteInput, adolescentesApi, DiscipuladoResumo } from './adolescentesApi'

const vazio: AdolescenteInput = { nome: '', dataNascimento: '', telefone: '', instagram: '', discipuladoId: 0, ativo: true }

export default function AdolescentManagement() {
  const [items, setItems] = useState<Adolescente[]>([])
  const [discipulados, setDiscipulados] = useState<DiscipuladoResumo[]>([])
  const [filtro, setFiltro] = useState<number>(0)
  const [form, setForm] = useState<AdolescenteInput>(vazio)
  const [editando, setEditando] = useState<Adolescente | null>(null)
  const [transferindo, setTransferindo] = useState<Adolescente | null>(null)
  const [destino, setDestino] = useState(0)
  const [dataTransferencia, setDataTransferencia] = useState(new Date().toISOString().slice(0, 10))
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [salvando, setSalvando] = useState(false)

  const carregar = useCallback(async () => {
    try {
      setErro('')
      const [pagina, ds] = await Promise.all([adolescentesApi.listar(filtro || undefined), adolescentesApi.listarDiscipulados()])
      setItems(pagina.content); setDiscipulados(ds.content)
    } catch (e) { setErro(e instanceof Error ? e.message : 'Não foi possível carregar adolescentes.') }
  }, [filtro])
  useEffect(() => { void carregar() }, [carregar])

  function novo() { setEditando(null); setForm({ ...vazio, discipuladoId: filtro || discipulados[0]?.id || 0 }); setSucesso('') }
  function editar(a: Adolescente) { setEditando(a); setForm({ nome: a.nome, dataNascimento: a.dataNascimento, telefone: a.telefone ?? '', instagram: a.instagram ?? '', discipuladoId: a.discipuladoId, ativo: a.ativo }); setSucesso('') }
  async function salvar(event: FormEvent) {
    event.preventDefault(); setSalvando(true); setErro(''); setSucesso('')
    try {
      if (editando) await adolescentesApi.atualizar(editando.id, form); else await adolescentesApi.criar(form)
      setSucesso(editando ? 'Adolescente atualizado.' : 'Adolescente cadastrado.'); setEditando(null); setForm(vazio); await carregar()
    } catch (e) { setErro(e instanceof Error ? e.message : 'Não foi possível salvar.') } finally { setSalvando(false) }
  }
  async function inativar(a: Adolescente) {
    if (!window.confirm(`Inativar ${a.nome}? O histórico será preservado.`)) return
    try { await adolescentesApi.atualizar(a.id, { nome: a.nome, dataNascimento: a.dataNascimento, telefone: a.telefone, instagram: a.instagram, discipuladoId: a.discipuladoId, ativo: false }); setSucesso('Adolescente inativado.'); await carregar() }
    catch (e) { setErro(e instanceof Error ? e.message : 'Não foi possível inativar.') }
  }
  async function transferir() {
    if (!transferindo || !destino) return
    setSalvando(true); setErro('')
    try { await adolescentesApi.transferir(transferindo.id, destino, dataTransferencia); setTransferindo(null); setSucesso('Transferência concluída.'); await carregar() }
    catch (e) { setErro(e instanceof Error ? e.message : 'Não foi possível transferir.') } finally { setSalvando(false) }
  }

  return <Stack spacing={3}>
    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2}><Box><Typography variant="h5">Adolescentes</Typography><Typography color="text.secondary">Cadastro, inativação e transferência com histórico.</Typography></Box><Button variant="contained" onClick={novo}>Novo adolescente</Button></Stack>
    {erro && <Alert severity="error">{erro}</Alert>}{sucesso && <Alert severity="success">{sucesso}</Alert>}
    <FormControl size="small" sx={{ maxWidth: 360 }}><InputLabel>Discipulado</InputLabel><Select label="Discipulado" value={filtro} onChange={e => setFiltro(Number(e.target.value))}><MenuItem value={0}>Todos do meu escopo</MenuItem>{discipulados.map(d => <MenuItem key={d.id} value={d.id}>{d.nome}</MenuItem>)}</Select></FormControl>
    <Paper sx={{ overflowX: 'auto' }}><Box component="table" sx={{ width: '100%', borderCollapse: 'collapse', '& th, & td': { p: 2, textAlign: 'left', borderBottom: '1px solid', borderColor: 'divider' } }}><thead><tr><th>Nome</th><th>Nascimento</th><th>Contato</th><th>Situação</th><th>Ações</th></tr></thead><tbody>{items.map(a => <tr key={a.id}><td>{a.nome}</td><td>{new Date(`${a.dataNascimento}T12:00:00`).toLocaleDateString('pt-BR')}</td><td>{a.telefone || a.instagram || '—'}</td><td>{a.ativo ? 'Ativo' : 'Inativo'}</td><td><Stack direction="row" gap={1}><Button size="small" onClick={() => editar(a)}>Editar</Button><Button size="small" onClick={() => { setTransferindo(a); setDestino(0) }}>Transferir</Button>{a.ativo && <Button size="small" color="warning" onClick={() => void inativar(a)}>Inativar</Button>}</Stack></td></tr>)}</tbody></Box>{items.length === 0 && <Typography sx={{ p: 3 }} color="text.secondary">Nenhum adolescente encontrado.</Typography>}</Paper>
    {(form.nome !== '' || form.discipuladoId !== 0 || editando) && <Paper component="form" onSubmit={salvar} sx={{ p: 3 }}><Typography variant="h6" mb={2}>{editando ? 'Editar adolescente' : 'Novo adolescente'}</Typography><Stack spacing={2}><TextField required label="Nome" value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })}/><TextField required type="date" label="Data de nascimento" InputLabelProps={{ shrink: true }} value={form.dataNascimento} onChange={e => setForm({ ...form, dataNascimento: e.target.value })}/><Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}><TextField fullWidth label="Telefone" value={form.telefone} onChange={e => setForm({ ...form, telefone: e.target.value })}/><TextField fullWidth label="Instagram" value={form.instagram} onChange={e => setForm({ ...form, instagram: e.target.value })}/></Stack><FormControl required><InputLabel>Discipulado</InputLabel><Select disabled={Boolean(editando)} label="Discipulado" value={form.discipuladoId} onChange={e => setForm({ ...form, discipuladoId: Number(e.target.value) })}>{discipulados.map(d => <MenuItem key={d.id} value={d.id}>{d.nome}</MenuItem>)}</Select></FormControl>{editando && <Stack direction="row" alignItems="center"><Switch checked={form.ativo} onChange={e => setForm({ ...form, ativo: e.target.checked })}/><Typography>Ativo</Typography></Stack>}<Stack direction="row" gap={1}><Button type="submit" variant="contained" disabled={salvando || !form.discipuladoId}>{salvando ? 'Salvando...' : 'Salvar'}</Button><Button onClick={() => { setEditando(null); setForm(vazio) }}>Cancelar</Button></Stack></Stack></Paper>}
    <Dialog open={Boolean(transferindo)} onClose={() => setTransferindo(null)} fullWidth maxWidth="sm"><DialogTitle>Transferir {transferindo?.nome}</DialogTitle><DialogContent><Stack spacing={2} mt={1}><FormControl required><InputLabel>Novo discipulado</InputLabel><Select label="Novo discipulado" value={destino} onChange={e => setDestino(Number(e.target.value))}>{discipulados.filter(d => d.id !== transferindo?.discipuladoId).map(d => <MenuItem key={d.id} value={d.id}>{d.nome}</MenuItem>)}</Select></FormControl><TextField required type="date" label="Data de início" InputLabelProps={{ shrink: true }} value={dataTransferencia} onChange={e => setDataTransferencia(e.target.value)}/></Stack></DialogContent><DialogActions><Button onClick={() => setTransferindo(null)}>Cancelar</Button><Button variant="contained" disabled={!destino || salvando} onClick={() => void transferir()}>Transferir</Button></DialogActions></Dialog>
  </Stack>
}
