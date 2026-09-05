import {
  CheckRounded,
  CloseRounded,
  DeleteOutlineRounded,
  EventAvailableRounded,
  EventBusyRounded,
  EventRounded,
  GroupsRounded,
  PersonAddAltRounded,
  SaveRounded,
} from '@mui/icons-material'
import {
  Alert,
  Avatar,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Paper,
  Stack,
  TextField,
  Typography,
  useMediaQuery,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import { type FormEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react'

import { AdolescenteFormFields, type DadosPessoaisAdolescente } from '@/features/adolescentes/AdolescenteFormFields'
import { adolescentesApi } from '@/features/adolescentes/api'
import { type FamiliaInput, familiaNaoConsta, toFamiliaPayload } from '@/features/familia/api'
import { FamilyFormFields } from '@/features/familia/FamilyFormFields'
import {
  type AdolescenteResumo,
  type CategoriaAdolescente,
  type Encontro,
  frequenciaApi,
  type SituacaoFrequencia,
} from '@/features/frequencia/api'
import { AVISO_LANCAMENTO_PENDENTE, dentroDoPrazoLancamento, ehSexta } from '@/features/frequencia/prazoLancamento'
import { ApiError } from '@/shared/api/httpClient'
import type { Discipulado } from '@/shared/api/types'
import { BOTTOM_NAV_OFFSET, DiscipuladoLiderancaInfo, EmptyState, SectionCard } from '@/shared/ui'
import { mensagemTelefoneInvalido, telefoneValido } from '@/shared/validation/telefone'

interface Props {
  discipuladoId: number
  discipulado?: Discipulado
  podeAdministrar?: boolean
  podeFamilia?: boolean
  podeRegistrarNaoRealizacao?: boolean
}
interface ParticipanteChamada extends AdolescenteResumo {
  registroAnterior: boolean
}

const hoje = () => {
  const agora = new Date()
  // Data local do usuário; toISOString() usaria UTC e viraria "amanhã" à noite no Brasil.
  return `${agora.getFullYear()}-${String(agora.getMonth() + 1).padStart(2, '0')}-${String(agora.getDate()).padStart(2, '0')}`
}
const visitanteVazio: DadosPessoaisAdolescente = {
  nome: '',
  dataNascimento: '',
  telefone: '',
  naoPossuiTelefone: false,
  instagram: '',
  consentimentoEm: '',
  categoria: 'VISITANTE',
  estrutura: '',
  motivoAfastamento: '',
}

export default function FrequencyManagement({
  discipuladoId,
  discipulado,
  podeAdministrar = false,
  podeFamilia = false,
  podeRegistrarNaoRealizacao = false,
}: Props) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const [data, setData] = useState(hoje)
  const [selecionado, setSelecionado] = useState<Encontro>()
  const [adolescentesAtuais, setAdolescentesAtuais] = useState<AdolescenteResumo[]>([])
  const [participantes, setParticipantes] = useState<ParticipanteChamada[]>([])
  const [chamada, setChamada] = useState<Record<number, SituacaoFrequencia>>({})
  const [intencao, setIntencao] = useState<'escolha' | 'justificando'>('escolha')
  const [justificativa, setJustificativa] = useState('')
  const [observacao, setObservacao] = useState('')
  const [alterado, setAlterado] = useState(false)
  const [carregando, setCarregando] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [visitante, setVisitante] = useState<DadosPessoaisAdolescente>()
  const [familiaVisitante, setFamiliaVisitante] = useState<FamiliaInput>(familiaNaoConsta())
  const [confirmandoExclusao, setConfirmandoExclusao] = useState(false)
  const requisicao = useRef(0)

  const listagemSimples = Boolean(discipulado?.emFormacao)
  const prazoAberto = useMemo(
    () => podeAdministrar || listagemSimples || dentroDoPrazoLancamento(data),
    [podeAdministrar, listagemSimples, data],
  )
  const editavel = useMemo(() => {
    if (!selecionado || selecionado.situacao !== 'REALIZADO') return false
    if (podeAdministrar) return true
    if (selecionado.chamadaSalvaEm) {
      return Date.now() <= new Date(selecionado.chamadaSalvaEm).getTime() + 3 * 60 * 60 * 1000
    }
    return listagemSimples || dentroDoPrazoLancamento(selecionado.data)
  }, [selecionado, podeAdministrar, listagemSimples])
  const podeEditarNaoRealizado = useMemo(
    () => Boolean(podeAdministrar || (podeRegistrarNaoRealizacao && prazoAberto)),
    [podeAdministrar, podeRegistrarNaoRealizacao, prazoAberto],
  )
  const presentesOpcionais = useMemo(
    () =>
      participantes.filter((a) => presencaOpcional(a.categoria, listagemSimples) && chamada[a.id] === 'PRESENTE')
        .length,
    [participantes, chamada, listagemSimples],
  )
  const discipulos = useMemo(
    () => participantes.filter((a) => !presencaOpcional(a.categoria, listagemSimples)),
    [participantes, listagemSimples],
  )
  const opcionais = useMemo(
    () => participantes.filter((a) => presencaOpcional(a.categoria, listagemSimples)),
    [participantes, listagemSimples],
  )
  const presentesDiscipulos = useMemo(
    () => discipulos.filter((a) => chamada[a.id] === 'PRESENTE').length,
    [discipulos, chamada],
  )
  const presentes = presentesDiscipulos + presentesOpcionais
  const ausentes = discipulos.length - presentesDiscipulos

  const carregarChamada = useCallback(
    async (encontro: Encontro, atuais: AdolescenteResumo[]) => {
      const atual = requisicao.current
      const existentes = await frequenciaApi.listarChamada(encontro.id)
      if (atual !== requisicao.current) return
      const idsAtuais = new Set(atuais.map((a) => a.id))
      const lista: ParticipanteChamada[] = [
        ...atuais.map((a) => ({ ...a, registroAnterior: false })),
        ...existentes
          .filter((f) => !idsAtuais.has(f.adolescenteId))
          .map((f) => ({
            id: f.adolescenteId,
            nome: f.adolescenteNome,
            categoria: f.categoria,
            registroAnterior: true,
          })),
      ]
      const mapa: Record<number, SituacaoFrequencia> = {}
      lista.forEach((a) => {
        if (!presencaOpcional(a.categoria, listagemSimples)) mapa[a.id] = 'AUSENTE'
      })
      existentes.forEach((f) => {
        if (presencaOpcional(f.categoria, listagemSimples)) {
          if (f.situacao === 'PRESENTE') mapa[f.adolescenteId] = 'PRESENTE'
          return
        }
        mapa[f.adolescenteId] = f.situacao
      })
      setParticipantes(lista)
      setChamada(mapa)
      setAlterado(false)
    },
    [listagemSimples],
  )

  const carregarData = useCallback(
    async (dataSelecionada: string) => {
      const atual = ++requisicao.current
      setCarregando(true)
      setErro('')
      setSucesso('')
      setIntencao('escolha')
      setJustificativa('')
      setObservacao('')
      setSelecionado(undefined)
      setParticipantes([])
      setChamada({})
      setAlterado(false)
      try {
        const [encontros, pagina] = await Promise.all([
          frequenciaApi.listarEncontros(discipuladoId, dataSelecionada, dataSelecionada),
          frequenciaApi.listarAdolescentes(discipuladoId),
        ])
        if (atual !== requisicao.current) return
        setAdolescentesAtuais(pagina.content)
        const existente = encontros.find((e) => e.data === dataSelecionada)
        if (existente) {
          setSelecionado(existente)
          setObservacao(existente.observacao ?? '')
          if (existente.situacao === 'REALIZADO') await carregarChamada(existente, pagina.content)
          else setJustificativa(existente.justificativa ?? '')
        }
      } catch (e) {
        if (atual === requisicao.current) setErro(mensagem(e))
      } finally {
        if (atual === requisicao.current) setCarregando(false)
      }
    },
    [discipuladoId, carregarChamada],
  )

  useEffect(() => {
    void carregarData(data)
  }, [carregarData, data])

  async function houveDiscipulado() {
    setSalvando(true)
    setErro('')
    setSucesso('')
    try {
      const novo = await frequenciaApi.criarEncontro({ discipuladoId, data, situacao: 'REALIZADO' })
      requisicao.current++
      setSelecionado(novo)
      await carregarChamada(novo, adolescentesAtuais)
    } catch (e) {
      setErro(mensagem(e))
    } finally {
      setSalvando(false)
    }
  }

  async function confirmarAusencia() {
    const motivo = justificativa.trim()
    if (!motivo) {
      setErro('Descreva por que não houve discipulado.')
      return
    }
    setSalvando(true)
    setErro('')
    setSucesso('')
    try {
      const novo = await frequenciaApi.criarEncontro({
        discipuladoId,
        data,
        situacao: 'NAO_REALIZADO',
        justificativa: motivo,
      })
      requisicao.current++
      setSelecionado(novo)
      setParticipantes([])
      setChamada({})
      setIntencao('escolha')
      setSucesso('Registro de ausência confirmado.')
    } catch (e) {
      setErro(mensagem(e))
    } finally {
      setSalvando(false)
    }
  }

  async function salvarFrequencia() {
    if (!selecionado) return
    setSalvando(true)
    setErro('')
    setSucesso('')
    try {
      const atualizado = await frequenciaApi.atualizarEncontro(selecionado.id, {
        observacao: observacao.trim() || null,
      })
      await frequenciaApi.salvarChamada(selecionado.id, [
        ...discipulos.map((a) => ({ adolescenteId: a.id, situacao: chamada[a.id] ?? 'AUSENTE' })),
        ...opcionais
          .filter((a) => chamada[a.id] === 'PRESENTE')
          .map((a) => ({ adolescenteId: a.id, situacao: 'PRESENTE' as const })),
      ])
      setSelecionado({
        ...atualizado,
        atualizadoEm: atualizado.atualizadoEm ?? new Date().toISOString(),
        chamadaSalvaEm: atualizado.chamadaSalvaEm ?? new Date().toISOString(),
      })
      setObservacao(atualizado.observacao ?? '')
      setAlterado(false)
      setSucesso('Frequência salva.')
    } catch (e) {
      setErro(mensagem(e))
    } finally {
      setSalvando(false)
    }
  }

  async function salvarJustificativa() {
    if (!selecionado) return
    const motivo = justificativa.trim()
    if (!motivo) {
      setErro('A justificativa é obrigatória.')
      return
    }
    setSalvando(true)
    setErro('')
    setSucesso('')
    try {
      const atualizado = await frequenciaApi.atualizarEncontro(selecionado.id, {
        situacao: 'NAO_REALIZADO',
        justificativa: motivo,
      })
      setSelecionado(atualizado)
      setSucesso('Justificativa atualizada.')
    } catch (e) {
      setErro(mensagem(e))
    } finally {
      setSalvando(false)
    }
  }

  async function corrigirParaRealizado() {
    if (!selecionado) return
    setSalvando(true)
    setErro('')
    setSucesso('')
    try {
      const atualizado = await frequenciaApi.atualizarEncontro(selecionado.id, { situacao: 'REALIZADO' })
      requisicao.current++
      setSelecionado(atualizado)
      await carregarChamada(atualizado, adolescentesAtuais)
      setSucesso(
        atualizado.fechamentoAutomatico
          ? 'Frequência liberada para preenchimento. O aviso de não lançamento pelo líder permanece.'
          : 'Corrigido: discipulado marcado como realizado.',
      )
    } catch (e) {
      setErro(mensagem(e))
    } finally {
      setSalvando(false)
    }
  }

  async function excluirFrequencia() {
    if (!selecionado) return
    setSalvando(true)
    setErro('')
    setSucesso('')
    try {
      await frequenciaApi.excluirEncontro(selecionado.id)
      setConfirmandoExclusao(false)
      await carregarData(data)
      setSucesso('Frequência excluída. A data ficou livre para um novo lançamento.')
    } catch (e) {
      setErro(mensagem(e))
    } finally {
      setSalvando(false)
    }
  }

  async function adicionarVisitante() {
    if (!visitante) return
    const nome = visitante.nome.trim()
    if (!nome || !visitante.dataNascimento) {
      setErro('Informe nome e data de nascimento do visitante.')
      return
    }
    if (!visitante.consentimentoEm) {
      setErro('Informe a data de consentimento do visitante.')
      return
    }
    const telefones: Array<[string | undefined, string]> = []
    if (!visitante.naoPossuiTelefone) telefones.push([visitante.telefone, 'telefone do adolescente'])
    for (const [valor, rotulo] of telefones) {
      if (!telefoneValido(valor)) {
        setErro(mensagemTelefoneInvalido(rotulo))
        return
      }
    }
    setSalvando(true)
    setErro('')
    try {
      const criado = await adolescentesApi.criar({
        nome,
        dataNascimento: visitante.dataNascimento,
        telefone: visitante.naoPossuiTelefone ? undefined : visitante.telefone || undefined,
        naoPossuiTelefone: Boolean(visitante.naoPossuiTelefone),
        instagram: visitante.instagram || undefined,
        consentimentoEm: visitante.consentimentoEm,
        categoria: 'VISITANTE',
        estrutura: visitante.estrutura || undefined,
        discipuladoId,
        ativo: true,
        dataInicio: data,
        ...(podeFamilia ? { familia: toFamiliaPayload(familiaVisitante) } : {}),
      })
      setParticipantes((atual) => [
        ...atual,
        { id: criado.id, nome: criado.nome, categoria: 'VISITANTE', registroAnterior: false },
      ])
      setChamada((atual) => ({ ...atual, [criado.id]: 'PRESENTE' }))
      setAlterado(true)
      setVisitante(undefined)
      setFamiliaVisitante(familiaNaoConsta())
      setSucesso('Visitante adicionado. Salve a frequência para confirmar.')
    } catch (e) {
      setErro(mensagem(e))
    } finally {
      setSalvando(false)
    }
  }

  function definirSituacao(id: number, situacao: SituacaoFrequencia | null) {
    setChamada((atual) => {
      const proximo = { ...atual }
      if (situacao == null) delete proximo[id]
      else proximo[id] = situacao
      return proximo
    })
    setAlterado(true)
  }
  function definirTodos(situacao: SituacaoFrequencia) {
    setChamada((atual) => {
      const mapa = { ...atual }
      discipulos.forEach((a) => (mapa[a.id] = situacao))
      return mapa
    })
    setAlterado(true)
  }

  const dataFormatada = formatarData(data)

  return (
    <Stack spacing={3}>
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

      <SectionCard
        title="Data do discipulado"
        description="Escolha a data para registrar a frequência."
        icon={<EventRounded />}
      >
        <TextField
          fullWidth
          label="Data"
          type="date"
          value={data}
          onChange={(e) => setData(e.target.value)}
          slotProps={{ inputLabel: { shrink: true } }}
        />
      </SectionCard>

      {!carregando && !selecionado && !prazoAberto && ehSexta(data) && (
        <Alert severity="warning">
          O prazo para lançar a frequência desta sexta encerrou no domingo subsequente. Sem registro, o encontro é
          marcado automaticamente como não realizado.
        </Alert>
      )}

      {!carregando && !selecionado && prazoAberto && intencao === 'escolha' && (
        <SectionCard
          title={`O que aconteceu em ${dataFormatada}?`}
          description="Informe se o discipulado ocorreu nesta data."
        >
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', sm: podeRegistrarNaoRealizacao ? 'repeat(2,1fr)' : '1fr' },
              gap: 2,
            }}
          >
            <Button
              size="large"
              variant="contained"
              color="success"
              startIcon={<EventAvailableRounded />}
              disabled={salvando}
              onClick={() => void houveDiscipulado()}
              sx={{ py: 2.5, flexDirection: 'column', gap: 0.5, fontSize: '1rem' }}
            >
              Houve discipulado
              <Typography variant="caption" sx={{ opacity: 0.85 }}>
                {listagemSimples ? 'Registrar a presença dos discípulos' : 'Registrar a presença dos adolescentes'}
              </Typography>
            </Button>
            {podeRegistrarNaoRealizacao && (
              <Button
                size="large"
                variant="outlined"
                color="warning"
                startIcon={<EventBusyRounded />}
                disabled={salvando}
                onClick={() => {
                  setIntencao('justificando')
                  setErro('')
                }}
                sx={{ py: 2.5, flexDirection: 'column', gap: 0.5, fontSize: '1rem' }}
              >
                Não houve discipulado
                <Typography variant="caption" sx={{ opacity: 0.85 }}>
                  Registrar o motivo da ausência
                </Typography>
              </Button>
            )}
          </Box>
        </SectionCard>
      )}

      {!carregando && !selecionado && prazoAberto && intencao === 'justificando' && (
        <SectionCard title="Por que não houve discipulado?" description="A justificativa é obrigatória.">
          <Stack spacing={2}>
            <TextField
              fullWidth
              required
              autoFocus
              multiline
              minRows={3}
              label="Justificativa"
              value={justificativa}
              onChange={(e) => setJustificativa(e.target.value.slice(0, 500))}
              helperText={`${justificativa.length}/500 caracteres`}
              inputProps={{ maxLength: 500 }}
            />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <Button
                variant="contained"
                disabled={salvando || !justificativa.trim()}
                onClick={() => void confirmarAusencia()}
              >
                Confirmar
              </Button>
              <Button
                color="inherit"
                disabled={salvando}
                onClick={() => {
                  setIntencao('escolha')
                  setJustificativa('')
                }}
              >
                Voltar
              </Button>
            </Stack>
          </Stack>
        </SectionCard>
      )}

      {selecionado?.situacao === 'NAO_REALIZADO' && (
        <SectionCard
          title={`Não houve discipulado em ${dataFormatada}`}
          description="Registro de ausência com justificativa."
          icon={<EventBusyRounded />}
        >
          <Stack spacing={2}>
            {selecionado.fechamentoAutomatico && <Alert severity="warning">{AVISO_LANCAMENTO_PENDENTE}</Alert>}
            <Alert severity="warning">
              <strong>Justificativa:</strong> {selecionado.justificativa}
            </Alert>
            {podeEditarNaoRealizado && (
              <>
                <TextField
                  fullWidth
                  required
                  multiline
                  minRows={3}
                  label="Editar justificativa"
                  value={justificativa}
                  onChange={(e) => setJustificativa(e.target.value.slice(0, 500))}
                  helperText={`${justificativa.length}/500 caracteres`}
                  inputProps={{ maxLength: 500 }}
                />
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} flexWrap="wrap" useFlexGap>
                  <Button
                    variant="contained"
                    disabled={salvando || !justificativa.trim()}
                    onClick={() => void salvarJustificativa()}
                  >
                    Salvar justificativa
                  </Button>
                  {podeAdministrar && (
                    <Button
                      variant="outlined"
                      color="success"
                      disabled={salvando}
                      onClick={() => void corrigirParaRealizado()}
                    >
                      {selecionado.fechamentoAutomatico
                        ? 'Modificar frequência'
                        : 'Corrigir: houve discipulado nesta data'}
                    </Button>
                  )}
                  {podeAdministrar && (
                    <Button
                      variant="outlined"
                      color="error"
                      startIcon={<DeleteOutlineRounded />}
                      disabled={salvando}
                      onClick={() => setConfirmandoExclusao(true)}
                    >
                      Excluir frequência
                    </Button>
                  )}
                </Stack>
              </>
            )}
          </Stack>
        </SectionCard>
      )}

      {selecionado?.situacao === 'REALIZADO' && (
        <SectionCard
          title={`Frequência de ${dataFormatada}`}
          description={
            <>
              {editavel ? 'Marque quem esteve presente.' : 'Consulta em modo somente leitura.'}
              {(selecionado.atualizadoEm || selecionado.chamadaSalvaEm) && (
                <>
                  {' '}
                  Registrado/alterado em {formatarDataHora(selecionado.atualizadoEm ?? selecionado.chamadaSalvaEm!)}.
                </>
              )}
            </>
          }
          icon={<CheckRounded />}
        >
          <Stack spacing={2.5}>
            {selecionado.fechamentoAutomatico && <Alert severity="warning">{AVISO_LANCAMENTO_PENDENTE}</Alert>}
            {discipulado && (
              <DiscipuladoLiderancaInfo
                discipuladorNome={discipulado.discipuladorNome}
                coLideres={discipulado.coLideres}
                ocultarCoLideres={listagemSimples}
              />
            )}
            {!editavel && (
              <Alert severity="info">
                {selecionado.chamadaSalvaEm
                  ? 'Frequência em modo somente leitura: a janela de três horas foi encerrada.'
                  : 'Frequência em modo somente leitura: o prazo de lançamento desta sexta encerrou no domingo subsequente.'}
              </Alert>
            )}
            {podeAdministrar && (
              <Button
                variant="outlined"
                color="error"
                startIcon={<DeleteOutlineRounded />}
                disabled={salvando}
                onClick={() => setConfirmandoExclusao(true)}
                sx={{ alignSelf: 'flex-start' }}
              >
                Excluir frequência
              </Button>
            )}

            <TextField
              label="Observação"
              placeholder="Ex.: Foi colocada a frequência, mas o menino só chegou na hora do culto"
              value={observacao}
              onChange={(e) => {
                setObservacao(e.target.value)
                setAlterado(true)
              }}
              multiline
              minRows={2}
              fullWidth
              disabled={!editavel}
              inputProps={{ maxLength: 500 }}
              helperText={`${observacao.length}/500 caracteres`}
            />

            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              alignItems={{ sm: 'center' }}
              justifyContent="space-between"
              gap={1.5}
            >
              <Typography variant="body2" color="text.secondary">
                {presentes} presentes · {ausentes} ausentes
              </Typography>
              {editavel && discipulos.length > 0 && (
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ width: { xs: '100%', sm: 'auto' } }}>
                  <Button
                    size="small"
                    variant="outlined"
                    color="success"
                    startIcon={<CheckRounded />}
                    onClick={() => definirTodos('PRESENTE')}
                    sx={{ width: { xs: '100%', sm: 'auto' } }}
                  >
                    Todos presentes
                  </Button>
                  <Button
                    size="small"
                    variant="outlined"
                    color="error"
                    startIcon={<CloseRounded />}
                    onClick={() => definirTodos('AUSENTE')}
                    sx={{ width: { xs: '100%', sm: 'auto' } }}
                  >
                    Todos ausentes
                  </Button>
                </Stack>
              )}
            </Stack>

            {discipulos.length ? (
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', md: 'repeat(2,minmax(0,1fr))' },
                  gap: 1,
                  overflowX: 'hidden',
                  pb: { xs: listagemSimples && editavel ? 14 : 0, sm: 0 },
                }}
              >
                {discipulos.map((a) => (
                  <CartaoFrequencia
                    key={a.id}
                    adolescente={a}
                    presente={chamada[a.id] === 'PRESENTE'}
                    editavel={editavel}
                    opcional={false}
                    ocultarCategoria={listagemSimples}
                    onToggle={() => definirSituacao(a.id, chamada[a.id] === 'PRESENTE' ? 'AUSENTE' : 'PRESENTE')}
                  />
                ))}
              </Box>
            ) : (
              <EmptyState
                title="Nenhum discípulo vinculado"
                description={
                  listagemSimples
                    ? 'Cadastre os discípulos deste grupo de formação para registrar a presença.'
                    : 'Marque GOE ou visitantes abaixo, ou adicione um visitante para registrar a primeira presença.'
                }
              />
            )}

            {!listagemSimples && (
              <Stack spacing={1.25} sx={{ pb: { xs: editavel ? 14 : 0, sm: 0 } }}>
                <Stack direction="row" alignItems="center" gap={1}>
                  <GroupsRounded color="action" />
                  <Typography variant="subtitle2" fontWeight={700}>
                    GOE e visitantes
                  </Typography>
                </Stack>
                <Typography variant="body2" color="text.secondary">
                  Marque a presença somente quando esses adolescentes comparecerem. Quem não vier não entra como falta.
                </Typography>
                {editavel && (
                  <Button
                    variant="text"
                    startIcon={<PersonAddAltRounded />}
                    onClick={() => {
                      setVisitante({ ...visitanteVazio, consentimentoEm: hoje() })
                      setFamiliaVisitante(familiaNaoConsta())
                      setErro('')
                    }}
                    sx={{ alignSelf: 'flex-start' }}
                  >
                    Adicionar visitante
                  </Button>
                )}
                {opcionais.length ? (
                  <Box
                    sx={{
                      display: 'grid',
                      gridTemplateColumns: { xs: '1fr', md: 'repeat(2,minmax(0,1fr))' },
                      gap: 1,
                      overflowX: 'hidden',
                    }}
                  >
                    {opcionais.map((a) => (
                      <CartaoFrequencia
                        key={a.id}
                        adolescente={a}
                        presente={chamada[a.id] === 'PRESENTE'}
                        editavel={editavel}
                        opcional
                        onToggle={() => definirSituacao(a.id, chamada[a.id] === 'PRESENTE' ? null : 'PRESENTE')}
                      />
                    ))}
                  </Box>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    Nenhum GOE ou visitante cadastrado neste discipulado.
                  </Typography>
                )}
              </Stack>
            )}

            {editavel && (
              <Paper
                variant="outlined"
                sx={{
                  p: 1.5,
                  position: { xs: 'sticky', sm: 'static' },
                  bottom: { xs: BOTTOM_NAV_OFFSET, sm: 'auto' },
                  zIndex: 3,
                  bgcolor: 'background.paper',
                  boxShadow: { xs: '0 8px 24px rgba(23,32,51,.16)', sm: 'none' },
                }}
              >
                <Stack
                  direction={{ xs: 'column', sm: 'row' }}
                  alignItems={{ sm: 'center' }}
                  justifyContent="space-between"
                  gap={1.5}
                >
                  <Typography variant="body2" color={alterado ? 'warning.dark' : 'text.secondary'}>
                    {salvando ? 'Salvando...' : alterado ? 'Há alterações ainda não salvas.' : 'Frequência atualizada.'}
                  </Typography>
                  <Button
                    variant="contained"
                    startIcon={<SaveRounded />}
                    disabled={salvando || !alterado}
                    onClick={() => void salvarFrequencia()}
                    sx={{ width: { xs: '100%', sm: 'auto' } }}
                  >
                    Salvar frequência
                  </Button>
                </Stack>
              </Paper>
            )}
          </Stack>
        </SectionCard>
      )}

      <Dialog
        open={Boolean(visitante)}
        onClose={() => {
          if (!salvando) {
            setVisitante(undefined)
            setFamiliaVisitante(familiaNaoConsta())
          }
        }}
        fullWidth
        maxWidth="md"
        fullScreen={mobile}
        PaperProps={{
          component: 'form',
          onSubmit: (e: FormEvent) => {
            e.preventDefault()
            void adicionarVisitante()
          },
        }}
      >
        <DialogTitle>Adicionar visitante</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            O visitante será cadastrado como adolescente do discipulado e marcado como presente nesta data.
          </Typography>
          <Stack spacing={2.25}>
            {visitante && (
              <AdolescenteFormFields
                value={visitante}
                onChange={(patch) => setVisitante((atual) => ({ ...(atual ?? visitanteVazio), ...patch }))}
                disabled={salvando}
              />
            )}
            {podeFamilia && (
              <FamilyFormFields value={familiaVisitante} onChange={setFamiliaVisitante} disabled={salvando} />
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setVisitante(undefined)
              setFamiliaVisitante(familiaNaoConsta())
            }}
            disabled={salvando}
          >
            Cancelar
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={salvando || !visitante?.nome.trim() || !visitante?.dataNascimento || !visitante?.consentimentoEm}
          >
            Adicionar
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={confirmandoExclusao}
        onClose={() => !salvando && setConfirmandoExclusao(false)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>Excluir frequência?</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary">
            Isso remove o encontro de {selecionado ? formatarData(selecionado.data) : dataFormatada}
            {discipulado ? ` do discipulado ${discipulado.nome}` : ''}, incluindo a chamada e os visitantes. A data fica
            livre para um novo lançamento. A exclusão não desfaz promoção automática de visitante a discípulo.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmandoExclusao(false)} disabled={salvando}>
            Cancelar
          </Button>
          <Button color="error" variant="contained" disabled={salvando} onClick={() => void excluirFrequencia()}>
            Excluir
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}

function CartaoFrequencia({
  adolescente,
  presente,
  editavel,
  opcional,
  ocultarCategoria = false,
  onToggle,
}: {
  adolescente: ParticipanteChamada
  presente: boolean
  editavel: boolean
  opcional: boolean
  ocultarCategoria?: boolean
  onToggle: () => void
}) {
  const rotulo = ocultarCategoria ? undefined : rotuloCategoria(adolescente.categoria)
  const estado = presente ? 'presente' : opcional ? 'não marcado' : 'ausente'
  const alternar = () => {
    if (editavel) onToggle()
  }
  return (
    <Paper
      variant="outlined"
      onClick={alternar}
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 1.25,
        p: 1.5,
        minHeight: 56,
        overflow: 'hidden',
        cursor: editavel ? 'pointer' : 'default',
        borderColor: presente ? 'success.light' : 'divider',
        bgcolor: presente ? (theme) => alpha(theme.palette.success.main, 0.08) : 'background.paper',
        WebkitTapHighlightColor: 'transparent',
      }}
    >
      <Avatar
        sx={{
          width: 40,
          height: 40,
          fontSize: 14,
          fontWeight: 700,
          bgcolor: presente ? 'success.main' : 'grey.500',
          flexShrink: 0,
        }}
      >
        {iniciais(adolescente.nome)}
      </Avatar>
      <Box minWidth={0} flexGrow={1}>
        <Typography variant="body2" fontWeight={700} noWrap>
          {adolescente.nome}
        </Typography>
        <Stack direction="row" alignItems="center" gap={0.75} flexWrap="wrap">
          {rotulo && <Chip size="small" label={rotulo} variant="outlined" />}
          {adolescente.registroAnterior && (
            <Typography variant="caption" color="text.secondary">
              Registro anterior
            </Typography>
          )}
        </Stack>
      </Box>
      <Button
        size="small"
        variant={presente ? 'contained' : 'outlined'}
        color={presente ? 'success' : opcional ? 'inherit' : 'error'}
        startIcon={presente ? <CheckRounded /> : opcional ? undefined : <CloseRounded />}
        disabled={!editavel}
        aria-pressed={presente}
        aria-label={`${adolescente.nome}: ${estado}. Clique para alterar.`}
        onClick={(event) => {
          event.stopPropagation()
          alternar()
        }}
        sx={{
          minWidth: { xs: 112, sm: 118 },
          flexShrink: 0,
          pointerEvents: editavel ? 'auto' : 'none',
        }}
      >
        {presente ? 'Presente' : opcional ? 'Marcar' : 'Ausente'}
      </Button>
    </Paper>
  )
}

function presencaOpcional(categoria?: CategoriaAdolescente, listagemSimples = false) {
  if (listagemSimples) return false
  return categoria === 'VISITANTE' || categoria === 'DISCIPULO_GOE'
}

function rotuloCategoria(categoria?: CategoriaAdolescente) {
  if (categoria === 'DISCIPULO_GOE') return 'GOE'
  if (categoria === 'VISITANTE') return 'Visitante'
  return undefined
}

function iniciais(nome: string) {
  const partes = nome.trim().split(/\s+/).filter(Boolean)
  return `${partes[0]?.[0] ?? ''}${partes.length > 1 ? (partes[partes.length - 1]?.[0] ?? '') : ''}`.toLocaleUpperCase(
    'pt-BR',
  )
}
function mensagem(e: unknown) {
  return e instanceof ApiError ? e.message : 'Não foi possível concluir a operação.'
}
function formatarData(data: string) {
  return new Intl.DateTimeFormat('pt-BR').format(new Date(`${data}T12:00:00`))
}
function formatarDataHora(data: string) {
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: 'America/Sao_Paulo',
  }).format(new Date(data))
}
