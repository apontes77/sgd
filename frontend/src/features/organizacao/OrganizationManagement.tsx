import { AddRounded } from '@mui/icons-material'
import {
  Alert,
  Autocomplete,
  Box,
  Button,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  FormControlLabel,
  InputLabel,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  MenuItem,
  Paper,
  Select,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useCallback, useEffect, useMemo, useState } from 'react'

import { pathForSection } from '@/app/appNavigation'
import { organizationApi } from '@/features/organizacao/api'
import { LoadingState, PageHeader, RowActionsMenu } from '@/shared/ui'

import type {
  CriarUsuarioRequest,
  Discipulado,
  DiscipuladoRequest,
  FaixaEtaria,
  Gerencia,
  GerenciaRequest,
  Perfil,
  SexoOrganizacional,
  Usuario,
} from './api'

type Modal =
  { kind: 'gerencia'; item?: Gerencia } | { kind: 'discipulado'; item?: Discipulado; formacao?: boolean } | undefined

type ExclusaoGerencia = { tipo: 'aviso' | 'confirmar'; item: Gerencia }

const AVISO_EXCLUSAO_GERENCIA =
  'Os discipulados associados a esta gerência precisam ser realocados ou desativados antes de excluir a gerência.'

const roleLabel: Record<Perfil, string> = {
  ADMIN: 'Administrador',
  GERENTE: 'Gerente',
  DISCIPULADOR: 'Discipulador',
  CO_LIDER: 'Co-líder',
}

const faixaEtariaLabel: Record<FaixaEtaria, string> = {
  DE_09_A_11: '09 a 11',
  DE_11_A_13: '11 a 13',
  DE_13_A_15: '13 a 15',
  DE_15_MAIS: '15+',
}

const faixasEtarias: FaixaEtaria[] = ['DE_09_A_11', 'DE_11_A_13', 'DE_13_A_15', 'DE_15_MAIS']

function userName(users: Usuario[], id: number) {
  return users.find((user) => user.id === id)?.nome ?? `Usuário #${id}`
}

function formatFaixas(faixas: FaixaEtaria[]) {
  return faixas.map((faixa) => faixaEtariaLabel[faixa]).join(', ')
}

function normalizarBusca(valor: string) {
  return valor.trim().toLocaleLowerCase('pt-BR')
}

function nomeContem(valor: string | undefined, termoBruto: string) {
  const termo = normalizarBusca(termoBruto)
  if (!termo) return true
  return normalizarBusca(valor ?? '').includes(termo)
}

export default function OrganizationManagement({
  onAbrirAdolescentes,
}: {
  onAbrirAdolescentes?: (discipuladoId: number) => void
}) {
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
  const [exclusaoGerencia, setExclusaoGerencia] = useState<ExclusaoGerencia>()
  const [filtroGerenciaSexo, setFiltroGerenciaSexo] = useState<SexoOrganizacional | ''>('')
  const [filtroGerenciaFaixa, setFiltroGerenciaFaixa] = useState<FaixaEtaria | ''>('')
  const [filtroGerenciaBusca, setFiltroGerenciaBusca] = useState('')
  const [filtroDiscipuladoSexo, setFiltroDiscipuladoSexo] = useState<SexoOrganizacional | ''>('')
  const [filtroDiscipuladoFaixa, setFiltroDiscipuladoFaixa] = useState<FaixaEtaria | ''>('')
  const [filtroDiscipuladoBusca, setFiltroDiscipuladoBusca] = useState('')
  const [gerenciaSelecionadaId, setGerenciaSelecionadaId] = useState<number>()

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [todosUsuarios, gerenciaPage, discipuladoPage] = await Promise.all([
        organizationApi.listarTodosUsuarios(),
        organizationApi.listarGerencias(),
        organizationApi.listarDiscipulados(),
      ])
      setUsers(todosUsuarios)
      setGerencias(gerenciaPage.content)
      setDiscipulados(discipuladoPage.content)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível carregar a estrutura organizacional.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const gerenciasFiltradas = useMemo(
    () =>
      gerencias.filter((item) => {
        if (filtroGerenciaSexo && item.sexo !== filtroGerenciaSexo) return false
        if (filtroGerenciaFaixa && !item.faixasEtarias.includes(filtroGerenciaFaixa)) return false
        return (
          nomeContem(item.nome, filtroGerenciaBusca) || nomeContem(userName(users, item.gerenteId), filtroGerenciaBusca)
        )
      }),
    [filtroGerenciaBusca, filtroGerenciaFaixa, filtroGerenciaSexo, gerencias, users],
  )

  const gerenciaSelecionada = useMemo(
    () => gerenciasFiltradas.find((item) => item.id === gerenciaSelecionadaId),
    [gerenciaSelecionadaId, gerenciasFiltradas],
  )

  const discipuladosDaGerencia = useMemo(
    () =>
      gerenciaSelecionada
        ? discipulados.filter((item) => item.gerenciaId === gerenciaSelecionada.id && !item.emFormacao)
        : [],
    [discipulados, gerenciaSelecionada],
  )

  const discipuladosFiltrados = useMemo(
    () =>
      discipulados.filter((item) => {
        if (item.emFormacao) return false
        if (filtroDiscipuladoSexo && item.sexo !== filtroDiscipuladoSexo) return false
        if (filtroDiscipuladoFaixa && item.faixaEtaria !== filtroDiscipuladoFaixa) return false
        const gerenciaNome = gerencias.find((gerencia) => gerencia.id === item.gerenciaId)?.nome
        const discipuladorNome = item.discipuladorNome ?? userName(users, item.discipuladorId)
        return (
          nomeContem(item.nome, filtroDiscipuladoBusca) ||
          nomeContem(discipuladorNome, filtroDiscipuladoBusca) ||
          nomeContem(gerenciaNome, filtroDiscipuladoBusca)
        )
      }),
    [discipulados, filtroDiscipuladoBusca, filtroDiscipuladoFaixa, filtroDiscipuladoSexo, gerencias, users],
  )

  const discipuladosFormacaoFiltrados = useMemo(
    () =>
      discipulados.filter((item) => {
        if (!item.emFormacao) return false
        const discipuladorNome = item.discipuladorNome ?? userName(users, item.discipuladorId)
        return nomeContem(item.nome, filtroDiscipuladoBusca) || nomeContem(discipuladorNome, filtroDiscipuladoBusca)
      }),
    [discipulados, filtroDiscipuladoBusca, users],
  )

  useEffect(() => {
    if (gerenciaSelecionadaId != null && !gerenciaSelecionada) {
      setGerenciaSelecionadaId(undefined)
    }
  }, [gerenciaSelecionada, gerenciaSelecionadaId])

  const gerentes = useMemo(
    () =>
      users.filter(
        (user) => user.ativo !== false && (user.perfis.includes('GERENTE') || user.perfis.includes('ADMIN')),
      ),
    [users],
  )
  const discipuladores = useMemo(
    () =>
      users.filter(
        (user) => user.ativo !== false && (user.perfis.includes('DISCIPULADOR') || user.perfis.includes('ADMIN')),
      ),
    [users],
  )
  const coLideres = useMemo(
    () => users.filter((user) => user.ativo !== false && user.perfis.includes('CO_LIDER')),
    [users],
  )

  async function saveGerencia(body: GerenciaRequest) {
    setSaving(true)
    setError('')
    try {
      if (modal?.item) await organizationApi.atualizarGerencia(modal.item.id, body)
      else await organizationApi.criarGerencia(body)
      setModal(undefined)
      await load()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível salvar a gerência.')
    } finally {
      setSaving(false)
    }
  }

  async function createUser(body: CriarUsuarioRequest) {
    const created = await organizationApi.criarUsuario(body)
    setUsers((current) => [...current.filter((user) => user.id !== created.id), created])
    return created
  }

  async function saveDiscipulado(body: DiscipuladoRequest, coLiderIds: number[]) {
    setSaving(true)
    setError('')
    try {
      const existingId = modal?.kind === 'discipulado' ? (modal.item?.id ?? pendingDiscipuladoId) : pendingDiscipuladoId
      const saved = existingId
        ? await organizationApi.atualizarDiscipulado(existingId, body)
        : await organizationApi.criarDiscipulado(body)
      setPendingDiscipuladoId(saved.id)
      if (!body.emFormacao) await organizationApi.definirCoLideres(saved.id, coLiderIds)
      setPendingDiscipuladoId(undefined)
      setModal(undefined)
      await load()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível salvar o discipulado.')
    } finally {
      setSaving(false)
    }
  }

  function solicitarExclusaoGerencia(item: Gerencia) {
    const possuiDiscipulados = discipulados.some((discipulado) => discipulado.gerenciaId === item.id)
    setExclusaoGerencia({ tipo: possuiDiscipulados ? 'aviso' : 'confirmar', item })
  }

  async function excluirGerencia() {
    if (exclusaoGerencia?.tipo !== 'confirmar') return
    const item = exclusaoGerencia.item
    setSaving(true)
    setError('')
    try {
      await organizationApi.excluirGerencia(item.id)
      setExclusaoGerencia(undefined)
      if (gerenciaSelecionadaId === item.id) setGerenciaSelecionadaId(undefined)
      await load()
    } catch (reason) {
      setExclusaoGerencia(undefined)
      setError(reason instanceof Error ? reason.message : 'Não foi possível excluir a gerência.')
    } finally {
      setSaving(false)
    }
  }

  async function deactivate() {
    if (!pendingDeactivate) return
    const item = pendingDeactivate
    setSaving(true)
    setError('')
    try {
      await organizationApi.atualizarDiscipulado(item.id, {
        nome: item.nome,
        sexo: item.sexo,
        faixaEtaria: item.faixaEtaria,
        gerenciaId: item.emFormacao ? null : item.gerenciaId,
        discipuladorId: item.discipuladorId,
        ativo: false,
        emFormacao: item.emFormacao,
      })
      setPendingDeactivate(undefined)
      await load()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível inativar o discipulado.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Stack spacing={3}>
      <PageHeader
        title="Estrutura organizacional"
        description="Gerencie gerências, discipulados e suas lideranças."
        eyebrow="Gestão"
        action={
          <Button
            variant="contained"
            startIcon={<AddRounded />}
            onClick={() => {
              setPendingDiscipuladoId(undefined)
              setModal(tab === 0 ? { kind: 'gerencia' } : { kind: 'discipulado', formacao: tab === 2 })
            }}
          >
            {tab === 0 ? 'Nova gerência' : tab === 2 ? 'Novo discipulado de formação' : 'Novo discipulado'}
          </Button>
        }
      />
      {error && (
        <Alert severity="error" onClose={() => setError('')} sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
        <Tabs
          value={tab}
          onChange={(_, value: number) => setTab(value)}
          aria-label="Seções da estrutura organizacional"
        >
          <Tab label="Gerências" />
          <Tab label="Discipulados" />
          <Tab label="Discipulados de formação" />
        </Tabs>
        {loading ? (
          <Box sx={{ p: 3 }}>
            <LoadingState label="Carregando estrutura..." />
          </Box>
        ) : tab === 0 ? (
          <Stack>
            <GerenciaList
              items={gerenciasFiltradas}
              users={users}
              selectedId={gerenciaSelecionada?.id}
              filtroSexo={filtroGerenciaSexo}
              filtroFaixa={filtroGerenciaFaixa}
              filtroBusca={filtroGerenciaBusca}
              onFiltroSexo={setFiltroGerenciaSexo}
              onFiltroFaixa={setFiltroGerenciaFaixa}
              onFiltroBusca={setFiltroGerenciaBusca}
              onSelect={(item) => setGerenciaSelecionadaId((atual) => (atual === item.id ? undefined : item.id))}
              onEdit={(item) => setModal({ kind: 'gerencia', item })}
              onDelete={solicitarExclusaoGerencia}
            />
            <Divider />
            <Box sx={{ minHeight: 220, bgcolor: 'action.hover' }}>
              {gerenciaSelecionada ? (
                <>
                  <Box sx={{ px: 2, pt: 2, pb: 1 }}>
                    <Typography variant="subtitle1" fontWeight={600}>
                      Discipulados de {gerenciaSelecionada.nome}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Gerente: {userName(users, gerenciaSelecionada.gerenteId)}
                    </Typography>
                  </Box>
                  <DiscipuladoList
                    items={discipuladosDaGerencia}
                    users={users}
                    gerencias={gerencias}
                    emptyLabel="Nenhum discipulado nesta gerência."
                    onAbrirAdolescentes={onAbrirAdolescentes}
                    onEdit={(item) => setModal({ kind: 'discipulado', item })}
                    onDeactivate={setPendingDeactivate}
                  />
                </>
              ) : (
                <EmptyState label="Selecione uma gerência para ver os discipulados sob sua responsabilidade." />
              )}
            </Box>
          </Stack>
        ) : tab === 1 ? (
          <Box>
            <OrganizacaoFiltros
              idPrefix="discipulado"
              filtroSexo={filtroDiscipuladoSexo}
              filtroFaixa={filtroDiscipuladoFaixa}
              filtroBusca={filtroDiscipuladoBusca}
              onFiltroSexo={setFiltroDiscipuladoSexo}
              onFiltroFaixa={setFiltroDiscipuladoFaixa}
              onFiltroBusca={setFiltroDiscipuladoBusca}
            />
            <DiscipuladoList
              items={discipuladosFiltrados}
              users={users}
              gerencias={gerencias}
              emptyLabel="Nenhum discipulado encontrado."
              onAbrirAdolescentes={onAbrirAdolescentes}
              onEdit={(item) => setModal({ kind: 'discipulado', item })}
              onDeactivate={setPendingDeactivate}
            />
          </Box>
        ) : (
          <Box>
            <OrganizacaoFiltros
              idPrefix="formacao"
              filtroSexo={filtroDiscipuladoSexo}
              filtroFaixa={filtroDiscipuladoFaixa}
              filtroBusca={filtroDiscipuladoBusca}
              onFiltroSexo={setFiltroDiscipuladoSexo}
              onFiltroFaixa={setFiltroDiscipuladoFaixa}
              onFiltroBusca={setFiltroDiscipuladoBusca}
              somenteBusca
            />
            <DiscipuladoList
              items={discipuladosFormacaoFiltrados}
              users={users}
              gerencias={gerencias}
              formacao
              emptyLabel="Nenhum discipulado de formação encontrado."
              onAbrirAdolescentes={onAbrirAdolescentes}
              onEdit={(item) => setModal({ kind: 'discipulado', item, formacao: true })}
              onDeactivate={setPendingDeactivate}
            />
          </Box>
        )}
      </Paper>
      {modal?.kind === 'gerencia' && (
        <GerenciaDialog
          item={modal.item}
          users={gerentes}
          saving={saving}
          onCreateUser={createUser}
          onClose={() => setModal(undefined)}
          onSave={(body) => void saveGerencia(body)}
        />
      )}
      {modal?.kind === 'discipulado' && (
        <DiscipuladoDialog
          item={modal.item}
          formacao={Boolean(modal.formacao || modal.item?.emFormacao)}
          gerencias={gerencias.filter((item) => item.ativo !== false)}
          discipuladores={discipuladores}
          coLideres={coLideres}
          saving={saving}
          onCreateUser={createUser}
          onClose={() => {
            setPendingDiscipuladoId(undefined)
            setModal(undefined)
          }}
          onSave={(body, ids) => void saveDiscipulado(body, ids)}
        />
      )}
      <Dialog open={Boolean(pendingDeactivate)} onClose={() => setPendingDeactivate(undefined)} fullWidth maxWidth="xs">
        <DialogTitle>Inativar discipulado?</DialogTitle>
        <DialogContent>
          <Typography color="text.secondary">
            O discipulado “{pendingDeactivate?.nome}” será inativado, mas seus dados históricos serão preservados.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPendingDeactivate(undefined)}>Cancelar</Button>
          <Button color="warning" variant="contained" disabled={saving} onClick={() => void deactivate()}>
            Inativar
          </Button>
        </DialogActions>
      </Dialog>
      <Dialog
        open={exclusaoGerencia?.tipo === 'aviso'}
        onClose={() => setExclusaoGerencia(undefined)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>Não é possível excluir a gerência</DialogTitle>
        <DialogContent>
          <Alert severity="warning">{AVISO_EXCLUSAO_GERENCIA}</Alert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setExclusaoGerencia(undefined)}>Entendi</Button>
        </DialogActions>
      </Dialog>
      <Dialog
        open={exclusaoGerencia?.tipo === 'confirmar'}
        onClose={() => setExclusaoGerencia(undefined)}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle>Excluir gerência?</DialogTitle>
        <DialogContent>
          <Typography color="text.secondary">
            A gerência “{exclusaoGerencia?.item.nome}” será excluída. Esta ação não poderá ser desfeita.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setExclusaoGerencia(undefined)}>Cancelar</Button>
          <Button color="error" variant="contained" disabled={saving} onClick={() => void excluirGerencia()}>
            Excluir
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}

function OrganizacaoFiltros({
  idPrefix,
  filtroSexo,
  filtroFaixa,
  filtroBusca,
  onFiltroSexo,
  onFiltroFaixa,
  onFiltroBusca,
  somenteBusca = false,
}: {
  idPrefix: string
  filtroSexo: SexoOrganizacional | ''
  filtroFaixa: FaixaEtaria | ''
  filtroBusca: string
  onFiltroSexo: (value: SexoOrganizacional | '') => void
  onFiltroFaixa: (value: FaixaEtaria | '') => void
  onFiltroBusca: (value: string) => void
  somenteBusca?: boolean
}) {
  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ p: 2 }}>
      <TextField
        size="small"
        label="Busca"
        placeholder="Nome ou parte do nome"
        value={filtroBusca}
        onChange={(event) => onFiltroBusca(event.target.value)}
        sx={{ minWidth: { xs: '100%', sm: 240 }, flex: { sm: 1 } }}
      />
      {!somenteBusca && (
        <>
          <FormControl size="small" sx={{ minWidth: 160 }}>
            <InputLabel id={`${idPrefix}-filtro-sexo-label`}>Sexo</InputLabel>
            <Select
              labelId={`${idPrefix}-filtro-sexo-label`}
              label="Sexo"
              value={filtroSexo}
              onChange={(event) => onFiltroSexo(event.target.value as SexoOrganizacional | '')}
            >
              <MenuItem value="">Todos</MenuItem>
              <MenuItem value="MASCULINO">Masculino</MenuItem>
              <MenuItem value="FEMININO">Feminino</MenuItem>
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel id={`${idPrefix}-filtro-faixa-label`}>Faixa etária</InputLabel>
            <Select
              labelId={`${idPrefix}-filtro-faixa-label`}
              label="Faixa etária"
              value={filtroFaixa}
              onChange={(event) => onFiltroFaixa(event.target.value as FaixaEtaria | '')}
            >
              <MenuItem value="">Todas</MenuItem>
              {faixasEtarias.map((faixa) => (
                <MenuItem key={faixa} value={faixa}>
                  {faixaEtariaLabel[faixa]}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </>
      )}
    </Stack>
  )
}

function GerenciaList({
  items,
  users,
  selectedId,
  filtroSexo,
  filtroFaixa,
  filtroBusca,
  onFiltroSexo,
  onFiltroFaixa,
  onFiltroBusca,
  onSelect,
  onEdit,
  onDelete,
}: {
  items: Gerencia[]
  users: Usuario[]
  selectedId?: number
  filtroSexo: SexoOrganizacional | ''
  filtroFaixa: FaixaEtaria | ''
  filtroBusca: string
  onFiltroSexo: (value: SexoOrganizacional | '') => void
  onFiltroFaixa: (value: FaixaEtaria | '') => void
  onFiltroBusca: (value: string) => void
  onSelect: (item: Gerencia) => void
  onEdit: (item: Gerencia) => void
  onDelete: (item: Gerencia) => void
}) {
  return (
    <Box>
      <OrganizacaoFiltros
        idPrefix="gerencia"
        filtroSexo={filtroSexo}
        filtroFaixa={filtroFaixa}
        filtroBusca={filtroBusca}
        onFiltroSexo={onFiltroSexo}
        onFiltroFaixa={onFiltroFaixa}
        onFiltroBusca={onFiltroBusca}
      />
      <List disablePadding>
        {items.length === 0 ? (
          <EmptyState label="Nenhuma gerência encontrada." />
        ) : (
          items.map((item) => (
            <ListItem
              key={item.id}
              disablePadding
              divider
              secondaryAction={
                <RowActionsMenu
                  ariaLabel={`Ações de ${item.nome}`}
                  actions={[
                    {
                      label: 'Editar',
                      onClick: () => onEdit(item),
                    },
                    {
                      label: 'Excluir',
                      onClick: () => onDelete(item),
                      color: 'error',
                    },
                  ]}
                />
              }
            >
              <ListItemButton selected={selectedId === item.id} onClick={() => onSelect(item)}>
                <ListItemText
                  primary={
                    <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                      <span>{item.nome}</span>
                      <Chip size="small" label={item.sexo === 'MASCULINO' ? 'Masculino' : 'Feminino'} />
                    </Stack>
                  }
                  secondary={`Gerente: ${userName(users, item.gerenteId)} · Faixas: ${formatFaixas(item.faixasEtarias)}`}
                  primaryTypographyProps={{ component: 'div', sx: { minWidth: 0 } }}
                  secondaryTypographyProps={{ sx: { minWidth: 0, pr: 1 } }}
                  sx={{ pr: 1, minWidth: 0 }}
                />
              </ListItemButton>
            </ListItem>
          ))
        )}
      </List>
    </Box>
  )
}

function DiscipuladoList({
  items,
  users,
  gerencias,
  formacao = false,
  emptyLabel = 'Nenhum discipulado cadastrado.',
  onAbrirAdolescentes,
  onEdit,
  onDeactivate,
}: {
  items: Discipulado[]
  users: Usuario[]
  gerencias: Gerencia[]
  formacao?: boolean
  emptyLabel?: string
  onAbrirAdolescentes?: (discipuladoId: number) => void
  onEdit: (item: Discipulado) => void
  onDeactivate: (item: Discipulado) => void
}) {
  return (
    <List disablePadding>
      {items.length === 0 ? (
        <EmptyState label={emptyLabel} />
      ) : (
        items.map((item) => {
          const href = pathForSection('adolescentes', { discipuladoId: item.id })
          const conteudo = (
            <ListItemText
              primary={
                <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                  <span>{item.nome}</span>
                  {!formacao && (
                    <>
                      <Chip
                        size="small"
                        label={item.sexo === 'MASCULINO' ? 'Masculino' : 'Feminino'}
                        color={item.ativo === false ? 'default' : 'primary'}
                      />
                      <Chip
                        size="small"
                        variant="outlined"
                        label={faixaEtariaLabel[item.faixaEtaria] ?? item.faixaEtaria}
                        color={item.ativo === false ? 'default' : 'primary'}
                      />
                    </>
                  )}
                </Stack>
              }
              secondary={
                <>
                  {!formacao && (
                    <>
                      <span>
                        Gerência:{' '}
                        {gerencias.find((gerencia) => gerencia.id === item.gerenciaId)?.nome ?? `#${item.gerenciaId}`}
                      </span>
                      <br />
                    </>
                  )}
                  <span>Discipulador: {userName(users, item.discipuladorId)}</span>
                  {!formacao && (
                    <>
                      <br />
                      <span>
                        Co-líderes:{' '}
                        {item.coLideres.length ? item.coLideres.map((user) => user.nome).join(', ') : 'Nenhum'}
                      </span>
                    </>
                  )}
                </>
              }
              primaryTypographyProps={{ component: 'div', sx: { minWidth: 0 } }}
              secondaryTypographyProps={{ component: 'div', sx: { minWidth: 0, pr: 1 } }}
              sx={{ pr: 1, minWidth: 0 }}
            />
          )
          return (
            <ListItem
              key={item.id}
              disablePadding={Boolean(onAbrirAdolescentes)}
              divider
              alignItems="flex-start"
              secondaryAction={
                <RowActionsMenu
                  ariaLabel={`Ações de ${item.nome}`}
                  actions={[
                    { label: 'Editar', onClick: () => onEdit(item) },
                    ...(item.ativo !== false
                      ? [{ label: 'Inativar', onClick: () => onDeactivate(item), color: 'warning' as const }]
                      : []),
                  ]}
                />
              }
            >
              {onAbrirAdolescentes ? (
                <ListItemButton
                  component="a"
                  href={href}
                  aria-label={`Ver adolescentes de ${item.nome}`}
                  onClick={(event) => {
                    if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || event.button !== 0) return
                    event.preventDefault()
                    onAbrirAdolescentes(item.id)
                  }}
                >
                  {conteudo}
                </ListItemButton>
              ) : (
                conteudo
              )}
            </ListItem>
          )
        })
      )}
    </List>
  )
}

function EmptyState({ label }: { label: string }) {
  return (
    <Box sx={{ p: 4 }}>
      <Typography color="text.secondary">{label}</Typography>
    </Box>
  )
}

function GerenciaDialog({
  item,
  users,
  saving,
  onCreateUser,
  onClose,
  onSave,
}: {
  item?: Gerencia
  users: Usuario[]
  saving: boolean
  onCreateUser: (body: CriarUsuarioRequest) => Promise<Usuario>
  onClose: () => void
  onSave: (body: GerenciaRequest) => void
}) {
  const [nome, setNome] = useState(item?.nome ?? '')
  const [sexo, setSexo] = useState<SexoOrganizacional>(item?.sexo ?? 'MASCULINO')
  const [faixasSelecionadas, setFaixasSelecionadas] = useState<FaixaEtaria[]>(item?.faixasEtarias ?? ['DE_15_MAIS'])
  const [gerenteId, setGerenteId] = useState(String(item?.gerenteId ?? ''))
  const [creatingUser, setCreatingUser] = useState(false)
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (faixasSelecionadas.length === 0) return
    onSave({ nome, sexo, faixasEtarias: faixasSelecionadas, gerenteId: Number(gerenteId) })
  }
  function toggleFaixa(faixa: FaixaEtaria) {
    setFaixasSelecionadas((atuais) =>
      atuais.includes(faixa) ? atuais.filter((item) => item !== faixa) : [...atuais, faixa],
    )
  }
  return (
    <>
      <Dialog open onClose={onClose} fullWidth maxWidth="sm" PaperProps={{ component: 'form', onSubmit: submit }}>
        <DialogTitle>{item ? 'Editar gerência' : 'Nova gerência'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            <TextField
              required
              autoFocus
              label="Nome"
              value={nome}
              onChange={(event) => setNome(event.target.value)}
              inputProps={{ maxLength: 120 }}
            />
            <FormControl required>
              <InputLabel id="gerencia-sexo-label">Sexo</InputLabel>
              <Select
                labelId="gerencia-sexo-label"
                label="Sexo"
                value={sexo}
                onChange={(event) => setSexo(event.target.value as SexoOrganizacional)}
              >
                <MenuItem value="MASCULINO">Masculino</MenuItem>
                <MenuItem value="FEMININO">Feminino</MenuItem>
              </Select>
            </FormControl>
            <Box>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>
                Faixas etárias *
              </Typography>
              {faixasEtarias.map((faixa) => (
                <FormControlLabel
                  key={faixa}
                  control={
                    <Checkbox checked={faixasSelecionadas.includes(faixa)} onChange={() => toggleFaixa(faixa)} />
                  }
                  label={faixaEtariaLabel[faixa]}
                />
              ))}
              {faixasSelecionadas.length === 0 && (
                <Typography variant="caption" color="error">
                  Selecione ao menos uma faixa etária.
                </Typography>
              )}
            </Box>
            <UserSelect required label="Gerente" value={gerenteId} users={users} onChange={setGerenteId} />
            <Button type="button" onClick={() => setCreatingUser(true)}>
              Cadastrar novo gerente
            </Button>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancelar</Button>
          <Button type="submit" variant="contained" disabled={saving || faixasSelecionadas.length === 0}>
            {saving ? 'Salvando...' : 'Salvar'}
          </Button>
        </DialogActions>
      </Dialog>
      {creatingUser && (
        <QuickUserDialog
          perfilInicial="GERENTE"
          onClose={() => setCreatingUser(false)}
          onCreate={async (body) => {
            const user = await onCreateUser(body)
            setGerenteId(String(user.id))
            setCreatingUser(false)
          }}
        />
      )}
    </>
  )
}

function DiscipuladoDialog({
  item,
  formacao = false,
  gerencias,
  discipuladores,
  coLideres,
  saving,
  onCreateUser,
  onClose,
  onSave,
}: {
  item?: Discipulado
  formacao?: boolean
  gerencias: Gerencia[]
  discipuladores: Usuario[]
  coLideres: Usuario[]
  saving: boolean
  onCreateUser: (body: CriarUsuarioRequest) => Promise<Usuario>
  onClose: () => void
  onSave: (body: DiscipuladoRequest, coLiderIds: number[]) => void
}) {
  const [nome, setNome] = useState(item?.nome ?? '')
  const [sexo, setSexo] = useState(item?.sexo ?? 'MASCULINO')
  const [faixaEtaria, setFaixaEtaria] = useState<FaixaEtaria>(item?.faixaEtaria ?? 'DE_15_MAIS')
  const [gerenciaId, setGerenciaId] = useState(String(item?.gerenciaId ?? ''))
  const [discipuladorId, setDiscipuladorId] = useState(String(item?.discipuladorId ?? ''))
  const [coLiderId, setCoLiderId] = useState(String(item?.coLideres?.[0]?.id ?? ''))
  const [coLiderTreinamentoId, setCoLiderTreinamentoId] = useState(String(item?.coLideres?.[1]?.id ?? ''))
  const [creatingRole, setCreatingRole] = useState<'DISCIPULADOR' | 'CO_LIDER'>()
  const candidatosCoLider = useMemo(() => {
    const byId = new Map<number, Usuario>()
    for (const user of coLideres) byId.set(user.id, user)
    for (const user of item?.coLideres ?? []) {
      if (!byId.has(user.id)) byId.set(user.id, user)
    }
    return [...byId.values()]
  }, [coLideres, item?.coLideres])
  const opcoesCoLider = candidatosCoLider.filter(
    (user) => user.id !== Number(discipuladorId) && String(user.id) !== coLiderTreinamentoId,
  )
  const opcoesTreinamento = candidatosCoLider.filter(
    (user) => user.id !== Number(discipuladorId) && String(user.id) !== coLiderId,
  )
  function idsCoLideres() {
    return [coLiderId, coLiderTreinamentoId].filter((id) => id !== '').map(Number)
  }
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onSave(
      {
        nome,
        sexo,
        faixaEtaria: formacao ? 'DE_15_MAIS' : faixaEtaria,
        gerenciaId: formacao ? null : Number(gerenciaId),
        discipuladorId: Number(discipuladorId),
        ativo: item?.ativo ?? true,
        emFormacao: formacao,
      },
      formacao ? [] : idsCoLideres(),
    )
  }
  function preencherProximoSlotCoLider(userId: number) {
    if (!coLiderId) {
      setCoLiderId(String(userId))
      return
    }
    if (!coLiderTreinamentoId) setCoLiderTreinamentoId(String(userId))
  }
  return (
    <>
      <Dialog open onClose={onClose} fullWidth maxWidth="sm" PaperProps={{ component: 'form', onSubmit: submit }}>
        <DialogTitle>
          {item
            ? formacao
              ? 'Editar discipulado de formação'
              : 'Editar discipulado'
            : formacao
              ? 'Novo discipulado de formação'
              : 'Novo discipulado'}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            <TextField
              required
              autoFocus
              label="Nome"
              value={nome}
              onChange={(event) => setNome(event.target.value)}
              inputProps={{ maxLength: 120 }}
            />
            <FormControl required>
              <InputLabel id="sexo-label">Sexo</InputLabel>
              <Select
                labelId="sexo-label"
                label="Sexo"
                value={sexo}
                onChange={(event) => setSexo(event.target.value as DiscipuladoRequest['sexo'])}
              >
                <MenuItem value="MASCULINO">Masculino</MenuItem>
                <MenuItem value="FEMININO">Feminino</MenuItem>
              </Select>
            </FormControl>
            {!formacao && (
              <FormControl required>
                <InputLabel id="discipulado-faixa-label">Faixa etária</InputLabel>
                <Select
                  labelId="discipulado-faixa-label"
                  label="Faixa etária"
                  value={faixaEtaria}
                  onChange={(event) => setFaixaEtaria(event.target.value as FaixaEtaria)}
                >
                  {faixasEtarias.map((faixa) => (
                    <MenuItem key={faixa} value={faixa}>
                      {faixaEtariaLabel[faixa]}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}
            {!formacao && (
              <FormControl required>
                <InputLabel id="gerencia-label">Gerência</InputLabel>
                <Select
                  labelId="gerencia-label"
                  label="Gerência"
                  value={gerenciaId}
                  onChange={(event) => setGerenciaId(event.target.value)}
                >
                  {gerencias.map((gerencia) => (
                    <MenuItem key={gerencia.id} value={String(gerencia.id)}>
                      {gerencia.nome}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}
            <UserSelect
              required
              label="Discipulador"
              value={discipuladorId}
              users={discipuladores}
              onChange={setDiscipuladorId}
            />
            <Button type="button" onClick={() => setCreatingRole('DISCIPULADOR')}>
              Cadastrar novo discipulador
            </Button>
            {!formacao && (
              <Box>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>
                  Co-líderes (até 2)
                </Typography>
                {candidatosCoLider.length === 0 ? (
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    Não há usuários com o perfil de co-líder.
                  </Typography>
                ) : (
                  <Stack spacing={2}>
                    <UserSelect
                      label="Co-líder"
                      value={coLiderId}
                      users={opcoesCoLider}
                      fullWidth
                      onChange={(value) => {
                        setCoLiderId(value)
                        if (!value) setCoLiderTreinamentoId('')
                      }}
                    />
                    <UserSelect
                      label="Co-líder em treinamento"
                      value={coLiderTreinamentoId}
                      users={opcoesTreinamento}
                      disabled={!coLiderId}
                      fullWidth
                      onChange={setCoLiderTreinamentoId}
                    />
                  </Stack>
                )}
                <Button
                  type="button"
                  disabled={idsCoLideres().length >= 2}
                  onClick={() => setCreatingRole('CO_LIDER')}
                  sx={{ mt: 1 }}
                >
                  Cadastrar novo co-líder
                </Button>
              </Box>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancelar</Button>
          <Button type="submit" variant="contained" disabled={saving}>
            {saving ? 'Salvando...' : 'Salvar'}
          </Button>
        </DialogActions>
      </Dialog>
      {creatingRole && (
        <QuickUserDialog
          perfilInicial={creatingRole}
          onClose={() => setCreatingRole(undefined)}
          onCreate={async (body) => {
            const user = await onCreateUser(body)
            if (creatingRole === 'DISCIPULADOR') setDiscipuladorId(String(user.id))
            else preencherProximoSlotCoLider(user.id)
            setCreatingRole(undefined)
          }}
        />
      )}
    </>
  )
}

function QuickUserDialog({
  perfilInicial,
  onClose,
  onCreate,
}: {
  perfilInicial: 'GERENTE' | 'DISCIPULADOR' | 'CO_LIDER'
  onClose: () => void
  onCreate: (body: CriarUsuarioRequest) => Promise<void>
}) {
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [perfis, setPerfis] = useState<Perfil[]>([perfilInicial])
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      await onCreate({ nome, email, senha, perfis })
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível cadastrar o usuário.')
    } finally {
      setSaving(false)
    }
  }
  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm" PaperProps={{ component: 'form', onSubmit: submit }}>
      <DialogTitle>Cadastrar {roleLabel[perfilInicial].toLowerCase()}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField
            required
            autoFocus
            label="Nome"
            value={nome}
            inputProps={{ maxLength: 120 }}
            onChange={(event) => setNome(event.target.value)}
          />
          <TextField
            required
            type="email"
            label="E-mail"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
          <TextField
            required
            type="password"
            label="Senha inicial"
            helperText="Use pelo menos 12 caracteres."
            value={senha}
            inputProps={{ minLength: 12 }}
            onChange={(event) => setSenha(event.target.value)}
          />
          <Box>
            <Typography variant="subtitle2">Perfis adicionais</Typography>
            {(Object.keys(roleLabel) as Perfil[]).map((perfil) => (
              <FormControlLabel
                key={perfil}
                control={
                  <Checkbox
                    checked={perfis.includes(perfil)}
                    disabled={perfil === perfilInicial}
                    onChange={() =>
                      setPerfis((current) =>
                        current.includes(perfil) ? current.filter((item) => item !== perfil) : [...current, perfil],
                      )
                    }
                  />
                }
                label={roleLabel[perfil]}
              />
            ))}
          </Box>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button type="submit" variant="contained" disabled={saving}>
          {saving ? 'Cadastrando...' : 'Cadastrar'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

function UserSelect({
  label,
  value,
  users,
  required,
  disabled,
  fullWidth,
  onChange,
}: {
  label: string
  value: string
  users: Usuario[]
  required?: boolean
  disabled?: boolean
  fullWidth?: boolean
  onChange: (value: string) => void
}) {
  const selecionado = users.find((user) => String(user.id) === value) ?? null
  const rotulo = (user: Usuario) => `${user.nome} · ${user.perfis.map((perfil) => roleLabel[perfil]).join(', ')}`
  return (
    <Autocomplete
      disabled={disabled}
      fullWidth={fullWidth}
      options={users}
      value={selecionado}
      onChange={(_, user) => onChange(user ? String(user.id) : '')}
      getOptionLabel={rotulo}
      isOptionEqualToValue={(option, current) => option.id === current.id}
      filterOptions={(options, state) => {
        const termo = normalizarBusca(state.inputValue)
        if (!termo || (selecionado && normalizarBusca(rotulo(selecionado)) === termo)) return options
        return options.filter((user) => nomeContem(user.nome, termo))
      }}
      noOptionsText="Nenhum usuário encontrado"
      renderInput={(params) => (
        <TextField {...params} required={required} label={label} placeholder="Pesquisar por nome" />
      )}
    />
  )
}
