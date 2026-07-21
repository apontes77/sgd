import {
  CheckRounded, CloseRounded, EventAvailableRounded, EventBusyRounded, EventRounded,
  PersonAddAltRounded, SaveRounded,
} from '@mui/icons-material'
import { type FormEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert, Avatar, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle,
  Paper, Stack, TextField, Typography,
} from '@mui/material'
import { ApiError } from './api'
import { adolescentesApi } from './adolescentesApi'
import { AdolescenteFormFields, type DadosPessoaisAdolescente } from './AdolescenteFormFields'
import { frequenciaApi, type AdolescenteResumo, type Encontro, type SituacaoFrequencia } from './frequenciaApi'
import { EmptyState, SectionCard } from './ui'

interface Props { discipuladoId:number; podeAdministrar?:boolean; podeRegistrarNaoRealizacao?:boolean }
interface ParticipanteChamada extends AdolescenteResumo { registroAnterior:boolean }

const hoje = () => {
  const agora = new Date()
  // Data local do usuário; toISOString() usaria UTC e viraria "amanhã" à noite no Brasil.
  return `${agora.getFullYear()}-${String(agora.getMonth() + 1).padStart(2, '0')}-${String(agora.getDate()).padStart(2, '0')}`
}
const visitanteVazio: DadosPessoaisAdolescente = { nome: '', dataNascimento: '', telefone: '', instagram: '' }

export default function FrequencyManagement({ discipuladoId, podeAdministrar = false, podeRegistrarNaoRealizacao = false }: Props) {
  const [data, setData] = useState(hoje)
  const [selecionado, setSelecionado] = useState<Encontro>()
  const [adolescentesAtuais, setAdolescentesAtuais] = useState<AdolescenteResumo[]>([])
  const [participantes, setParticipantes] = useState<ParticipanteChamada[]>([])
  const [chamada, setChamada] = useState<Record<number, SituacaoFrequencia>>({})
  const [intencao, setIntencao] = useState<'escolha' | 'justificando'>('escolha')
  const [justificativa, setJustificativa] = useState('')
  const [alterado, setAlterado] = useState(false)
  const [carregando, setCarregando] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [visitante, setVisitante] = useState<DadosPessoaisAdolescente>()
  const requisicao = useRef(0)

  const editavel = useMemo(() => Boolean(selecionado && selecionado.situacao === 'REALIZADO'
    && (podeAdministrar || Date.now() <= new Date(selecionado.criadoEm).getTime() + 3 * 60 * 60 * 1000)), [selecionado, podeAdministrar])
  const presentes = useMemo(() => participantes.filter(a => chamada[a.id] === 'PRESENTE').length, [participantes, chamada])
  const ausentes = participantes.length - presentes

  const carregarChamada = useCallback(async (encontro: Encontro, atuais: AdolescenteResumo[]) => {
    const atual = requisicao.current
    const existentes = await frequenciaApi.listarChamada(encontro.id)
    if (atual !== requisicao.current) return
    const idsAtuais = new Set(atuais.map(a => a.id))
    const lista: ParticipanteChamada[] = [
      ...atuais.map(a => ({ ...a, registroAnterior: false })),
      ...existentes.filter(f => !idsAtuais.has(f.adolescenteId)).map(f => ({ id: f.adolescenteId, nome: f.adolescenteNome, registroAnterior: true })),
    ]
    const mapa: Record<number, SituacaoFrequencia> = {}
    lista.forEach(a => mapa[a.id] = 'AUSENTE')
    existentes.forEach(f => mapa[f.adolescenteId] = f.situacao)
    setParticipantes(lista)
    setChamada(mapa)
    setAlterado(false)
  }, [])

  const carregarData = useCallback(async (dataSelecionada: string) => {
    const atual = ++requisicao.current
    setCarregando(true); setErro(''); setSucesso(''); setIntencao('escolha'); setJustificativa('')
    setSelecionado(undefined); setParticipantes([]); setChamada({}); setAlterado(false)
    try {
      const [encontros, pagina] = await Promise.all([
        frequenciaApi.listarEncontros(discipuladoId, dataSelecionada, dataSelecionada),
        frequenciaApi.listarAdolescentes(discipuladoId),
      ])
      if (atual !== requisicao.current) return
      setAdolescentesAtuais(pagina.content)
      const existente = encontros.find(e => e.data === dataSelecionada)
      if (existente) {
        setSelecionado(existente)
        if (existente.situacao === 'REALIZADO') await carregarChamada(existente, pagina.content)
        else setJustificativa(existente.justificativa ?? '')
      }
    } catch (e) {
      if (atual === requisicao.current) setErro(mensagem(e))
    } finally {
      if (atual === requisicao.current) setCarregando(false)
    }
  }, [discipuladoId, carregarChamada])

  useEffect(() => { void carregarData(data) }, [carregarData, data])

  async function houveDiscipulado() {
    setSalvando(true); setErro(''); setSucesso('')
    try {
      const novo = await frequenciaApi.criarEncontro({ discipuladoId, data, situacao: 'REALIZADO' })
      requisicao.current++
      setSelecionado(novo)
      await carregarChamada(novo, adolescentesAtuais)
    } catch (e) { setErro(mensagem(e)) } finally { setSalvando(false) }
  }

  async function confirmarAusencia() {
    const motivo = justificativa.trim()
    if (!motivo) { setErro('Descreva por que não houve discipulado.'); return }
    setSalvando(true); setErro(''); setSucesso('')
    try {
      const novo = await frequenciaApi.criarEncontro({ discipuladoId, data, situacao: 'NAO_REALIZADO', justificativa: motivo })
      requisicao.current++
      setSelecionado(novo); setParticipantes([]); setChamada({}); setIntencao('escolha')
      setSucesso('Registro de ausência confirmado.')
    } catch (e) { setErro(mensagem(e)) } finally { setSalvando(false) }
  }

  async function salvarFrequencia() {
    if (!selecionado) return
    setSalvando(true); setErro(''); setSucesso('')
    try {
      await frequenciaApi.salvarChamada(selecionado.id, participantes.map(a => ({ adolescenteId: a.id, situacao: chamada[a.id] ?? 'AUSENTE' })))
      setAlterado(false)
      setSucesso('Frequência salva.')
    } catch (e) { setErro(mensagem(e)) } finally { setSalvando(false) }
  }

  async function salvarJustificativa() {
    if (!selecionado) return
    const motivo = justificativa.trim()
    if (!motivo) { setErro('A justificativa é obrigatória.'); return }
    setSalvando(true); setErro(''); setSucesso('')
    try {
      const atualizado = await frequenciaApi.atualizarEncontro(selecionado.id, { situacao: 'NAO_REALIZADO', justificativa: motivo })
      setSelecionado(atualizado)
      setSucesso('Justificativa atualizada.')
    } catch (e) { setErro(mensagem(e)) } finally { setSalvando(false) }
  }

  async function corrigirParaRealizado() {
    if (!selecionado) return
    setSalvando(true); setErro(''); setSucesso('')
    try {
      const atualizado = await frequenciaApi.atualizarEncontro(selecionado.id, { situacao: 'REALIZADO' })
      requisicao.current++
      setSelecionado(atualizado)
      await carregarChamada(atualizado, adolescentesAtuais)
      setSucesso('Corrigido: discipulado marcado como realizado.')
    } catch (e) { setErro(mensagem(e)) } finally { setSalvando(false) }
  }

  async function adicionarVisitante() {
    if (!visitante) return
    const nome = visitante.nome.trim()
    if (!nome || !visitante.dataNascimento) { setErro('Informe nome e data de nascimento do visitante.'); return }
    setSalvando(true); setErro('')
    try {
      const criado = await adolescentesApi.criar({
        nome, dataNascimento: visitante.dataNascimento, telefone: visitante.telefone || undefined,
        instagram: visitante.instagram || undefined, discipuladoId, ativo: true, dataInicio: data,
      })
      setParticipantes(atual => [...atual, { id: criado.id, nome: criado.nome, registroAnterior: false }])
      setChamada(atual => ({ ...atual, [criado.id]: 'PRESENTE' }))
      setAlterado(true)
      setVisitante(undefined)
      setSucesso('Visitante adicionado. Salve a frequência para confirmar.')
    } catch (e) { setErro(mensagem(e)) } finally { setSalvando(false) }
  }

  function definirSituacao(id: number, situacao: SituacaoFrequencia) {
    setChamada(atual => ({ ...atual, [id]: situacao })); setAlterado(true)
  }
  function definirTodos(situacao: SituacaoFrequencia) {
    const mapa: Record<number, SituacaoFrequencia> = {}
    participantes.forEach(a => mapa[a.id] = situacao)
    setChamada(mapa); setAlterado(true)
  }

  const dataFormatada = formatarData(data)

  return <Stack spacing={3}>
    {erro && <Alert severity="error" onClose={() => setErro('')}>{erro}</Alert>}
    {sucesso && <Alert severity="success" onClose={() => setSucesso('')}>{sucesso}</Alert>}

    <SectionCard title="Data do discipulado" description="Escolha a data para registrar a frequência." icon={<EventRounded />}>
      <TextField fullWidth label="Data" type="date" value={data} onChange={e => setData(e.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
    </SectionCard>

    {!carregando && !selecionado && intencao === 'escolha' && <SectionCard title={`O que aconteceu em ${dataFormatada}?`} description="Informe se o discipulado ocorreu nesta data.">
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: podeRegistrarNaoRealizacao ? 'repeat(2,1fr)' : '1fr' }, gap: 2 }}>
        <Button size="large" variant="contained" color="success" startIcon={<EventAvailableRounded />} disabled={salvando}
          onClick={() => void houveDiscipulado()} sx={{ py: 2.5, flexDirection: 'column', gap: 0.5, fontSize: '1rem' }}>
          Houve discipulado
          <Typography variant="caption" sx={{ opacity: .85 }}>Registrar a presença dos adolescentes</Typography>
        </Button>
        {podeRegistrarNaoRealizacao && <Button size="large" variant="outlined" color="warning" startIcon={<EventBusyRounded />} disabled={salvando}
          onClick={() => { setIntencao('justificando'); setErro('') }} sx={{ py: 2.5, flexDirection: 'column', gap: 0.5, fontSize: '1rem' }}>
          Não houve discipulado
          <Typography variant="caption" sx={{ opacity: .85 }}>Registrar o motivo da ausência</Typography>
        </Button>}
      </Box>
    </SectionCard>}

    {!carregando && !selecionado && intencao === 'justificando' && <SectionCard title="Por que não houve discipulado?" description="A justificativa é obrigatória.">
      <Stack spacing={2}>
        <TextField fullWidth required autoFocus multiline minRows={3} label="Justificativa" value={justificativa}
          onChange={e => setJustificativa(e.target.value.slice(0, 500))} helperText={`${justificativa.length}/500 caracteres`} inputProps={{ maxLength: 500 }} />
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
          <Button variant="contained" disabled={salvando || !justificativa.trim()} onClick={() => void confirmarAusencia()}>Confirmar</Button>
          <Button color="inherit" disabled={salvando} onClick={() => { setIntencao('escolha'); setJustificativa('') }}>Voltar</Button>
        </Stack>
      </Stack>
    </SectionCard>}

    {selecionado?.situacao === 'NAO_REALIZADO' && <SectionCard title={`Não houve discipulado em ${dataFormatada}`} description="Registro de ausência com justificativa." icon={<EventBusyRounded />}>
      <Stack spacing={2}>
        <Alert severity="warning"><strong>Justificativa:</strong> {selecionado.justificativa}</Alert>
        {podeRegistrarNaoRealizacao && <>
          <TextField fullWidth required multiline minRows={3} label="Editar justificativa" value={justificativa}
            onChange={e => setJustificativa(e.target.value.slice(0, 500))} helperText={`${justificativa.length}/500 caracteres`} inputProps={{ maxLength: 500 }} />
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
            <Button variant="contained" disabled={salvando || !justificativa.trim()} onClick={() => void salvarJustificativa()}>Salvar justificativa</Button>
            {podeAdministrar && <Button variant="outlined" color="success" disabled={salvando} onClick={() => void corrigirParaRealizado()}>Corrigir: houve discipulado nesta data</Button>}
          </Stack>
        </>}
      </Stack>
    </SectionCard>}

    {selecionado?.situacao === 'REALIZADO' && <SectionCard title={`Frequência de ${dataFormatada}`}
      description={editavel ? 'Marque quem esteve presente.' : 'Consulta em modo somente leitura.'} icon={<CheckRounded />}>
      <Stack spacing={2.5}>
        {!editavel && <Alert severity="info">Frequência em modo somente leitura: a janela de três horas foi encerrada.</Alert>}

        <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ sm: 'center' }} justifyContent="space-between" gap={1.5}>
          <Typography variant="body2" color="text.secondary">{presentes} presentes · {ausentes} ausentes · {participantes.length} no total</Typography>
          {editavel && participantes.length > 0 && <Stack direction="row" spacing={1}>
            <Button size="small" variant="outlined" color="success" startIcon={<CheckRounded />} onClick={() => definirTodos('PRESENTE')}>Todos presentes</Button>
            <Button size="small" variant="outlined" color="error" startIcon={<CloseRounded />} onClick={() => definirTodos('AUSENTE')}>Todos ausentes</Button>
          </Stack>}
        </Stack>

        {participantes.length ? <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2,minmax(0,1fr))' }, gap: 1 }}>
          {participantes.map(a => { const presente = chamada[a.id] === 'PRESENTE'; return <Paper key={a.id} variant="outlined" sx={{ display: 'flex', alignItems: 'center', gap: 1.25, p: 1.25, borderColor: presente ? 'success.light' : 'divider', bgcolor: presente ? '#F1F8F2' : 'background.paper' }}>
            <Avatar sx={{ width: 38, height: 38, fontSize: 14, fontWeight: 700, bgcolor: presente ? 'success.main' : 'grey.500' }}>{iniciais(a.nome)}</Avatar>
            <Box minWidth={0} flexGrow={1}><Typography variant="body2" fontWeight={700} noWrap>{a.nome}</Typography>{a.registroAnterior && <Typography variant="caption" color="text.secondary">Registro anterior</Typography>}</Box>
            <Button size="small" variant={presente ? 'contained' : 'outlined'} color={presente ? 'success' : 'error'} startIcon={presente ? <CheckRounded /> : <CloseRounded />} disabled={!editavel} aria-pressed={presente} aria-label={`${a.nome}: ${presente ? 'presente' : 'ausente'}. Clique para alterar.`} onClick={() => definirSituacao(a.id, presente ? 'AUSENTE' : 'PRESENTE')} sx={{ minWidth: { xs: 112, sm: 118 } }}>{presente ? 'Presente' : 'Ausente'}</Button>
          </Paper> })}
        </Box> : <EmptyState title="Nenhum adolescente vinculado" description="Adicione um visitante para registrar a primeira presença." />}

        {editavel && <Button variant="text" startIcon={<PersonAddAltRounded />} onClick={() => { setVisitante(visitanteVazio); setErro('') }} sx={{ alignSelf: 'flex-start' }}>Adicionar visitante</Button>}

        {editavel && <Paper variant="outlined" sx={{ p: 1.5, position: { xs: 'sticky', sm: 'static' }, bottom: { xs: 8, sm: 'auto' }, zIndex: 2, boxShadow: { xs: '0 8px 24px rgba(23,32,51,.16)', sm: 'none' } }}>
          <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ sm: 'center' }} justifyContent="space-between" gap={1.5}>
            <Typography variant="body2" color={alterado ? 'warning.dark' : 'text.secondary'}>{salvando ? 'Salvando...' : alterado ? 'Há alterações ainda não salvas.' : 'Frequência atualizada.'}</Typography>
            <Button variant="contained" startIcon={<SaveRounded />} disabled={salvando || !alterado} onClick={() => void salvarFrequencia()} sx={{ width: { xs: '100%', sm: 'auto' } }}>Salvar frequência</Button>
          </Stack>
        </Paper>}
      </Stack>
    </SectionCard>}

    <Dialog open={Boolean(visitante)} onClose={() => { if (!salvando) setVisitante(undefined) }} fullWidth maxWidth="sm"
      PaperProps={{ component: 'form', onSubmit: (e: FormEvent) => { e.preventDefault(); void adicionarVisitante() } }}>
      <DialogTitle>Adicionar visitante</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>O visitante será cadastrado como adolescente do discipulado e marcado como presente nesta data.</Typography>
        <Stack spacing={2.25}>
          {visitante && <AdolescenteFormFields value={visitante} onChange={patch => setVisitante(atual => ({ ...(atual ?? visitanteVazio), ...patch }))} disabled={salvando} />}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={() => setVisitante(undefined)} disabled={salvando}>Cancelar</Button>
        <Button type="submit" variant="contained" disabled={salvando || !visitante?.nome.trim() || !visitante?.dataNascimento}>Adicionar</Button>
      </DialogActions>
    </Dialog>
  </Stack>
}

function iniciais(nome: string) { const partes = nome.trim().split(/\s+/).filter(Boolean); return `${partes[0]?.[0] ?? ''}${partes.length > 1 ? partes[partes.length - 1]?.[0] ?? '' : ''}`.toLocaleUpperCase('pt-BR') }
function mensagem(e: unknown) { return e instanceof ApiError ? e.message : 'Não foi possível concluir a operação.' }
function formatarData(data: string) { return new Intl.DateTimeFormat('pt-BR').format(new Date(`${data}T12:00:00`)) }
