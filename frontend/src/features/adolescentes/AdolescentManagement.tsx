import { AddRounded, DeleteForeverRounded, EditRounded, SwapHorizRounded } from '@mui/icons-material'
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
import type { FormEvent, ReactNode } from 'react'
import { useCallback, useEffect, useMemo, useState } from 'react'

import { AdolescenteFormFields } from '@/features/adolescentes/AdolescenteFormFields'
import type { Adolescente, AdolescenteInput, AlertaGoe, DiscipuladoResumo } from '@/features/adolescentes/api'
import { adolescentesApi } from '@/features/adolescentes/api'
import { DataTableCard, EmptyState, FilterToolbar, FormSheet, PageHeader, SectionCard, StatusChip } from '@/shared/ui'
import { contatoMinimoValido, mensagemTelefoneInvalido, telefoneValido } from '@/shared/validation/telefone'

const hoje = () => new Date().toISOString().slice(0, 10)

function idadeAnos(dataNascimento: string): number {
  const nasc = new Date(`${dataNascimento}T12:00:00`)
  const agora = new Date()
  let idade = agora.getFullYear() - nasc.getFullYear()
  const m = agora.getMonth() - nasc.getMonth()
  if (m < 0 || (m === 0 && agora.getDate() < nasc.getDate())) idade -= 1
  return idade
}

function aniversario(dataNascimento: string): string {
  const d = new Date(`${dataNascimento}T12:00:00`)
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
}

function inputFromAdolescente(a: Adolescente): AdolescenteInput {
  return {
    nome: a.nome,
    dataNascimento: a.dataNascimento,
    telefone: a.telefone ?? '',
    instagram: a.instagram ?? '',
    responsavelNome: a.responsavelNome ?? '',
    responsavelTelefone: a.responsavelTelefone ?? '',
    consentimentoEm: a.consentimentoEm ?? hoje(),
    categoria: a.categoria,
    nomeMae: a.nomeMae ?? '',
    telefoneMae: a.telefoneMae ?? '',
    nomePai: a.nomePai ?? '',
    telefonePai: a.telefonePai ?? '',
    estrutura: a.estrutura ?? '',
    motivoAfastamento: a.motivoAfastamento ?? '',
    discipuladoId: a.discipuladoId,
    ativo: a.ativo,
  }
}

const vazio: AdolescenteInput = {
  nome: '',
  dataNascimento: '',
  telefone: '',
  instagram: '',
  responsavelNome: '',
  responsavelTelefone: '',
  consentimentoEm: '',
  categoria: 'DISCIPULO',
  nomeMae: '',
  telefoneMae: '',
  nomePai: '',
  telefonePai: '',
  estrutura: '',
  motivoAfastamento: '',
  discipuladoId: 0,
  ativo: true,
}

function validarFormularioAdolescente(form: AdolescenteInput): string | null {
  const telefones: Array<[string | undefined, string]> = [
    [form.telefone, 'telefone do adolescente'],
    [form.telefoneMae, 'telefone da mãe'],
    [form.telefonePai, 'telefone do pai'],
    [form.responsavelTelefone, 'telefone do responsável'],
  ]
  for (const [valor, rotulo] of telefones) {
    if (!telefoneValido(valor)) return mensagemTelefoneInvalido(rotulo)
  }
  if (!contatoMinimoValido(form)) {
    return 'Informe nome e telefone da mãe, ou do pai, ou do responsável.'
  }
  return null
}

export default function AdolescentManagement({
  podeAnonimizar = false,
  podeEditar = true,
}: {
  podeAnonimizar?: boolean
  podeEditar?: boolean
}) {
  const [items, setItems] = useState<Adolescente[]>([])
  const [discipulados, setDiscipulados] = useState<DiscipuladoResumo[]>([])
  const [filtro, setFiltro] = useState<number>(0)
  const [form, setForm] = useState<AdolescenteInput>(vazio)
  const [editando, setEditando] = useState<Adolescente | null>(null)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [transferindo, setTransferindo] = useState<Adolescente | null>(null)
  const [inativando, setInativando] = useState<Adolescente | null>(null)
  const [anonimizando, setAnonimizando] = useState<Adolescente | null>(null)
  const [destino, setDestino] = useState(0)
  const [dataTransferencia, setDataTransferencia] = useState(hoje())
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [alertasGoe, setAlertasGoe] = useState<AlertaGoe[]>([])
  const [alertaAtual, setAlertaAtual] = useState<AlertaGoe | null>(null)
  const [confirmarGoe, setConfirmarGoe] = useState(false)
  const [motivoGoe, setMotivoGoe] = useState('')
  const [ignoradosGoe, setIgnoradosGoe] = useState<number[]>([])

  const carregarDiscipulados = useCallback(async () => {
    try {
      const ds = await adolescentesApi.listarDiscipulados()
      setDiscipulados(ds.content)
      setFiltro((atual) => {
        if (atual && ds.content.some((d) => d.id === atual)) return atual
        if (ds.content.length === 1) return ds.content[0].id
        return atual
      })
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível carregar discipulados.')
    }
  }, [])

  const carregar = useCallback(async () => {
    try {
      setErro('')
      const pagina = await adolescentesApi.listar(filtro || undefined)
      setItems(
        pagina.content.map((a) => ({
          ...a,
          categoria: a.categoria ?? 'DISCIPULO',
        })),
      )
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível carregar adolescentes.')
    }
  }, [filtro])

  const carregarAlertas = useCallback(async (discipuladoId: number) => {
    if (!discipuladoId) {
      setAlertasGoe([])
      setAlertaAtual(null)
      return
    }
    try {
      const alertas = await adolescentesApi.alertasGoe(discipuladoId)
      setAlertasGoe(alertas)
    } catch {
      setAlertasGoe([])
    }
  }, [])

  useEffect(() => {
    void carregarDiscipulados()
  }, [carregarDiscipulados])

  useEffect(() => {
    void carregar()
  }, [carregar])

  useEffect(() => {
    void carregarAlertas(filtro)
  }, [filtro, carregarAlertas])

  useEffect(() => {
    const pendente = alertasGoe.find((a) => !ignoradosGoe.includes(a.adolescenteId))
    setAlertaAtual(pendente ?? null)
    setConfirmarGoe(false)
    setMotivoGoe('')
  }, [alertasGoe, ignoradosGoe])

  const discipulos = useMemo(() => items.filter((a) => (a.categoria ?? 'DISCIPULO') === 'DISCIPULO'), [items])
  const visitantes = useMemo(() => items.filter((a) => a.categoria === 'VISITANTE'), [items])
  const goe = useMemo(() => items.filter((a) => a.categoria === 'DISCIPULO_GOE'), [items])
  const discipuladoSelecionado = filtro || discipulados[0]?.id || 0
  const podeCadastrar = podeEditar && discipuladoSelecionado > 0

  function novo() {
    const discipuladoId = discipuladoSelecionado
    if (!discipuladoId) {
      setErro('Selecione um discipulado para cadastrar.')
      return
    }
    setEditando(null)
    setForm({
      ...vazio,
      consentimentoEm: hoje(),
      discipuladoId,
      categoria: 'DISCIPULO',
    })
    setSucesso('')
    setDrawerOpen(true)
  }

  function editar(a: Adolescente) {
    setEditando(a)
    setForm(inputFromAdolescente(a))
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
    if (form.categoria === 'DISCIPULO_GOE' && !form.motivoAfastamento?.trim()) {
      setErro('Informe o motivo do afastamento para Discípulo GOE.')
      return
    }
    const erroValidacao = validarFormularioAdolescente(form)
    if (erroValidacao) {
      setErro(erroValidacao)
      return
    }
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
      await carregarAlertas(filtro)
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
      await adolescentesApi.atualizar(a.id, { ...inputFromAdolescente(a), ativo: false })
      setInativando(null)
      setSucesso('Adolescente inativado.')
      await carregar()
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível inativar.')
    } finally {
      setSalvando(false)
    }
  }

  async function anonimizar() {
    if (!anonimizando) return
    const a = anonimizando
    setSalvando(true)
    setErro('')
    try {
      await adolescentesApi.anonimizar(a.id)
      setAnonimizando(null)
      setSucesso('Dados pessoais anonimizados. O histórico de frequência foi preservado.')
      await carregar()
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível anonimizar.')
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

  function recusarGoe() {
    if (!alertaAtual) return
    setIgnoradosGoe((prev) => [...prev, alertaAtual.adolescenteId])
  }

  async function confirmarAtualizacaoGoe() {
    if (!alertaAtual || !motivoGoe.trim()) {
      setErro('Informe o motivo do afastamento.')
      return
    }
    const alvo = items.find((a) => a.id === alertaAtual.adolescenteId)
    if (!alvo) {
      setIgnoradosGoe((prev) => [...prev, alertaAtual.adolescenteId])
      return
    }
    setSalvando(true)
    setErro('')
    try {
      await adolescentesApi.atualizar(alvo.id, {
        ...inputFromAdolescente(alvo),
        categoria: 'DISCIPULO_GOE',
        motivoAfastamento: motivoGoe.trim(),
      })
      setSucesso(`Status de ${alvo.nome} atualizado para Discípulo GOE.`)
      setIgnoradosGoe((prev) => [...prev, alertaAtual.adolescenteId])
      setConfirmarGoe(false)
      setMotivoGoe('')
      await carregar()
      await carregarAlertas(filtro)
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível atualizar o status.')
    } finally {
      setSalvando(false)
    }
  }

  function acoesLinha(a: Adolescente) {
    if (!podeEditar && !podeAnonimizar) return null
    return (
      <Stack direction="row" justifyContent="flex-end" gap={0.5} flexWrap="wrap">
        {podeEditar && (
          <>
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
          </>
        )}
        {podeAnonimizar && !a.anonimizado && (
          <Button size="small" color="error" startIcon={<DeleteForeverRounded />} onClick={() => setAnonimizando(a)}>
            Excluir dados
          </Button>
        )}
      </Stack>
    )
  }

  return (
    <Stack spacing={3}>
      <PageHeader
        title="Gestão de Discípulos"
        description="Liste discípulos, visitantes e discípulos GOE do discipulado selecionado."
        eyebrow="Gestão"
        action={
          podeEditar ? (
            <Button variant="contained" startIcon={<AddRounded />} onClick={novo} disabled={!podeCadastrar}>
              Cadastrar discípulo
            </Button>
          ) : undefined
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
          <FormControl sx={{ minWidth: { xs: '100%', sm: 320 } }} required={discipulados.length > 1}>
            <InputLabel>Discipulado</InputLabel>
            <Select
              label="Discipulado"
              value={filtro}
              onChange={(e) => {
                setFiltro(Number(e.target.value))
                setIgnoradosGoe([])
              }}
            >
              {discipulados.length > 1 && <MenuItem value={0}>Selecione um discipulado</MenuItem>}
              {discipulados.map((d) => (
                <MenuItem key={d.id} value={d.id}>
                  {d.nome}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Typography variant="body2" color="text.secondary">
            {items.length} adolescente{items.length === 1 ? '' : 's'}
          </Typography>
        </Stack>
      </FilterToolbar>

      {!discipuladoSelecionado && discipulados.length > 1 ? (
        <EmptyState
          title="Selecione um discipulado"
          description="Gerentes e administradores visualizam o cadastro por discipulado."
        />
      ) : (
        <>
          <CategoriaSection
            titulo="Discípulos ativos"
            items={discipulos}
            empty="Nenhum discípulo ativo neste discipulado."
            acoes={acoesLinha}
            onEditar={podeEditar ? editar : undefined}
          />
          <CategoriaSection
            titulo="Visitantes"
            items={visitantes}
            empty="Nenhum visitante cadastrado neste discipulado."
            acoes={acoesLinha}
            onEditar={podeEditar ? editar : undefined}
          />
          <CategoriaSection
            titulo="Discípulos GOE"
            items={goe}
            empty="Nenhum discípulo GOE neste discipulado."
            acoes={acoesLinha}
            mostrarMotivo
            onEditar={podeEditar ? editar : undefined}
          />
        </>
      )}

      <FormSheet
        open={drawerOpen}
        onClose={fecharDrawer}
        title={editando ? 'Editar adolescente' : 'Cadastrar discípulo'}
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

      <Dialog open={Boolean(anonimizando)} onClose={() => setAnonimizando(null)} fullWidth maxWidth="xs">
        <DialogTitle>Excluir dados pessoais?</DialogTitle>
        <DialogContent>
          <Typography color="text.secondary">
            Os dados pessoais de {anonimizando?.nome} (nome, contato e responsável) serão removidos de forma permanente
            para atender a um pedido de exclusão. O histórico de frequência é preservado de forma anonimizada. Esta ação
            não pode ser desfeita.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAnonimizando(null)}>Cancelar</Button>
          <Button color="error" variant="contained" disabled={salvando} onClick={() => void anonimizar()}>
            Excluir dados
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(alertaAtual) && !confirmarGoe} onClose={recusarGoe} fullWidth maxWidth="sm">
        <DialogTitle>Potencial discípulo GOE</DialogTitle>
        <DialogContent>
          <Typography>
            {podeEditar
              ? `${alertaAtual?.nome} é potencial discípulo GOE (pelo menos ${alertaAtual?.faltas} faltas nas últimas 6 semanas). Deseja atualizar o status dele(a)?`
              : `${alertaAtual?.nome} é potencial discípulo GOE (pelo menos ${alertaAtual?.faltas} faltas nas últimas 6 semanas). A liderança do discipulado pode atualizar o status.`}
          </Typography>
        </DialogContent>
        <DialogActions>
          {podeEditar ? (
            <>
              <Button onClick={recusarGoe}>Não</Button>
              <Button variant="contained" onClick={() => setConfirmarGoe(true)}>
                Sim
              </Button>
            </>
          ) : (
            <Button variant="contained" onClick={recusarGoe}>
              Entendi
            </Button>
          )}
        </DialogActions>
      </Dialog>

      <Dialog
        open={Boolean(alertaAtual) && confirmarGoe}
        onClose={() => setConfirmarGoe(false)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>Atualizar para Discípulo GOE</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <Typography color="text.secondary">Informe o motivo do afastamento de {alertaAtual?.nome}.</Typography>
            <TextField
              required
              autoFocus
              label="Motivo do afastamento"
              value={motivoGoe}
              multiline
              minRows={2}
              onChange={(e) => setMotivoGoe(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmarGoe(false)}>Cancelar</Button>
          <Button
            variant="contained"
            disabled={salvando || !motivoGoe.trim()}
            onClick={() => void confirmarAtualizacaoGoe()}
          >
            Atualizar status
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}

function CategoriaSection({
  titulo,
  items,
  empty,
  acoes,
  mostrarMotivo = false,
  onEditar,
}: {
  titulo: string
  items: Adolescente[]
  empty: string
  acoes: (a: Adolescente) => ReactNode
  mostrarMotivo?: boolean
  onEditar?: (a: Adolescente) => void
}) {
  return (
    <SectionCard>
      <Stack spacing={2}>
        <Typography variant="h6" fontWeight={700}>
          {titulo}
        </Typography>
        <DataTableCard>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Nome</TableCell>
                <TableCell>Idade</TableCell>
                <TableCell>Aniv.</TableCell>
                <TableCell>Tel.</TableCell>
                <TableCell>Tel. mãe</TableCell>
                <TableCell>Tel. pai</TableCell>
                <TableCell>Estrutura</TableCell>
                {mostrarMotivo && <TableCell>Motivo do afastamento</TableCell>}
                <TableCell>Situação</TableCell>
                <TableCell align="right">Ações</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((a) => (
                <TableRow key={a.id} hover>
                  <TableCell>
                    {onEditar ? (
                      <Button
                        variant="text"
                        size="small"
                        onClick={() => onEditar(a)}
                        sx={{
                          px: 0.5,
                          minWidth: 0,
                          justifyContent: 'flex-start',
                          fontWeight: 650,
                          textTransform: 'none',
                        }}
                      >
                        {a.nome}
                      </Button>
                    ) : (
                      <Typography variant="body2" fontWeight={650}>
                        {a.nome}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>{idadeAnos(a.dataNascimento)}a</TableCell>
                  <TableCell>{aniversario(a.dataNascimento)}</TableCell>
                  <TableCell>{a.telefone || '—'}</TableCell>
                  <TableCell>{a.telefoneMae || '—'}</TableCell>
                  <TableCell>{a.telefonePai || '—'}</TableCell>
                  <TableCell>{a.estrutura || '—'}</TableCell>
                  {mostrarMotivo && <TableCell>{a.motivoAfastamento || '—'}</TableCell>}
                  <TableCell>
                    <StatusChip active={a.ativo} />
                  </TableCell>
                  <TableCell align="right">{acoes(a)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {items.length === 0 && <EmptyState title={empty} description="" />}
        </DataTableCard>
      </Stack>
    </SectionCard>
  )
}
