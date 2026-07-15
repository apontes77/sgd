import { EventAvailableRounded, FilterAltRounded, GroupsRounded, HowToRegRounded, PersonOffRounded, PersonRounded } from '@mui/icons-material'
import { Box, Button, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField } from '@mui/material'
import ReactECharts from 'echarts-for-react'
import type { EvolucaoMensal, ResumoPainel } from './painelApi'
import { formatarMes, percentual, type MesVisual } from './dashboardUtils'
import { AnalyticsCard, FilterToolbar, KpiCard } from './ui'

export function FiltroPeriodo({ dataInicio, dataFim, onInicio, onFim, onAplicar }: { dataInicio: string; dataFim: string; onInicio: (valor: string) => void; onFim: (valor: string) => void; onAplicar: () => void }) {
  const invalido = !dataInicio || !dataFim || dataInicio > dataFim
  return <FilterToolbar component="form" onSubmit={(event) => { event.preventDefault(); if (!invalido) onAplicar() }}>
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ sm: 'center' }}>
      <TextField required label="Data inicial" type="date" value={dataInicio} onChange={(event) => onInicio(event.target.value)} slotProps={{ inputLabel: { shrink: true } }} error={Boolean(dataInicio && dataFim && dataInicio > dataFim)} />
      <TextField required label="Data final" type="date" value={dataFim} onChange={(event) => onFim(event.target.value)} slotProps={{ inputLabel: { shrink: true } }} error={Boolean(dataInicio && dataFim && dataInicio > dataFim)} helperText={dataInicio && dataFim && dataInicio > dataFim ? 'A data final deve ser posterior à inicial.' : undefined} />
      <Button type="submit" variant="contained" disabled={invalido} startIcon={<FilterAltRounded />}>Aplicar</Button>
    </Stack>
  </FilterToolbar>
}

export function KpisPresenca({ resumo }: { resumo: ResumoPainel }) {
  const itens = [
    { nome: 'Presentes', valor: resumo.presentes, icon: <PersonRounded />, tone: 'success' as const },
    { nome: 'Ausentes', valor: resumo.ausentes, icon: <PersonOffRounded />, tone: 'error' as const },
    { nome: 'Visitantes', valor: resumo.visitantes, icon: <GroupsRounded />, tone: 'secondary' as const },
    { nome: 'Encontros', valor: resumo.encontrosRealizados, icon: <EventAvailableRounded />, tone: 'primary' as const },
    { nome: 'Presença', valor: percentual(resumo.percentualPresenca), icon: <HowToRegRounded />, tone: 'warning' as const },
  ]
  return <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: 'repeat(2, minmax(0, 1fr))', sm: 'repeat(3, minmax(0, 1fr))', xl: 'repeat(5, minmax(0, 1fr))' } }}>
    {itens.map((item) => <KpiCard key={item.nome} label={item.nome} value={item.valor} icon={item.icon} tone={item.tone} />)}
  </Box>
}

export function PainelEvolucao({ titulo, tabelaTitulo, dados }: { titulo: string; tabelaTitulo: string; dados: MesVisual[] }) {
  return <AnalyticsCard title={titulo} description="Volumes no eixo esquerdo e presença no eixo direito. Visitantes não entram no cálculo da presença." chart={<GraficoEvolucao titulo={titulo} dados={dados} />} table={<TabelaEvolucao titulo={tabelaTitulo} dados={dados} />} />
}

export function GraficoEvolucao({ titulo, dados }: { titulo: string; dados: MesVisual[] }) {
  const labels = dados.map((item) => formatarMes(item.referencia))
  const valor = (item: MesVisual, campo: keyof Pick<EvolucaoMensal, 'presentes' | 'ausentes' | 'visitantes' | 'percentualPresenca'>) => item.possuiEncontro ? item[campo] : null
  return <Box role="img" aria-label={`${titulo}. Gráfico combinado de presentes, ausentes, visitantes e percentual de presença por mês.`}>
    <ReactECharts style={{ height: 360 }} option={{ aria: { enabled: true }, tooltip: { trigger: 'axis' }, legend: { top: 8, data: ['Presentes', 'Ausentes', 'Visitantes', 'Presença'] }, grid: { top: 70, left: 55, right: 60, bottom: 45, containLabel: true }, xAxis: { type: 'category', data: labels }, yAxis: [{ type: 'value', name: 'Pessoas', minInterval: 1 }, { type: 'value', name: 'Presença', min: 0, max: 100, axisLabel: { formatter: '{value}%' } }], series: [{ name: 'Presentes', type: 'bar', data: dados.map((i) => valor(i, 'presentes')), itemStyle: { color: '#2E7D32', borderRadius: [4, 4, 0, 0] } }, { name: 'Ausentes', type: 'bar', data: dados.map((i) => valor(i, 'ausentes')), itemStyle: { color: '#C62828', borderRadius: [4, 4, 0, 0] } }, { name: 'Visitantes', type: 'bar', data: dados.map((i) => valor(i, 'visitantes')), itemStyle: { color: '#0F8B8D', borderRadius: [4, 4, 0, 0] } }, { name: 'Presença', type: 'line', yAxisIndex: 1, symbol: 'circle', symbolSize: 8, data: dados.map((i) => valor(i, 'percentualPresenca')), lineStyle: { width: 3, color: '#3451B2' }, itemStyle: { color: '#3451B2' } }] }} notMerge />
  </Box>
}

export function TabelaEvolucao({ titulo, dados }: { titulo: string; dados: MesVisual[] }) {
  return <TableContainer><Table size="small" aria-label={titulo}><caption style={{ textAlign: 'left', paddingBottom: 12, color: '#667085' }}>{titulo}</caption><TableHead><TableRow><TableCell scope="col">Mês</TableCell><TableCell scope="col">Presentes</TableCell><TableCell scope="col">Ausentes</TableCell><TableCell scope="col">Visitantes</TableCell><TableCell scope="col">Presença</TableCell></TableRow></TableHead><TableBody>
    {dados.map((item) => <TableRow key={item.referencia}><TableCell component="th" scope="row">{formatarMes(item.referencia)}</TableCell>{item.possuiEncontro ? <><TableCell>{item.presentes}</TableCell><TableCell>{item.ausentes}</TableCell><TableCell>{item.visitantes}</TableCell><TableCell>{percentual(item.percentualPresenca)}</TableCell></> : <TableCell colSpan={4} sx={{ color: 'text.secondary' }}>Sem encontros realizados</TableCell>}</TableRow>)}
  </TableBody></Table></TableContainer>
}
