import { Box, Button, Paper, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Typography } from '@mui/material'
import ReactECharts from 'echarts-for-react'
import type { EvolucaoMensal, ResumoPainel } from './painelApi'
import { formatarMes, percentual, type MesVisual } from './dashboardUtils'

export function FiltroPeriodo({ dataInicio, dataFim, onInicio, onFim, onAplicar }: { dataInicio: string; dataFim: string; onInicio: (valor: string) => void; onFim: (valor: string) => void; onAplicar: () => void }) {
  const invalido = !dataInicio || !dataFim || dataInicio > dataFim
  return <Paper component="form" onSubmit={(event) => { event.preventDefault(); if (!invalido) onAplicar() }} sx={{ p: 2 }}>
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
      <TextField required label="Data inicial" type="date" value={dataInicio} onChange={(event) => onInicio(event.target.value)} slotProps={{ inputLabel: { shrink: true } }} error={Boolean(dataInicio && dataFim && dataInicio > dataFim)} />
      <TextField required label="Data final" type="date" value={dataFim} onChange={(event) => onFim(event.target.value)} slotProps={{ inputLabel: { shrink: true } }} error={Boolean(dataInicio && dataFim && dataInicio > dataFim)} helperText={dataInicio && dataFim && dataInicio > dataFim ? 'A data final deve ser posterior à inicial.' : undefined} />
      <Button type="submit" variant="contained" disabled={invalido}>Aplicar</Button>
    </Stack>
  </Paper>
}

export function KpisPresenca({ resumo }: { resumo: ResumoPainel }) {
  const itens: Array<[string, string | number]> = [['Presentes', resumo.presentes], ['Ausentes', resumo.ausentes], ['Visitantes', resumo.visitantes], ['Encontros', resumo.encontrosRealizados], ['Presença', percentual(resumo.percentualPresenca)]]
  return <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: 'repeat(2, minmax(0, 1fr))', md: 'repeat(5, minmax(0, 1fr))' } }}>
    {itens.map(([nome, valor]) => <Paper key={nome} sx={{ p: 2 }}><Typography color="text.secondary">{nome}</Typography><Typography variant="h5">{valor}</Typography></Paper>)}
  </Box>
}

export function GraficoEvolucao({ titulo, dados }: { titulo: string; dados: MesVisual[] }) {
  const labels = dados.map((item) => formatarMes(item.referencia))
  const valor = (item: MesVisual, campo: keyof Pick<EvolucaoMensal, 'presentes' | 'ausentes' | 'visitantes' | 'percentualPresenca'>) => item.possuiEncontro ? item[campo] : null
  return <Paper sx={{ p: 2 }}><Typography variant="h6">{titulo}</Typography><Typography color="text.secondary" variant="body2">Volumes no eixo esquerdo e presença no eixo direito. Visitantes não entram no cálculo da presença.</Typography>
    <Box role="img" aria-label={`${titulo}. Gráfico combinado de presentes, ausentes, visitantes e percentual de presença por mês.`}>
      <ReactECharts style={{ height: 360 }} option={{ aria: { enabled: true }, tooltip: { trigger: 'axis' }, legend: { top: 8, data: ['Presentes', 'Ausentes', 'Visitantes', 'Presença'] }, grid: { top: 70, left: 55, right: 60, bottom: 45 }, xAxis: { type: 'category', data: labels }, yAxis: [{ type: 'value', name: 'Pessoas', minInterval: 1 }, { type: 'value', name: 'Presença', min: 0, max: 100, axisLabel: { formatter: '{value}%' } }], series: [{ name: 'Presentes', type: 'bar', data: dados.map((i) => valor(i, 'presentes')), itemStyle: { color: '#2e7d32' } }, { name: 'Ausentes', type: 'bar', data: dados.map((i) => valor(i, 'ausentes')), itemStyle: { color: '#c62828' } }, { name: 'Visitantes', type: 'bar', data: dados.map((i) => valor(i, 'visitantes')), itemStyle: { color: '#6a1b9a' } }, { name: 'Presença', type: 'line', yAxisIndex: 1, symbol: 'diamond', symbolSize: 9, data: dados.map((i) => valor(i, 'percentualPresenca')), lineStyle: { width: 3, type: 'dashed', color: '#1565c0' }, itemStyle: { color: '#1565c0' } }] }} notMerge />
    </Box>
  </Paper>
}

export function TabelaEvolucao({ titulo, dados }: { titulo: string; dados: MesVisual[] }) {
  return <TableContainer component={Paper}><Table size="small"><caption style={{ captionSide: 'top', textAlign: 'left', padding: 16, fontSize: '1.25rem', fontWeight: 500, color: 'inherit' }}>{titulo}</caption><TableHead><TableRow><TableCell scope="col">Mês</TableCell><TableCell scope="col">Presentes</TableCell><TableCell scope="col">Ausentes</TableCell><TableCell scope="col">Visitantes</TableCell><TableCell scope="col">Presença</TableCell></TableRow></TableHead><TableBody>
    {dados.map((item) => <TableRow key={item.referencia}><TableCell component="th" scope="row">{formatarMes(item.referencia)}</TableCell>{item.possuiEncontro ? <><TableCell>{item.presentes}</TableCell><TableCell>{item.ausentes}</TableCell><TableCell>{item.visitantes}</TableCell><TableCell>{percentual(item.percentualPresenca)}</TableCell></> : <TableCell colSpan={4} sx={{ color: 'text.secondary' }}>Sem encontros realizados</TableCell>}</TableRow>)}
  </TableBody></Table></TableContainer>
}
