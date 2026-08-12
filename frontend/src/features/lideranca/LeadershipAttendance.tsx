import { CheckRounded, CloseRounded, SaveRounded } from '@mui/icons-material'
import { Alert, Box, Button, Paper, Stack, TextField, Typography, useMediaQuery } from '@mui/material'
import { alpha } from '@mui/material/styles'
import { type FormEvent, useCallback, useEffect, useMemo, useState } from 'react'

import {
  type ChamadaLiderancaResponse,
  type DiscipuladoChamadaLideranca,
  liderancaApi,
  type SituacaoPresencaLideranca,
} from '@/features/lideranca/api'
import { FilterToolbar, PageHeader, SectionCard } from '@/shared/ui'

const hojeLocal = () => {
  const agora = new Date()
  return `${agora.getFullYear()}-${String(agora.getMonth() + 1).padStart(2, '0')}-${String(agora.getDate()).padStart(2, '0')}`
}

type PresencasState = Record<number, Record<number, SituacaoPresencaLideranca | null>>
type ObservacoesState = Record<number, string>

export default function LeadershipAttendance() {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const [data, setData] = useState(hojeLocal)
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

  const incompleto = useMemo(() => {
    if (!grade) return true
    return grade.discipulados.some((d) => d.presencas.some((p) => !presencas[d.discipuladoId]?.[p.usuarioId]))
  }, [grade, presencas])

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
    if (incompleto) {
      setErro('Marque presença ou ausência para todos os líderes e co-líderes.')
      return
    }
    setSalvando(true)
    try {
      const resposta = await liderancaApi.salvar({
        data: grade.data,
        observacaoGeral: observacaoGeral.trim() || null,
        discipulados: grade.discipulados.map((d) => ({
          discipuladoId: d.discipuladoId,
          observacao: observacoes[d.discipuladoId]?.trim() || null,
          presencas: d.presencas.map((p) => ({
            usuarioId: p.usuarioId,
            papel: p.papel,
            situacao: presencas[d.discipuladoId][p.usuarioId] as SituacaoPresencaLideranca,
          })),
        })),
      })
      aplicarGrade(resposta)
      setSucesso('Chamada de liderança salva.')
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível salvar a chamada.')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <Stack spacing={3} component="form" onSubmit={salvar}>
      <PageHeader
        title="Chamada de liderança"
        description="Registre a presença dos discipuladores e co-líderes por discipulado na sexta-feira."
        eyebrow="Operações"
      />
      <FilterToolbar>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ sm: 'center' }}>
          <TextField
            type="date"
            label="Data"
            value={data}
            onChange={(e) => setData(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }}
            sx={{ width: { xs: '100%', sm: 220 } }}
          />
          <Button
            type="submit"
            variant="contained"
            startIcon={<SaveRounded />}
            disabled={salvando || carregando || !grade}
          >
            {salvando ? 'Salvando...' : alterado ? 'Salvar alterações' : 'Salvar'}
          </Button>
        </Stack>
      </FilterToolbar>
      {erro && <Alert severity="error">{erro}</Alert>}
      {sucesso && <Alert severity="success">{sucesso}</Alert>}
      {carregando && <Alert severity="info">Carregando chamada...</Alert>}
      {grade && (
        <Stack spacing={2}>
          {grade.discipulados.map((d) => (
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
            {item.gerenciaNome}
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
