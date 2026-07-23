import { AddRounded, EditRounded, SwapHorizRounded } from '@mui/icons-material'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useCallback, useEffect, useState } from 'react'

import { AdolescenteFormFields } from '@/features/adolescentes/AdolescenteFormFields'
import type { Adolescente, AdolescenteInput, DiscipuladoResumo } from '@/features/adolescentes/api'
import { adolescentesApi } from '@/features/adolescentes/api'
import { DataTableCard, EmptyState, FilterToolbar, FormSheet, PageHeader, StatusChip } from '@/shared/ui'

const vazio: AdolescenteInput = {
  nome: '',
  dataNascimento: '',
  telefone: '',
  instagram: '',
  discipuladoId: 0,
  ativo: true,
}

export default function AdolescentManagement() {
  const [items, setItems] = useState<Adolescente[]>([])
  const [discipulados, setDiscipulados] = useState<DiscipuladoResumo[]>([])
  const [filtro, setFiltro] = useState<number>(0)
  const [form, setForm] = useState<AdolescenteInput>(vazio)
  const [editando, setEditando] = useState<Adolescente | null>(null)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [transferindo, setTransferindo] = useState<Adolescente | null>(null)
  const [inativando, setInativando] = useState<Adolescente | null>(null)
  const [destino, setDestino] = useState(0)
  const [dataTransferencia, setDataTransferencia] = useState(new Date().toISOString().slice(0, 10))
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [salvando, setSalvando] = useState(false)

  const carregar = useCallback(async () => {
    try {
      setErro('')
      const [pagina, ds] = await Promise.all([
        adolescentesApi.listar(filtro || undefined),
        adolescentesApi.listarDiscipulados(),
      ])
      setItems(pagina.content)
      setDiscipulados(ds.content)
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível carregar adolescentes.')
    }
  }, [filtro])
  useEffect(() => {
    void carregar()
  }, [carregar])

  function novo() {
    setEditando(null)
    setForm({ ...vazio, discipuladoId: filtro || discipulados[0]?.id || 0 })
    setSucesso('')
    setDrawerOpen(true)
  }
  function editar(a: Adolescente) {
    setEditando(a)
    setForm({
      nome: a.nome,
      dataNascimento: a.dataNascimento,
      telefone: a.telefone ?? '',
      instagram: a.instagram ?? '',
      discipuladoId: a.discipuladoId,
      ativo: a.ativo,
    })
    setSucesso('')
    setDrawerOpen(true)
  }
  function fecharDrawer() {
    if (!salvando) {
      setDrawerOpen(false)
      setEditando(null)
      setForm(vazio)
    }
  }
  async function salvar(event: FormEvent) {
    event.preventDefault()
    setSalvando(true)
    setErro('')
    setSucesso('')
    try {
      if (editando) await adolescentesApi.atualizar(editando.id, form)
      else await adolescentesApi.criar(form)
      setSucesso(editando ? 'Adolescente atualizado.' : 'Adolescente cadastrado.')
      setEditando(null)
      setForm(vazio)
      setDrawerOpen(false)
      await carregar()
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível salvar.')
    } finally {
      setSalvando(false)
    }
  }
  async function inativar() {
    if (!inativando) return
    const a = inativando
    setSalvando(true)
    setErro('')
    try {
      await adolescentesApi.atualizar(a.id, {
        nome: a.nome,
        dataNascimento: a.dataNascimento,
        telefone: a.telefone,
        instagram: a.instagram,
        discipuladoId: a.discipuladoId,
        ativo: false,
      })
      setInativando(null)
      setSucesso('Adolescente inativado.')
      await carregar()
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível inativar.')
    } finally {
      setSalvando(false)
    }
  }
  async function transferir() {
    if (!transferindo || !destino) return
    setSalvando(true)
    setErro('')
    try {
      await adolescentesApi.transferir(transferindo.id, destino, dataTransferencia)
      setTransferindo(null)
      setSucesso('Transferência concluída.')
      await carregar()
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível transferir.')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <Stack spacing={3}>
      <PageHeader
        title="Adolescentes"
        description="Cadastro, inativação e transferência com histórico."
        eyebrow="Gestão"
        action={
          <Button variant="contained" startIcon={<AddRounded />} onClick={novo}>
            Novo adolescente
          </Button>
        }
      />
      {erro && (
        <Alert severity="error" onClose={() => setErro('')}>
          {erro}
        </Alert>
      )}
      {sucesso && (
        <Alert severity="success" onClose={() => setSucesso('')}>
          {sucesso}
        </Alert>
      )}
      <FilterToolbar>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          justifyContent="space-between"
          alignItems={{ sm: 'center' }}
          gap={2}
        >
          <FormControl sx={{ minWidth: { xs: '100%', sm: 320 } }}>
            <InputLabel>Discipulado</InputLabel>
            <Select label="Discipulado" value={filtro} onChange={(e) => setFiltro(Number(e.target.value))}>
              <MenuItem value={0}>Todos do meu escopo</MenuItem>
              {discipulados.map((d) => (
                <MenuItem key={d.id} value={d.id}>
                  {d.nome}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Typography variant="body2" color="text.secondary">
            {items.length} resultado{items.length === 1 ? '' : 's'}
          </Typography>
        </Stack>
      </FilterToolbar>
      <DataTableCard>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Nome</TableCell>
              <TableCell>Nascimento</TableCell>
              <TableCell>Contato</TableCell>
              <TableCell>Situação</TableCell>
              <TableCell align="right">Ações</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {items.map((a) => (
              <TableRow key={a.id} hover>
                <TableCell>
                  <Typography variant="body2" fontWeight={650}>
                    {a.nome}
                  </Typography>
                </TableCell>
                <TableCell>{new Date(`${a.dataNascimento}T12:00:00`).toLocaleDateString('pt-BR')}</TableCell>
                <TableCell>{a.telefone || a.instagram || '—'}</TableCell>
                <TableCell>
                  <StatusChip active={a.ativo} />
                </TableCell>
                <TableCell align="right">
                  <Stack direction="row" justifyContent="flex-end" gap={0.5}>
                    <Button size="small" startIcon={<EditRounded />} onClick={() => editar(a)}>
                      Editar
                    </Button>
                    <Button
                      size="small"
                      startIcon={<SwapHorizRounded />}
                      onClick={() => {
                        setTransferindo(a)
                        setDestino(0)
                      }}
                    >
                      Transferir
                    </Button>
                    {a.ativo && (
                      <Button size="small" color="warning" onClick={() => setInativando(a)}>
                        Inativar
                      </Button>
                    )}
                  </Stack>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        {items.length === 0 && (
          <EmptyState
            title="Nenhum adolescente encontrado"
            description="Ajuste o filtro ou cadastre um novo adolescente."
          />
        )}
      </DataTableCard>
      <FormSheet
        open={drawerOpen}
        onClose={fecharDrawer}
        title={editando ? 'Editar adolescente' : 'Novo adolescente'}
        width={520}
        component="form"
        onSubmit={salvar}
        actions={
          <>
            <Button onClick={fecharDrawer}>Cancelar</Button>
            <Button type="submit" variant="contained" disabled={salvando || !form.discipuladoId}>
              {salvando ? 'Salvando...' : 'Salvar'}
            </Button>
          </>
        }
      >
        {erro && <Alert severity="error">{erro}</Alert>}
        <AdolescenteFormFields value={form} onChange={(patch) => setForm({ ...form, ...patch })} disabled={salvando} />
        <FormControl required>
          <InputLabel>Discipulado</InputLabel>
          <Select
            disabled={Boolean(editando)}
            label="Discipulado"
            value={form.discipuladoId}
            onChange={(e) => setForm({ ...form, discipuladoId: Number(e.target.value) })}
          >
            {discipulados.map((d) => (
              <MenuItem key={d.id} value={d.id}>
                {d.nome}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        {editando && (
          <Stack direction="row" alignItems="center">
            <Switch checked={form.ativo} onChange={(e) => setForm({ ...form, ativo: e.target.checked })} />
            <Typography>Cadastro ativo</Typography>
          </Stack>
        )}
      </FormSheet>
      <Dialog open={Boolean(transferindo)} onClose={() => setTransferindo(null)} fullWidth maxWidth="sm">
        <DialogTitle>Transferir {transferindo?.nome}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <Typography color="text.secondary">O histórico anterior será preservado.</Typography>
            <FormControl required>
              <InputLabel>Novo discipulado</InputLabel>
              <Select label="Novo discipulado" value={destino} onChange={(e) => setDestino(Number(e.target.value))}>
                {discipulados
                  .filter((d) => d.id !== transferindo?.discipuladoId)
                  .map((d) => (
                    <MenuItem key={d.id} value={d.id}>
                      {d.nome}
                    </MenuItem>
                  ))}
              </Select>
            </FormControl>
            <TextField
              required
              type="date"
              label="Data de início"
              InputLabelProps={{ shrink: true }}
              value={dataTransferencia}
              onChange={(e) => setDataTransferencia(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTransferindo(null)}>Cancelar</Button>
          <Button variant="contained" disabled={!destino || salvando} onClick={() => void transferir()}>
            Transferir
          </Button>
        </DialogActions>
      </Dialog>
      <Dialog open={Boolean(inativando)} onClose={() => setInativando(null)} fullWidth maxWidth="xs">
        <DialogTitle>Inativar adolescente?</DialogTitle>
        <DialogContent>
          <Typography color="text.secondary">
            O cadastro de {inativando?.nome} será inativado, mas todo o histórico permanecerá disponível.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setInativando(null)}>Cancelar</Button>
          <Button color="warning" variant="contained" disabled={salvando} onClick={() => void inativar()}>
            Inativar
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
