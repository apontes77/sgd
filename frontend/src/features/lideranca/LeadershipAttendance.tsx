import { CheckRounded, CloseRounded, SaveRounded } from '@mui/icons-material'
import {
  Alert,
  Box,
  Button,
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
  type DiscipuladoChamadaLideranca,
  type FiltroSexoLideranca,
  liderancaApi,
  type SalvarChamadaLiderancaRequest,
  type SituacaoPresencaLideranca,
} from '@/features/lideranca/api'
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

function normalizarBusca(valor: string) {
  return valor.trim().toLocaleLowerCase('pt-BR')
}

function combinaBusca(d: DiscipuladoChamadaLideranca, termo: string) {
  const t = normalizarBusca(termo)
  if (!t) return true
  if (normalizarBusca(d.discipuladoNome).includes(t)) return true
  return d.presencas.some((p) => normalizarBusca(p.nome).includes(t))
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
  const [alterado, setAlterado] = useState(false)

  const aplicarGrade = useCallback((resposta: ChamadaLiderancaResponse) => {
    setGrade(resposta)
    setObservacaoGeral(resposta.observacaoGeral ?? '')
    const nextPresencas: PresencasState = {}
    const nextObs: ObservacoesState = {}
    for (const d of resposta.discipulados) {
      nextObs[d.discipuladoId] = d.observacao ?? ''
      nextPresencas[d.discipuladoId] = {}
      for (const p of d.presencas) {
        nextPresencas[d.discipuladoId][p.usuarioId] = p.situacao
      }
    }
    setPresencas(nextPresencas)
    setObservacoes(nextObs)
    setAlterado(false)
  }, [])

  const carregar = useCallback(
    async (dataConsulta: string) => {
      setCarregando(true)
      setErro('')
      setSucesso('')
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

  function marcar(discipuladoId: number, usuarioId: number, situacao: SituacaoPresencaLideranca) {
    setPresencas((prev) => ({
      ...prev,
      [discipuladoId]: { ...prev[discipuladoId], [usuarioId]: situacao },
    }))
    setAlterado(true)
    setSucesso('')
  }

  async function salvar(event: FormEvent) {
    event.preventDefault()
    if (!grade) return
    setErro('')
    setSucesso('')
    const payloadDiscipulados = discipuladosParaSalvar(discipuladosVisiveis, presencas, observacoes)
    setSalvando(true)
    try {
      const resposta = await liderancaApi.salvar({
        data: grade.data,
        observacaoGeral: observacaoGeral.trim() || null,
        discipulados: payloadDiscipulados,
      })
      aplicarGrade(resposta)
      setSucesso('Chamada de liderança salva.')
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível salvar a chamada.')
    } finally {
      setSalvando(false)
    }
  }

  const mensagemVazia =
    normalizarBusca(buscaNome) !== ''
      ? 'Nenhum discipulador ou co-líder encontrado para a busca.'
      : 'Nenhum discipulado ativo para o filtro de sexo selecionado.'

  return (
    <Stack spacing={3} component="form" onSubmit={salvar}>
      <PageHeader
        title="Chamada de liderança"
        description="Registre a presença dos discipuladores e co-líderes por discipulado na sexta-feira. É possível salvar parcialmente e continuar depois."
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
            disabled={salvando || carregando || !grade || !alterado}
          >
            {salvando ? 'Salvando...' : alterado ? 'Salvar alterações' : 'Salvar'}
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
                setAlterado(true)
                setSucesso('')
              }}
              situacoes={presencas[d.discipuladoId] ?? {}}
              onMarcar={(usuarioId, situacao) => marcar(d.discipuladoId, usuarioId, situacao)}
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
                setAlterado(true)
                setSucesso('')
              }}
              inputProps={{ maxLength: 1000 }}
            />
          </SectionCard>
        </Stack>
      )}
    </Stack>
  )
}

function DiscipuladoCard({
  item,
  mobile,
  observacao,
  onObservacao,
  situacoes,
  onMarcar,
}: {
  item: DiscipuladoChamadaLideranca
  mobile: boolean
  observacao: string
  onObservacao: (valor: string) => void
  situacoes: Record<number, SituacaoPresencaLideranca | null>
  onMarcar: (usuarioId: number, situacao: SituacaoPresencaLideranca) => void
}) {
  return (
    <Paper
      variant="outlined"
      sx={{
        p: { xs: 2, sm: 2.5 },
        borderRadius: '16px',
        borderColor: (theme) => theme.app.border.subtle,
      }}
    >
      <Stack spacing={2}>
        <Box>
          <Typography variant="h6">{item.discipuladoNome}</Typography>
          <Typography variant="body2" color="text.secondary">
            {item.gerenciaNome} · {item.sexo === 'MASCULINO' ? 'Masculino' : 'Feminino'}
          </Typography>
        </Box>
        <Stack spacing={1.25}>
          {item.presencas.map((p) => {
            const situacao = situacoes[p.usuarioId] ?? null
            return (
              <Stack
                key={p.usuarioId}
                direction={{ xs: 'column', sm: 'row' }}
                spacing={1}
                alignItems={{ sm: 'center' }}
                justifyContent="space-between"
              >
                <Box>
                  <Typography fontWeight={600}>{p.nome}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {p.papel === 'DISCIPULADOR' ? 'Discipulador' : 'Co-líder'}
                  </Typography>
                </Box>
                <Stack direction="row" spacing={1}>
                  <Button
                    size={mobile ? 'medium' : 'small'}
                    variant={situacao === 'PRESENTE' ? 'contained' : 'outlined'}
                    color="success"
                    startIcon={<CheckRounded />}
                    onClick={() => onMarcar(p.usuarioId, 'PRESENTE')}
                    sx={{
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
