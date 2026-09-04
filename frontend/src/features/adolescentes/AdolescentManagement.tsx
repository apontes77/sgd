import { AddRounded, SearchRounded } from '@mui/icons-material'
import {
  Alert,
  Autocomplete,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  InputAdornment,
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
import {
  familiaApi,
  type FamiliaInput,
  familiaNaoConsta,
  toFamiliaPayload,
  validarFamiliaObrigatoria,
} from '@/features/familia/api'
import { FamilyFormFields } from '@/features/familia/FamilyFormFields'
import { labelDiscipulado } from '@/shared/api/types'
import {
  DataTableCard,
  DiscipuladoLiderancaInfo,
  EmptyState,
  FilterToolbar,
  FormSheet,
  PageHeader,
  RowActionsMenu,
  SectionCard,
  StatusChip,
} from '@/shared/ui'
import { mensagemTelefoneInvalido, telefoneValido, validarTelefoneAdolescente } from '@/shared/validation/telefone'

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
  const semTelefone = a.categoria === 'DISCIPULO_GOE' && !a.telefone?.trim()
  return {
    nome: a.nome,
    dataNascimento: a.dataNascimento,
    telefone: a.telefone ?? '',
    naoPossuiTelefone: semTelefone,
    instagram: a.instagram ?? '',
    consentimentoEm: a.consentimentoEm ?? hoje(),
    categoria: a.categoria,
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
  naoPossuiTelefone: false,
  instagram: '',
  consentimentoEm: '',
  categoria: 'DISCIPULO',
  estrutura: '',
  motivoAfastamento: '',
  discipuladoId: 0,
  ativo: true,
}

function validarFormularioAdolescente(form: AdolescenteInput): string | null {
  if (!form.naoPossuiTelefone && !telefoneValido(form.telefone)) {
    return mensagemTelefoneInvalido('telefone do adolescente')
  }
  return validarTelefoneAdolescente(form.categoria, form)
}

export default function AdolescentManagement({
  discipuladoInicial,
  podeAnonimizar = false,
  podeEditar = true,
  podeFamilia = false,
}: {
  discipuladoInicial?: number
  podeAnonimizar?: boolean
  podeEditar?: boolean
  podeFamilia?: boolean
}) {
  const [items, setItems] = useState<Adolescente[]>([])
  const [totalAtivos, setTotalAtivos] = useState(0)
  const [discipulados, setDiscipulados] = useState<DiscipuladoResumo[]>([])
  const [filtro, setFiltro] = useState<number>(discipuladoInicial && discipuladoInicial > 0 ? discipuladoInicial : 0)
  const [form, setForm] = useState<AdolescenteInput>(vazio)
  const [familiaForm, setFamiliaForm] = useState<FamiliaInput>(familiaNaoConsta())
  const [editando, setEditando] = useState<Adolescente | null>(null)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [familiaAlvo, setFamiliaAlvo] = useState<Adolescente | null>(null)
  const [familiaDialog, setFamiliaDialog] = useState<FamiliaInput>(familiaNaoConsta())
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
  const [telefoneGoe, setTelefoneGoe] = useState('')
  const [naoPossuiTelefoneGoe, setNaoPossuiTelefoneGoe] = useState(false)
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
      const pagina = await adolescentesApi.listar(filtro || undefined, true)
      setTotalAtivos(pagina.totalElements)
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

  const carregarAlertas = useCallback(async (discipuladoId: number, emFormacao?: boolean) => {
    if (!discipuladoId || emFormacao) {
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
    if (!filtro) {
      setAlertasGoe([])
      setAlertaAtual(null)
      return
    }
    if (discipulados.length === 0) return
    const emFormacao = discipulados.find((d) => d.id === filtro)?.emFormacao
    void carregarAlertas(filtro, emFormacao)
  }, [filtro, discipulados, carregarAlertas])

  useEffect(() => {
    const pendente = alertasGoe.find((a) => !ignoradosGoe.includes(a.adolescenteId))
    setAlertaAtual(pendente ?? null)
    setConfirmarGoe(false)
    setMotivoGoe('')
    setTelefoneGoe('')
  }, [alertasGoe, ignoradosGoe])

  const discipulos = useMemo(() => items.filter((a) => (a.categoria ?? 'DISCIPULO') === 'DISCIPULO'), [items])
  const visitantes = useMemo(() => items.filter((a) => a.categoria === 'VISITANTE'), [items])
  const goe = useMemo(() => items.filter((a) => a.categoria === 'DISCIPULO_GOE'), [items])
  const discipuladoAtual = useMemo(() => discipulados.find((d) => d.id === filtro), [discipulados, filtro])
  const listagemSimples = Boolean(discipuladoAtual?.emFormacao)
  const podeCadastrar = podeEditar && filtro > 0

  function novo() {
    if (!filtro) {
      setErro('Selecione um discipulado para cadastrar.')
      return
    }
    setEditando(null)
    setForm({
      ...vazio,
      consentimentoEm: hoje(),
      discipuladoId: filtro,
      categoria: 'DISCIPULO',
    })
    setFamiliaForm(familiaNaoConsta())
    setSucesso('')
    setDrawerOpen(true)
  }

  function editar(a: Adolescente) {
    setEditando(a)
    setForm(inputFromAdolescente(a))
    setFamiliaForm(familiaNaoConsta())
    setSucesso('')
    setDrawerOpen(true)
  }

  function fecharDrawer() {
    if (!salvando) {
      setDrawerOpen(false)
      setEditando(null)
      setForm(vazio)
      setFamiliaForm(familiaNaoConsta())
    }
  }

  async function abrirFamilia(a: Adolescente) {
    setErro('')
    setFamiliaAlvo(a)
    try {
      const ficha = await familiaApi.obter(a.id)
      setFamiliaDialog({
        cep: ficha.cep,
        rua: ficha.rua,
        numero: ficha.numero,
        complemento: ficha.complemento,
        bairro: ficha.bairro,
        cidade: ficha.cidade,
        situacaoIgreja: ficha.situacaoIgreja,
        atuaOnde: ficha.atuaOnde,
        situacaoPais: ficha.situacaoPais,
        descricao: ficha.descricao,
        desafioFinanceiro: ficha.desafioFinanceiro,
        desafioEmocional: ficha.desafioEmocional,
        desafioEspiritual: ficha.desafioEspiritual,
        desafiosDescricao: ficha.desafiosDescricao,
        atividadesJuntas: ficha.atividadesJuntas,
        rotinaSemana: ficha.rotinaSemana,
        irmaoDokmos: ficha.irmaoDokmos,
        pedidoOracao: ficha.pedidoOracao,
        intervencao: ficha.intervencao,
        observacaoDiscipulador: ficha.observacaoDiscipulador,
        observacaoGerente: ficha.observacaoGerente,
        responsavel1: ficha.responsavel1,
        responsavel2: ficha.responsavel2,
      })
    } catch {
      setFamiliaDialog(familiaNaoConsta())
    }
  }

  async function salvarFamiliaDialog() {
    if (!familiaAlvo) return
    setSalvando(true)
    setErro('')
    try {
      await familiaApi.salvar(familiaAlvo.id, familiaDialog)
      setFamiliaAlvo(null)
      setSucesso('Ficha de família salva.')
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível salvar a ficha de família.')
    } finally {
      setSalvando(false)
    }
  }

  async function salvar(event: FormEvent) {
    event.preventDefault()
    const paraSalvar: AdolescenteInput = {
      ...form,
      categoria: listagemSimples ? 'DISCIPULO' : form.categoria,
    }
    if (!listagemSimples && paraSalvar.categoria === 'DISCIPULO_GOE' && !paraSalvar.motivoAfastamento?.trim()) {
      setErro('Informe o motivo do afastamento para Discípulo GOE.')
      return
    }
    const erroValidacao = validarFormularioAdolescente(paraSalvar)
    if (erroValidacao) {
      setErro(erroValidacao)
      return
    }
    if (!editando && podeFamilia) {
      const erroFamilia = validarFamiliaObrigatoria(familiaForm)
      if (erroFamilia) {
        setErro(erroFamilia)
        return
      }
    }
    setSalvando(true)
    setErro('')
    setSucesso('')
    try {
      const payload: AdolescenteInput = {
        ...paraSalvar,
        telefone: paraSalvar.naoPossuiTelefone ? '' : paraSalvar.telefone,
        naoPossuiTelefone: Boolean(paraSalvar.naoPossuiTelefone),
      }
      if (editando) await adolescentesApi.atualizar(editando.id, payload)
      else await adolescentesApi.criar(podeFamilia ? { ...payload, familia: toFamiliaPayload(familiaForm) } : payload)
      setSucesso(editando ? 'Adolescente atualizado.' : 'Adolescente cadastrado.')
      setEditando(null)
      setForm(vazio)
      setFamiliaForm(familiaNaoConsta())
      setDrawerOpen(false)
      await carregar()
      await carregarAlertas(filtro, listagemSimples)
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
    const jaTemTelefone = Boolean(alvo.telefone?.trim())
    const telefoneInformado = jaTemTelefone ? alvo.telefone : telefoneGoe.trim()
    if (!jaTemTelefone && !naoPossuiTelefoneGoe && !telefoneInformado) {
      setErro('Informe o telefone do adolescente ou marque que não possui telefone.')
      return
    }
    if (!naoPossuiTelefoneGoe && telefoneInformado && !telefoneValido(telefoneInformado)) {
      setErro(mensagemTelefoneInvalido('telefone do adolescente'))
      return
    }
    setSalvando(true)
    setErro('')
    try {
      await adolescentesApi.atualizar(alvo.id, {
        ...inputFromAdolescente(alvo),
        telefone: naoPossuiTelefoneGoe ? '' : telefoneInformado,
        naoPossuiTelefone: naoPossuiTelefoneGoe || !telefoneInformado,
        categoria: 'DISCIPULO_GOE',
        motivoAfastamento: motivoGoe.trim(),
      })
      setSucesso(`Status de ${alvo.nome} atualizado para Discípulo GOE.`)
      setIgnoradosGoe((prev) => [...prev, alertaAtual.adolescenteId])
      setConfirmarGoe(false)
      setMotivoGoe('')
      setTelefoneGoe('')
      setNaoPossuiTelefoneGoe(false)
      await carregar()
      await carregarAlertas(filtro, listagemSimples)
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível atualizar o status.')
    } finally {
      setSalvando(false)
    }
  }

  function acoesLinha(a: Adolescente) {
    const actions = [
      ...(podeFamilia ? [{ label: 'Ficha de família', onClick: () => void abrirFamilia(a) }] : []),
      ...(podeEditar
        ? [
            { label: 'Editar', onClick: () => editar(a) },
            {
              label: 'Transferir',
              onClick: () => {
                setTransferindo(a)
                setDestino(0)
              },
            },
            ...(a.ativo ? [{ label: 'Inativar', onClick: () => setInativando(a), color: 'warning' as const }] : []),
          ]
        : []),
      ...(podeAnonimizar && !a.anonimizado
        ? [{ label: 'Excluir dados', onClick: () => setAnonimizando(a), color: 'error' as const }]
        : []),
    ]
    return <RowActionsMenu ariaLabel={`Ações de ${a.nome}`} actions={actions} />
  }

  return (
    <Stack spacing={3}>
      <PageHeader
        title="Gestão de Discípulos"
        description={
          listagemSimples
            ? 'Liste os discípulos do grupo de formação selecionado.'
            : 'Liste discípulos, visitantes e discípulos GOE do discipulado selecionado.'
        }
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
          <Stack spacing={1.25} sx={{ minWidth: { xs: '100%', sm: 320 }, width: { xs: '100%', sm: 'auto' } }}>
            <Autocomplete
              options={discipulados}
              value={discipulados.find((d) => d.id === filtro) ?? null}
              onChange={(_, value) => {
                setFiltro(value?.id ?? 0)
                setIgnoradosGoe([])
              }}
              getOptionLabel={labelDiscipulado}
              isOptionEqualToValue={(option, value) => option.id === value.id}
              noOptionsText="Nenhum discipulado encontrado"
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Discipulado"
                  placeholder="Pesquisar discipulado ou discipulador"
                  required={discipulados.length > 1}
                  InputProps={{
                    ...params.InputProps,
                    startAdornment: (
                      <>
                        <InputAdornment position="start">
                          <SearchRounded fontSize="small" color="action" />
                        </InputAdornment>
                        {params.InputProps.startAdornment}
                      </>
                    ),
                  }}
                />
              )}
            />
            {discipuladoAtual && (
              <DiscipuladoLiderancaInfo
                discipuladorNome={discipuladoAtual.discipuladorNome}
                coLideres={discipuladoAtual.coLideres}
                faixaEtaria={discipuladoAtual.faixaEtaria}
                showFaixaEtaria={!listagemSimples}
                ocultarCoLideres={listagemSimples}
              />
            )}
          </Stack>
          <Typography variant="body2" color="text.secondary">
            {totalAtivos}{' '}
            {listagemSimples
              ? `discípulo${totalAtivos === 1 ? '' : 's'}`
              : `adolescente${totalAtivos === 1 ? '' : 's'}`}
          </Typography>
        </Stack>
      </FilterToolbar>

      {filtro === 0 ? (
        <EmptyState
          title="Selecione um discipulado"
          description="Gerentes e administradores visualizam o cadastro por discipulado."
        />
      ) : (
        <>
          {listagemSimples ? (
            <CategoriaSection
              titulo="Discípulos"
              items={items}
              empty="Nenhum discípulo neste discipulado de formação."
              acoes={acoesLinha}
              ocultarEstrutura
              onEditar={podeEditar ? editar : undefined}
              onAbrirFamilia={podeFamilia ? (a) => void abrirFamilia(a) : undefined}
            />
          ) : (
            <>
              <CategoriaSection
                titulo="Discípulos ativos"
                items={discipulos}
                empty="Nenhum discípulo ativo neste discipulado."
                acoes={acoesLinha}
                onEditar={podeEditar ? editar : undefined}
                onAbrirFamilia={podeFamilia ? (a) => void abrirFamilia(a) : undefined}
              />
              <CategoriaSection
                titulo="Visitantes"
                items={visitantes}
                empty="Nenhum visitante cadastrado neste discipulado."
                acoes={acoesLinha}
                onEditar={podeEditar ? editar : undefined}
                onAbrirFamilia={podeFamilia ? (a) => void abrirFamilia(a) : undefined}
              />
              <CategoriaSection
                titulo="Discípulos GOE"
                items={goe}
                empty="Nenhum discípulo GOE neste discipulado."
                acoes={acoesLinha}
                mostrarMotivo
                onEditar={podeEditar ? editar : undefined}
                onAbrirFamilia={podeFamilia ? (a) => void abrirFamilia(a) : undefined}
              />
            </>
          )}
        </>
      )}

      <FormSheet
        open={drawerOpen}
        onClose={fecharDrawer}
        title={editando ? (listagemSimples ? 'Editar discípulo' : 'Editar adolescente') : 'Cadastrar discípulo'}
        width={560}
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
        <AdolescenteFormFields
          value={form}
          onChange={(patch) => setForm({ ...form, ...patch })}
          disabled={salvando}
          listagemSimples={listagemSimples}
        />
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
                {labelDiscipulado(d)}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        {!editando && podeFamilia && (
          <FamilyFormFields value={familiaForm} onChange={setFamiliaForm} disabled={salvando} />
        )}
        {editando && (
          <Stack spacing={1.5}>
            <Stack direction="row" alignItems="center">
              <Switch checked={form.ativo} onChange={(e) => setForm({ ...form, ativo: e.target.checked })} />
              <Typography>Cadastro ativo</Typography>
            </Stack>
            {podeFamilia && (
              <Stack spacing={0.5}>
                <Typography variant="subtitle2" fontWeight={700}>
                  Ficha de família
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  A ficha é editada em janela própria para não misturar com os dados cadastrais.
                </Typography>
                <Button
                  variant="outlined"
                  onClick={() => void abrirFamilia(editando)}
                  disabled={salvando}
                  sx={{ alignSelf: 'flex-start' }}
                >
                  Abrir ficha de família
                </Button>
              </Stack>
            )}
          </Stack>
        )}
      </FormSheet>

      {podeFamilia && (
        <Dialog open={Boolean(familiaAlvo)} onClose={() => !salvando && setFamiliaAlvo(null)} fullWidth maxWidth="md">
          <DialogTitle>Família de {familiaAlvo?.nome}</DialogTitle>
          <DialogContent>
            <Stack spacing={2} mt={1}>
              <FamilyFormFields value={familiaDialog} onChange={setFamiliaDialog} disabled={salvando || !podeEditar} />
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setFamiliaAlvo(null)}>Cancelar</Button>
            {podeEditar && (
              <Button variant="contained" disabled={salvando} onClick={() => void salvarFamiliaDialog()}>
                {salvando ? 'Salvando...' : 'Salvar ficha'}
              </Button>
            )}
          </DialogActions>
        </Dialog>
      )}

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
                      {labelDiscipulado(d)}
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
            {alertaAtual && !items.find((a) => a.id === alertaAtual.adolescenteId)?.telefone?.trim() && (
              <>
                <TextField
                  required={!naoPossuiTelefoneGoe}
                  autoFocus
                  label="Telefone do adolescente"
                  value={naoPossuiTelefoneGoe ? '' : telefoneGoe}
                  disabled={naoPossuiTelefoneGoe}
                  onChange={(e) => {
                    setTelefoneGoe(e.target.value)
                    setNaoPossuiTelefoneGoe(false)
                  }}
                  error={Boolean(telefoneGoe.trim() && !telefoneValido(telefoneGoe))}
                  helperText={
                    naoPossuiTelefoneGoe
                      ? 'Cadastro permitido sem telefone. É possível informar depois na edição.'
                      : telefoneGoe.trim() && !telefoneValido(telefoneGoe)
                        ? mensagemTelefoneInvalido('telefone do adolescente')
                        : 'Obrigatório para Discípulo GOE, salvo se não possuir telefone.'
                  }
                />
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={naoPossuiTelefoneGoe}
                      onChange={(e) => {
                        setNaoPossuiTelefoneGoe(e.target.checked)
                        if (e.target.checked) setTelefoneGoe('')
                      }}
                    />
                  }
                  label="Não possui telefone"
                />
              </>
            )}
            <TextField
              required
              autoFocus={Boolean(items.find((a) => a.id === alertaAtual?.adolescenteId)?.telefone?.trim())}
              label="Motivo do afastamento"
              value={motivoGoe}
              multiline
              minRows={2}
              onChange={(e) => setMotivoGoe(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setConfirmarGoe(false)
              setNaoPossuiTelefoneGoe(false)
              setTelefoneGoe('')
            }}
          >
            Cancelar
          </Button>
          <Button
            variant="contained"
            disabled={
              salvando ||
              !motivoGoe.trim() ||
              (Boolean(alertaAtual) &&
                !items.find((a) => a.id === alertaAtual?.adolescenteId)?.telefone?.trim() &&
                !naoPossuiTelefoneGoe &&
                !telefoneGoe.trim())
            }
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
  ocultarEstrutura = false,
  onEditar,
  onAbrirFamilia,
}: {
  titulo: string
  items: Adolescente[]
  empty: string
  acoes: (a: Adolescente) => ReactNode
  mostrarMotivo?: boolean
  ocultarEstrutura?: boolean
  onEditar?: (a: Adolescente) => void
  onAbrirFamilia?: (a: Adolescente) => void
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
                {!ocultarEstrutura && <TableCell>Estrutura</TableCell>}
                {mostrarMotivo && <TableCell>Motivo do afastamento</TableCell>}
                <TableCell>Situação</TableCell>
                {onAbrirFamilia && <TableCell>Famílias</TableCell>}
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
                  {!ocultarEstrutura && <TableCell>{a.estrutura || '—'}</TableCell>}
                  {mostrarMotivo && <TableCell>{a.motivoAfastamento || '—'}</TableCell>}
                  <TableCell>
                    <StatusChip active={a.ativo} />
                  </TableCell>
                  {onAbrirFamilia && (
                    <TableCell>
                      <Button size="small" variant="outlined" onClick={() => onAbrirFamilia(a)}>
                        Abrir ficha
                      </Button>
                    </TableCell>
                  )}
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
