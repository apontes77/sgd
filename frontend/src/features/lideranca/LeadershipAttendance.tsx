import { CheckRounded, CloseRounded, SaveRounded } from '@mui/icons-material'
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  TextField,
  Typography,
  useMediaQuery,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import { type FormEvent, useCallback, useEffect, useMemo, useState } from 'react'

import {
  type ChamadaLiderancaResponse,
  type ConflitoPresencaLideranca,
  type DiscipuladoChamadaLideranca,
  type FiltroSexoLideranca,
  liderancaApi,
  type RegistroPresencaDoDia,
  type SalvarChamadaLiderancaRequest,
  type SituacaoPresencaLideranca,
} from '@/features/lideranca/api'
import { ApiError } from '@/shared/api/httpClient'
import { FilterToolbar, PageHeader, SectionCard } from '@/shared/ui'

const hojeLocal = () => {
  const agora = new Date()
  return `${agora.getFullYear()}-${String(agora.getMonth() + 1).padStart(2, '0')}-${String(agora.getDate()).padStart(2, '0')}`
}

const SEXO_OPCOES: { value: FiltroSexoLideranca; label: string }[] = [
  { value: 'TODOS', label: 'Todos' },
  { value: 'MASCULINO', label: 'Masculino' },
  { value: 'FEMININO', label: 'Feminino' },
]

type PresencasState = Record<number, Record<number, SituacaoPresencaLideranca | null>>
type ObservacoesState = Record<number, string>
type OpcoesPersistir = {
  observacaoGeralEnviada: string | null
  preservarObservacaoGeral: boolean
}
type PendenciaConfirmacao = {
  payload: SalvarChamadaLiderancaRequest['discipulados']
  conflitos: ConflitoPresencaLideranca[]
} & OpcoesPersistir

function normalizarBusca(valor: string) {
  return valor.trim().toLocaleLowerCase('pt-BR')
}

function combinaBusca(d: DiscipuladoChamadaLideranca, termo: string) {
  const t = normalizarBusca(termo)
  if (!t) return true
  if (normalizarBusca(d.discipuladoNome).includes(t)) return true
  return d.presencas.some((p) => normalizarBusca(p.nome).includes(t))
}

function rotuloSituacao(situacao: SituacaoPresencaLideranca) {
  return situacao === 'PRESENTE' ? 'Presente' : 'Ausente'
}

function situacaoDestaLinha(
  discipuladoId: number,
  situacao: SituacaoPresencaLideranca | null,
  registroDoDia?: RegistroPresencaDoDia | null,
) {
  if (registroDoDia && registroDoDia.discipuladoId !== discipuladoId) return null
  return situacao
}

function indexarRegistrosPersistidos(discipulados: DiscipuladoChamadaLideranca[]) {
  const registros = new Map<number, RegistroPresencaDoDia>()
  for (const d of discipulados) {
    for (const p of d.presencas) {
      if (p.registroDoDia && !registros.has(p.usuarioId)) registros.set(p.usuarioId, p.registroDoDia)
    }
  }
  return registros
}

function nomeDoUsuario(discipulados: DiscipuladoChamadaLideranca[], usuarioId: number) {
  return (
    discipulados.flatMap((d) => d.presencas).find((p) => p.usuarioId === usuarioId)?.nome ??
    'Este discipulador/co-líder'
  )
}

function conflitosDoPayload(
  payload: SalvarChamadaLiderancaRequest['discipulados'],
  discipulados: DiscipuladoChamadaLideranca[],
): ConflitoPresencaLideranca[] {
  const persistidos = indexarRegistrosPersistidos(discipulados)
  const conflitos: ConflitoPresencaLideranca[] = []
  const vistos = new Set<number>()
  for (const d of payload) {
    for (const p of d.presencas) {
      const existente = persistidos.get(p.usuarioId)
      if (!existente) continue
      if (existente.discipuladoId === d.discipuladoId && existente.situacao === p.situacao) continue
      if (!vistos.add(p.usuarioId)) continue
      conflitos.push({
        usuarioId: p.usuarioId,
        nome: nomeDoUsuario(discipulados, p.usuarioId),
        discipuladoId: existente.discipuladoId,
        discipuladoNome: existente.discipuladoNome,
        situacao: existente.situacao,
      })
    }
  }
  return conflitos
}

function conflitosDoErro(erro: unknown): ConflitoPresencaLideranca[] | undefined {
  if (!(erro instanceof ApiError) || erro.status !== 409 || !Array.isArray(erro.body?.conflitos)) return undefined
  const conflitos = erro.body.conflitos.filter(isConflitoPresenca)
  return conflitos.length > 0 ? conflitos : undefined
}

function isConflitoPresenca(valor: unknown): valor is ConflitoPresencaLideranca {
  if (!valor || typeof valor !== 'object') return false
  const item = valor as Record<string, unknown>
  return (
    typeof item.usuarioId === 'number' &&
    typeof item.nome === 'string' &&
    typeof item.discipuladoId === 'number' &&
    typeof item.discipuladoNome === 'string' &&
    (item.situacao === 'PRESENTE' || item.situacao === 'AUSENTE')
  )
}

function semUsuarios(
  payload: SalvarChamadaLiderancaRequest['discipulados'],
  usuarioIds: Set<number>,
): SalvarChamadaLiderancaRequest['discipulados'] {
  return payload.map((d) => ({
    ...d,
    presencas: d.presencas.filter((p) => !usuarioIds.has(p.usuarioId)),
  }))
}

function discipuladosParaSalvar(
  visiveis: DiscipuladoChamadaLideranca[],
  presencas: PresencasState,
  observacoes: ObservacoesState,
): SalvarChamadaLiderancaRequest['discipulados'] {
  return visiveis.flatMap((d) => {
    const situacoes = presencas[d.discipuladoId] ?? {}
    const marcadas = d.presencas.flatMap((p) => {
      const situacao = situacoes[p.usuarioId]
      if (!situacao) return []
      return [{ usuarioId: p.usuarioId, papel: p.papel, situacao }]
    })
    const observacao = observacoes[d.discipuladoId]?.trim() || null
    if (marcadas.length === 0 && observacao === (d.observacao ?? null)) return []
    return [{ discipuladoId: d.discipuladoId, observacao, presencas: marcadas }]
  })
}

function idsNaoSalvos(alterados: Set<number>, payload: SalvarChamadaLiderancaRequest['discipulados']): Set<number> {
  const salvos = new Set(payload.map((d) => d.discipuladoId))
  const restantes = new Set<number>()
  for (const id of alterados) {
    if (!salvos.has(id)) restantes.add(id)
  }
  return restantes
}

export default function LeadershipAttendance() {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const [data, setData] = useState(hojeLocal)
  const [filtroSexo, setFiltroSexo] = useState<FiltroSexoLideranca>('TODOS')
  const [buscaNome, setBuscaNome] = useState('')
  const [grade, setGrade] = useState<ChamadaLiderancaResponse>()
  const [presencas, setPresencas] = useState<PresencasState>({})
  const [observacoes, setObservacoes] = useState<ObservacoesState>({})
  const [observacaoGeral, setObservacaoGeral] = useState('')
  const [carregando, setCarregando] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [discipuladosAlterados, setDiscipuladosAlterados] = useState<Set<number>>(new Set())
  const [observacaoGeralAlterada, setObservacaoGeralAlterada] = useState(false)
  const [pendencia, setPendencia] = useState<PendenciaConfirmacao>()

  const aplicarGrade = useCallback(
    (
      resposta: ChamadaLiderancaResponse,
      preservar?: {
        discipuladoIds: Set<number>
        presencas: PresencasState
        observacoes: ObservacoesState
        observacaoGeral?: string
      },
    ) => {
      setGrade(resposta)
      const nextPresencas: PresencasState = {}
      const nextObs: ObservacoesState = {}
      for (const d of resposta.discipulados) {
        nextObs[d.discipuladoId] = d.observacao ?? ''
        nextPresencas[d.discipuladoId] = {}
        for (const p of d.presencas) {
          nextPresencas[d.discipuladoId][p.usuarioId] = situacaoDestaLinha(d.discipuladoId, p.situacao, p.registroDoDia)
        }
        if (preservar?.discipuladoIds.has(d.discipuladoId)) {
          if (preservar.presencas[d.discipuladoId])
            nextPresencas[d.discipuladoId] = { ...preservar.presencas[d.discipuladoId] }
          if (preservar.observacoes[d.discipuladoId] !== undefined)
            nextObs[d.discipuladoId] = preservar.observacoes[d.discipuladoId]
        }
      }
      setPresencas(nextPresencas)
      setObservacoes(nextObs)
      setObservacaoGeral(
        preservar?.observacaoGeral !== undefined ? preservar.observacaoGeral : (resposta.observacaoGeral ?? ''),
      )
      setDiscipuladosAlterados(preservar ? new Set(preservar.discipuladoIds) : new Set())
      setObservacaoGeralAlterada(preservar?.observacaoGeral !== undefined)
    },
    [],
  )

  const carregar = useCallback(
    async (dataConsulta: string) => {
      setCarregando(true)
      setErro('')
      setSucesso('')
      setPendencia(undefined)
      try {
        aplicarGrade(await liderancaApi.consultar(dataConsulta))
      } catch (e) {
        setGrade(undefined)
        setErro(e instanceof Error ? e.message : 'Não foi possível carregar a chamada.')
      } finally {
        setCarregando(false)
      }
    },
    [aplicarGrade],
  )

  useEffect(() => {
    void carregar(data)
  }, [carregar, data])

  const discipuladosVisiveis = useMemo(() => {
    if (!grade) return []
    return grade.discipulados.filter((d) => {
      if (filtroSexo !== 'TODOS' && d.sexo !== filtroSexo) return false
      return combinaBusca(d, buscaNome)
    })
  }, [grade, filtroSexo, buscaNome])

  const registrosPersistidos = useMemo(() => indexarRegistrosPersistidos(grade?.discipulados ?? []), [grade])

  const toolbarAlterada =
    observacaoGeralAlterada || discipuladosVisiveis.some((d) => discipuladosAlterados.has(d.discipuladoId))

  function marcarDiscipuladoAlterado(discipuladoId: number) {
    setDiscipuladosAlterados((prev) => {
      const next = new Set(prev)
      next.add(discipuladoId)
      return next
    })
    setSucesso('')
  }

  function marcar(discipuladoId: number, usuarioId: number, situacao: SituacaoPresencaLideranca) {
    setPresencas((prev) => {
      const next: PresencasState = {}
      for (const [id, situacoes] of Object.entries(prev)) {
        const atual = Number(id)
        next[atual] =
          atual === discipuladoId ? { ...situacoes, [usuarioId]: situacao } : { ...situacoes, [usuarioId]: null }
      }
      if (!next[discipuladoId]) next[discipuladoId] = { [usuarioId]: situacao }
      return next
    })
    marcarDiscipuladoAlterado(discipuladoId)
  }

  async function persistir(
    payloadDiscipulados: SalvarChamadaLiderancaRequest['discipulados'],
    confirmarAtualizacao: boolean,
    opcoes: OpcoesPersistir,
  ) {
    if (!grade) return
    const snapshotPresencas = presencas
    const snapshotObservacoes = observacoes
    const snapshotAlterados = discipuladosAlterados
    const snapshotObservacaoGeral = observacaoGeral
    setPendencia(undefined)
    setErro('')
    setSucesso('')
    setSalvando(true)
    try {
      const resposta = await liderancaApi.salvar({
        data: grade.data,
        observacaoGeral: opcoes.observacaoGeralEnviada,
        discipulados: payloadDiscipulados,
        confirmarAtualizacao: confirmarAtualizacao || undefined,
      })
      const preservarIds = idsNaoSalvos(snapshotAlterados, payloadDiscipulados)
      const devePreservar = preservarIds.size > 0 || opcoes.preservarObservacaoGeral
      aplicarGrade(
        resposta,
        devePreservar
          ? {
              discipuladoIds: preservarIds,
              presencas: snapshotPresencas,
              observacoes: snapshotObservacoes,
              observacaoGeral: opcoes.preservarObservacaoGeral ? snapshotObservacaoGeral : undefined,
            }
          : undefined,
      )
      setSucesso('Chamada de liderança salva.')
    } catch (e) {
      const conflitos = conflitosDoErro(e)
      if (conflitos) {
        setPendencia({ payload: payloadDiscipulados, conflitos, ...opcoes })
        return
      }
      setErro(e instanceof Error ? e.message : 'Não foi possível salvar a chamada.')
    } finally {
      setSalvando(false)
    }
  }

  function iniciarSalvamento(
    payloadDiscipulados: SalvarChamadaLiderancaRequest['discipulados'],
    opcoes: OpcoesPersistir,
  ) {
    if (!grade) return
    const conflitos = conflitosDoPayload(payloadDiscipulados, grade.discipulados)
    if (conflitos.length > 0) {
      setPendencia({ payload: payloadDiscipulados, conflitos, ...opcoes })
      setSucesso('')
      return
    }
    void persistir(payloadDiscipulados, false, opcoes)
  }

  async function salvar(event: FormEvent) {
    event.preventDefault()
    if (!grade) return
    iniciarSalvamento(discipuladosParaSalvar(discipuladosVisiveis, presencas, observacoes), {
      observacaoGeralEnviada: observacaoGeral.trim() || null,
      preservarObservacaoGeral: false,
    })
  }

  function salvarDiscipulado(item: DiscipuladoChamadaLideranca) {
    if (!grade) return
    const payload = discipuladosParaSalvar([item], presencas, observacoes)
    if (payload.length === 0) return
    iniciarSalvamento(payload, {
      observacaoGeralEnviada: grade.observacaoGeral,
      preservarObservacaoGeral: observacaoGeralAlterada,
    })
  }

  const mensagemVazia =
    normalizarBusca(buscaNome) !== ''
      ? 'Nenhum discipulador ou co-líder encontrado para a busca.'
      : 'Nenhum discipulado ativo para o filtro de sexo selecionado.'

  return (
    <Stack spacing={3} component="form" onSubmit={salvar}>
      <PageHeader
        title="Chamada de liderança"
        description="Registre a presença dos discipuladores e co-líderes por discipulado na sexta-feira. Se a chamada da pessoa já tiver sido salva, outro admin poderá atualizar depois de confirmar. É possível salvar parcialmente e continuar depois."
        eyebrow="Operações"
      />
      <FilterToolbar>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1.5}
          alignItems={{ sm: 'center' }}
          flexWrap="wrap"
          useFlexGap
        >
          <TextField
            type="date"
            label="Data"
            value={data}
            onChange={(e) => setData(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }}
            sx={{ width: { xs: '100%', sm: 220 } }}
          />
          <FormControl sx={{ minWidth: { xs: '100%', sm: 180 } }}>
            <InputLabel id="filtro-sexo-lideranca">Sexo</InputLabel>
            <Select
              labelId="filtro-sexo-lideranca"
              label="Sexo"
              value={filtroSexo}
              onChange={(e) => setFiltroSexo(e.target.value as FiltroSexoLideranca)}
            >
              {SEXO_OPCOES.map((opcao) => (
                <MenuItem key={opcao.value} value={opcao.value}>
                  {opcao.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <TextField
            label="Busca"
            placeholder="Pesquisar discipulador ou co-líder"
            value={buscaNome}
            onChange={(e) => setBuscaNome(e.target.value)}
            sx={{ flex: { sm: 1 }, minWidth: { xs: '100%', sm: 240 } }}
          />
          <Button
            type="submit"
            variant="contained"
            startIcon={<SaveRounded />}
            disabled={salvando || carregando || !grade || !toolbarAlterada}
            sx={{ width: { xs: '100%', sm: 'auto' }, flexShrink: 0 }}
          >
            {salvando ? 'Salvando...' : toolbarAlterada ? 'Salvar alterações' : 'Salvar'}
          </Button>
        </Stack>
      </FilterToolbar>
      {erro && <Alert severity="error">{erro}</Alert>}
      {sucesso && <Alert severity="success">{sucesso}</Alert>}
      {carregando && <Alert severity="info">Carregando chamada...</Alert>}
      {grade && !carregando && (
        <Stack spacing={2}>
          {discipuladosVisiveis.length === 0 && <Alert severity="info">{mensagemVazia}</Alert>}
          {discipuladosVisiveis.map((d) => (
            <DiscipuladoCard
              key={d.discipuladoId}
              item={d}
              mobile={mobile}
              observacao={observacoes[d.discipuladoId] ?? ''}
              onObservacao={(valor) => {
                setObservacoes((prev) => ({ ...prev, [d.discipuladoId]: valor }))
                marcarDiscipuladoAlterado(d.discipuladoId)
              }}
              situacoes={presencas[d.discipuladoId] ?? {}}
              registrosPersistidos={registrosPersistidos}
              onMarcar={(usuarioId, situacao) => marcar(d.discipuladoId, usuarioId, situacao)}
              alterado={discipuladosAlterados.has(d.discipuladoId)}
              salvando={salvando}
              onSalvar={() => salvarDiscipulado(d)}
            />
          ))}
          <SectionCard title="Observações gerais da sexta-feira">
            <TextField
              fullWidth
              multiline
              minRows={3}
              label="Observação geral"
              value={observacaoGeral}
              onChange={(e) => {
                setObservacaoGeral(e.target.value)
                setObservacaoGeralAlterada(true)
                setSucesso('')
              }}
              inputProps={{ maxLength: 1000 }}
            />
          </SectionCard>
        </Stack>
      )}
      <Dialog
        open={Boolean(pendencia)}
        onClose={() => setPendencia(undefined)}
        fullWidth
        maxWidth="sm"
        transitionDuration={0}
      >
        <DialogTitle>Chamada já salva</DialogTitle>
        <DialogContent>
          <Typography color="text.secondary">
            Este discipulador/co-líder já teve chamada salva. Tem certeza que quer atualizar essa chamada?
          </Typography>
          {pendencia && (
            <Stack component="ul" spacing={0.5} sx={{ pl: 2.5, mt: 2, mb: 0 }}>
              {pendencia.conflitos.map((conflito) => (
                <Typography key={conflito.usuarioId} component="li">
                  {conflito.nome} — {rotuloSituacao(conflito.situacao)} no discipulado {conflito.discipuladoNome}
                </Typography>
              ))}
            </Stack>
          )}
        </DialogContent>
        <DialogActions
          sx={{
            flexDirection: { xs: 'column-reverse', sm: 'row' },
            alignItems: { xs: 'stretch', sm: 'center' },
            gap: 1,
            px: { xs: 2, sm: 3 },
            pb: { xs: 2, sm: 2 },
          }}
        >
          <Button
            type="button"
            onClick={() => {
              if (!pendencia) return
              const ids = new Set(pendencia.conflitos.map((c) => c.usuarioId))
              void persistir(semUsuarios(pendencia.payload, ids), false, {
                observacaoGeralEnviada: pendencia.observacaoGeralEnviada,
                preservarObservacaoGeral: pendencia.preservarObservacaoGeral,
              })
            }}
            sx={{ width: { xs: '100%', sm: 'auto' } }}
          >
            Manter o que já estava
          </Button>
          <Button
            type="button"
            variant="contained"
            onClick={() => {
              if (!pendencia) return
              void persistir(pendencia.payload, true, {
                observacaoGeralEnviada: pendencia.observacaoGeralEnviada,
                preservarObservacaoGeral: pendencia.preservarObservacaoGeral,
              })
            }}
            sx={{ width: { xs: '100%', sm: 'auto' } }}
          >
            Atualizar chamada
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}

function DiscipuladoCard({
  item,
  mobile,
  observacao,
  onObservacao,
  situacoes,
  registrosPersistidos,
  onMarcar,
  alterado,
  salvando,
  onSalvar,
}: {
  item: DiscipuladoChamadaLideranca
  mobile: boolean
  observacao: string
  onObservacao: (valor: string) => void
  situacoes: Record<number, SituacaoPresencaLideranca | null>
  registrosPersistidos: Map<number, RegistroPresencaDoDia>
  onMarcar: (usuarioId: number, situacao: SituacaoPresencaLideranca) => void
  alterado: boolean
  salvando: boolean
  onSalvar: () => void
}) {
  return (
    <Paper
      variant="outlined"
      sx={{
        p: { xs: 1.5, sm: 2.5 },
        borderRadius: '16px',
        borderColor: (theme) => theme.app.border.subtle,
      }}
    >
      <Stack spacing={{ xs: 1.5, sm: 2 }}>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1.25}
          alignItems={{ xs: 'stretch', sm: 'flex-start' }}
          justifyContent="space-between"
        >
          <Box sx={{ minWidth: 0, flex: 1 }}>
            <Typography variant="h6" sx={{ overflowWrap: 'anywhere' }}>
              {item.discipuladoNome}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ overflowWrap: 'anywhere' }}>
              {item.gerenciaNome} · {item.sexo === 'MASCULINO' ? 'Masculino' : 'Feminino'}
            </Typography>
          </Box>
          <Button
            type="button"
            variant="contained"
            size={mobile ? 'medium' : 'small'}
            startIcon={<SaveRounded />}
            aria-label={`Salvar ${item.discipuladoNome}`}
            disabled={salvando || !alterado}
            onClick={onSalvar}
            sx={{ width: { xs: '100%', sm: 'auto' }, flexShrink: 0, whiteSpace: 'nowrap' }}
          >
            {salvando ? 'Salvando...' : 'Salvar'}
          </Button>
        </Stack>
        <Stack spacing={1.25}>
          {item.presencas.map((p) => {
            const situacao = situacoes[p.usuarioId] ?? null
            const registro = registrosPersistidos.get(p.usuarioId)
            const avisoId = `aviso-${item.discipuladoId}-${p.usuarioId}`
            return (
              <Stack
                key={`${p.usuarioId}-${p.papel}`}
                direction={{ xs: 'column', sm: 'row' }}
                spacing={1}
                alignItems={{ sm: 'center' }}
                justifyContent="space-between"
              >
                <Box sx={{ minWidth: 0 }}>
                  <Typography fontWeight={600} sx={{ overflowWrap: 'anywhere' }}>
                    {p.nome}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {p.papel === 'DISCIPULADOR' ? 'Discipulador' : 'Co-líder'}
                  </Typography>
                  {registro && (
                    <Typography id={avisoId} variant="caption" color="text.secondary" display="block">
                      {registro.discipuladoId === item.discipuladoId
                        ? `Chamada já salva nesta sexta como ${rotuloSituacao(registro.situacao)}`
                        : `Chamada já salva em ${registro.discipuladoNome} nesta sexta`}
                    </Typography>
                  )}
                </Box>
                <Stack
                  direction="row"
                  spacing={1}
                  useFlexGap
                  flexWrap="wrap"
                  sx={{ width: { xs: '100%', sm: 'auto' } }}
                >
                  <Button
                    size={mobile ? 'medium' : 'small'}
                    variant={situacao === 'PRESENTE' ? 'contained' : 'outlined'}
                    color="success"
                    startIcon={<CheckRounded />}
                    aria-describedby={registro ? avisoId : undefined}
                    onClick={() => onMarcar(p.usuarioId, 'PRESENTE')}
                    sx={{
                      flex: { xs: 1, sm: 'none' },
                      minWidth: { xs: 0, sm: 'auto' },
                      bgcolor: situacao === 'PRESENTE' ? undefined : (theme) => alpha(theme.palette.success.main, 0.04),
                    }}
                  >
                    Presente
                  </Button>
                  <Button
                    size={mobile ? 'medium' : 'small'}
                    variant={situacao === 'AUSENTE' ? 'contained' : 'outlined'}
                    color="warning"
                    startIcon={<CloseRounded />}
                    onClick={() => onMarcar(p.usuarioId, 'AUSENTE')}
                    sx={{
                      flex: { xs: 1, sm: 'none' },
                      minWidth: { xs: 0, sm: 'auto' },
                    }}
                  >
                    Ausente
                  </Button>
                </Stack>
              </Stack>
            )
          })}
        </Stack>
        <TextField
          fullWidth
          multiline
          minRows={2}
          label="Observação do discipulado"
          value={observacao}
          onChange={(e) => onObservacao(e.target.value)}
          inputProps={{ maxLength: 500 }}
        />
      </Stack>
    </Paper>
  )
}
