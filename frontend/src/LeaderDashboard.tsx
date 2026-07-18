import { HowToRegRounded, PersonOffRounded, PersonRounded } from '@mui/icons-material'
import { Alert, Autocomplete, Box, Chip, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField } from '@mui/material'
import { useEffect, useState } from 'react'
import ReactECharts from 'echarts-for-react'
import { FiltroPeriodo, KpisPresenca, PainelEvolucao } from './dashboardComponents'
import { formatarMes, normalizarMeses, percentual, periodoPadrao } from './dashboardUtils'
import { painelApi, type DiscipuloPainel, type PainelLiderResponse } from './painelApi'
import { AnalyticsCard, EmptyState, KpiCard, LoadingState, PageHeader, SectionCard } from './ui'

export default function LeaderDashboard() {
  const inicial = periodoPadrao()
  const [dataInicio, setDataInicio] = useState(inicial.inicio)
  const [dataFim, setDataFim] = useState(inicial.fim)
  const [periodo, setPeriodo] = useState(inicial)
  const [dados, setDados] = useState<PainelLiderResponse>()
  const [discipuloId, setDiscipuloId] = useState<number>()
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)
  useEffect(() => { let ativo = true; setCarregando(true); setErro(''); painelApi.consultarLider(periodo.inicio, periodo.fim).then((resposta) => { if (ativo) { setDados(resposta); setDiscipuloId((atual) => resposta.discipulos.some((item) => item.adolescenteId === atual) ? atual : resposta.discipulos[0]?.adolescenteId) } }).catch((error: Error) => { if (ativo) { setDados(undefined); setDiscipuloId(undefined); setErro(error.message) } }).finally(() => { if (ativo) setCarregando(false) }); return () => { ativo = false } }, [periodo])
  const meses = dados ? normalizarMeses(dados.dataInicio, dados.dataFim, dados.evolucao) : []
  const discipulo = dados?.discipulos.find((item) => item.adolescenteId === discipuloId)
  return <Stack spacing={3}>
    <PageHeader title="Meu discipulado" description={dados?.discipulado.nome ?? 'Histórico de presença do grupo em que você exerce liderança.'} eyebrow="Visão da liderança" action={dados && !dados.discipulado.ativo ? <Chip color="warning" label="Inativo" size="small" /> : undefined} />
    <FiltroPeriodo dataInicio={dataInicio} dataFim={dataFim} onInicio={setDataInicio} onFim={setDataFim} onAplicar={() => setPeriodo({ inicio: dataInicio, fim: dataFim })} />
    {carregando && <LoadingState label="Carregando histórico..." />}
    {erro && <Alert severity="error">{erro}</Alert>}
    {!carregando && dados && <><KpisPresenca resumo={dados.resumo} />{dados.resumo.encontrosRealizados === 0 && <Alert severity="info">Não há encontros realizados no período selecionado.</Alert>}<PainelEvolucao titulo="Evolução mensal do discipulado" tabelaTitulo="Histórico mensal do discipulado" dados={meses} /><PainelIndividual dados={dados} selecionado={discipulo} onSelecionar={setDiscipuloId} /></>}
  </Stack>
}

function PainelIndividual({ dados, selecionado, onSelecionar }: { dados: PainelLiderResponse; selecionado?: DiscipuloPainel; onSelecionar: (id: number | undefined) => void }) {
  if (dados.discipulos.length === 0) return <SectionCard title="Progresso individual de presença"><EmptyState title="Nenhum discípulo no período" description="Não há discípulos vinculados ou com frequência registrada no intervalo selecionado." /></SectionCard>
  const meses = normalizarDiscipulo(dados.dataInicio, dados.dataFim, selecionado?.evolucao ?? [])
  return <Stack spacing={2}>
    <SectionCard title="Progresso individual de presença" description="Pesquise e selecione um discípulo para acompanhar sua presença no intervalo aplicado.">
      <Autocomplete fullWidth options={dados.discipulos} value={selecionado ?? null} onChange={(_, valor) => onSelecionar(valor?.adolescenteId)} getOptionLabel={(item) => item.nome} isOptionEqualToValue={(option, value) => option.adolescenteId === value.adolescenteId} renderInput={(params) => <TextField {...params} label="Discípulo" placeholder="Pesquisar discípulo" />} noOptionsText="Nenhum discípulo encontrado" />
    </SectionCard>
    {selecionado && <><Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, minmax(0, 1fr))' } }}><KpiCard label="Presentes" value={selecionado.presentes} icon={<PersonRounded />} tone="success" /><KpiCard label="Ausentes" value={selecionado.ausentes} icon={<PersonOffRounded />} tone="error" /><KpiCard label="Presença" value={selecionado.percentualPresenca == null ? '—' : percentual(selecionado.percentualPresenca)} icon={<HowToRegRounded />} tone="primary" /></Box>{selecionado.evolucao.length === 0 && <Alert severity="info">Não há registros de frequência para {selecionado.nome} no período selecionado.</Alert>}<AnalyticsCard title={`Presença mensal de ${selecionado.nome}`} description="Percentual calculado apenas com registros de presença e ausência do discípulo." chart={<GraficoIndividual nome={selecionado.nome} dados={meses} />} table={<TabelaIndividual nome={selecionado.nome} dados={meses} />} /></>}
  </Stack>
}

type MesDiscipulo = { referencia: string; presentes: number; ausentes: number; percentualPresenca: number; possuiRegistro: boolean }
function normalizarDiscipulo(inicio: string, fim: string, evolucao: DiscipuloPainel['evolucao']): MesDiscipulo[] {
  const base = normalizarMeses(inicio, fim, evolucao.map((item) => ({ ...item, visitantes: 0 })))
  const referencias = new Set(evolucao.map((item) => item.referencia))
  return base.map((item) => ({ referencia: item.referencia, presentes: item.presentes, ausentes: item.ausentes, percentualPresenca: item.percentualPresenca, possuiRegistro: referencias.has(item.referencia) }))
}

function GraficoIndividual({ nome, dados }: { nome: string; dados: MesDiscipulo[] }) {
  return <Box role="img" aria-label={`Gráfico do percentual mensal de presença de ${nome}.`}><ReactECharts style={{ height: 320 }} option={{ aria: { enabled: true }, tooltip: { trigger: 'axis', valueFormatter: (valor: number) => percentual(valor) }, grid: { left: 55, right: 35, bottom: 45, containLabel: true }, xAxis: { type: 'category', data: dados.map((item) => formatarMes(item.referencia)) }, yAxis: { type: 'value', min: 0, max: 100, axisLabel: { formatter: '{value}%' } }, series: [{ name: 'Presença', type: 'line', connectNulls: false, symbolSize: 9, data: dados.map((item) => item.possuiRegistro ? item.percentualPresenca : null), lineStyle: { width: 3, color: '#3451B2' }, itemStyle: { color: '#3451B2' } }] }} /></Box>
}

function TabelaIndividual({ nome, dados }: { nome: string; dados: MesDiscipulo[] }) {
  return <TableContainer><Table size="small" aria-label={`Histórico mensal de presença de ${nome}`}><TableHead><TableRow><TableCell scope="col">Mês</TableCell><TableCell scope="col">Presentes</TableCell><TableCell scope="col">Ausentes</TableCell><TableCell scope="col">Presença</TableCell></TableRow></TableHead><TableBody>{dados.map((item) => <TableRow key={item.referencia}><TableCell component="th" scope="row">{formatarMes(item.referencia)}</TableCell>{item.possuiRegistro ? <><TableCell>{item.presentes}</TableCell><TableCell>{item.ausentes}</TableCell><TableCell>{percentual(item.percentualPresenca)}</TableCell></> : <TableCell colSpan={3} sx={{ color: 'text.secondary' }}>Sem registros de frequência</TableCell>}</TableRow>)}</TableBody></Table></TableContainer>
}
