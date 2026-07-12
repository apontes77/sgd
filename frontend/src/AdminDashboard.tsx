import { Alert, Box, Button, CircularProgress, Paper, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Typography } from '@mui/material'
import ReactECharts from 'echarts-for-react'
import { useEffect, useState } from 'react'
import { painelApi, type PainelAdminResponse } from './painelApi'

const iso = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
const periodoPadrao = () => { const fim = new Date(); const mes = fim.getMonth() - 6; const ultimoDia = new Date(fim.getFullYear(), mes + 1, 0).getDate(); const inicio = new Date(fim.getFullYear(), mes, Math.min(fim.getDate(), ultimoDia)); return { inicio: iso(inicio), fim: iso(fim) } }
const percentual = (value: number) => `${value.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}%`

export default function AdminDashboard() {
  const inicial = periodoPadrao()
  const [dataInicio, setDataInicio] = useState(inicial.inicio)
  const [dataFim, setDataFim] = useState(inicial.fim)
  const [periodo, setPeriodo] = useState(inicial)
  const [dados, setDados] = useState<PainelAdminResponse>()
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)
  useEffect(() => { let ativo = true; setCarregando(true); setErro(''); painelApi.consultar(periodo.inicio, periodo.fim).then((response) => { if (ativo) setDados(response) }).catch((error: Error) => { if (ativo) { setDados(undefined); setErro(error.message) } }).finally(() => { if (ativo) setCarregando(false) }); return () => { ativo = false } }, [periodo])
  return <Stack spacing={3}>
    <Box><Typography variant="h4">Painel administrativo</Typography><Typography color="text.secondary">Visão consolidada da frequência.</Typography></Box>
    <Paper component="form" onSubmit={(event) => { event.preventDefault(); setPeriodo({ inicio: dataInicio, fim: dataFim }) }} sx={{ p: 2 }}><Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}><TextField label="Data inicial" type="date" value={dataInicio} onChange={(event) => setDataInicio(event.target.value)} slotProps={{ inputLabel: { shrink: true } }} required /><TextField label="Data final" type="date" value={dataFim} onChange={(event) => setDataFim(event.target.value)} slotProps={{ inputLabel: { shrink: true } }} required /><Button type="submit" variant="contained">Aplicar</Button></Stack></Paper>
    {carregando && <Box role="status" sx={{ py: 8, textAlign: 'center' }}><CircularProgress /><Typography>Carregando painel...</Typography></Box>}
    {erro && <Alert severity="error">{erro}</Alert>}
    {!carregando && dados && <Conteudo dados={dados} />}
  </Stack>
}

function Conteudo({ dados }: { dados: PainelAdminResponse }) {
  const vazio = dados.resumo.presentes + dados.resumo.ausentes + dados.resumo.visitantes === 0
  const kpis: Array<[string, string | number]> = [['Presentes', dados.resumo.presentes], ['Ausentes', dados.resumo.ausentes], ['Visitantes', dados.resumo.visitantes], ['Encontros', dados.resumo.encontrosRealizados], ['Presença geral', percentual(dados.resumo.percentualPresenca)]]
  return <Stack spacing={3}>
    <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(5, 1fr)' } }}>{kpis.map(([label, value]) => <Paper key={label} sx={{ p: 2 }}><Typography color="text.secondary">{label}</Typography><Typography variant="h5">{value}</Typography></Paper>)}</Box>
    {vazio && <Alert severity="info">Não há registros de frequência no período selecionado.</Alert>}
    <Grafico titulo="Evolução mensal" option={{ tooltip: { trigger: 'axis' }, legend: { data: ['Presentes', 'Ausentes', 'Visitantes'] }, xAxis: { type: 'category', data: dados.evolucao.map((item) => item.referencia) }, yAxis: { type: 'value' }, series: [{ name: 'Presentes', type: 'line', data: dados.evolucao.map((item) => item.presentes) }, { name: 'Ausentes', type: 'line', data: dados.evolucao.map((item) => item.ausentes) }, { name: 'Visitantes', type: 'line', data: dados.evolucao.map((item) => item.visitantes) }] }} />
    <TabelaEvolucao dados={dados} />
    <Grafico titulo="Presença por gerência" option={{ tooltip: { trigger: 'axis', valueFormatter: (value: number) => percentual(value) }, xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } }, yAxis: { type: 'category', data: dados.gerencias.map((item) => item.nome) }, series: [{ name: 'Presença', type: 'bar', data: dados.gerencias.map((item) => item.percentualPresenca) }] }} />
    <TabelaIndicadores titulo="Resumo por gerência" primeiraColuna="Gerência" linhas={dados.gerencias.map((item) => [item.nome, item.presentes, item.ausentes, percentual(item.percentualPresenca)])} />
    <Grafico titulo="Distribuição por sexo do discipulado" option={{ tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' }, legend: { bottom: 0 }, series: [{ name: 'Presenças', type: 'pie', radius: ['45%', '70%'], data: dados.sexos.map((item) => ({ name: item.sexo === 'MASCULINO' ? 'Masculino' : 'Feminino', value: item.presentes })) }] }} />
    <TabelaIndicadores titulo="Resumo por sexo" primeiraColuna="Sexo" linhas={dados.sexos.map((item) => [item.sexo === 'MASCULINO' ? 'Masculino' : 'Feminino', item.presentes, item.ausentes, percentual(item.percentualPresenca)])} />
  </Stack>
}

function Grafico({ titulo, option }: { titulo: string; option: object }) { return <Paper sx={{ p: 2 }}><Typography variant="h6">{titulo}</Typography><ReactECharts option={option} style={{ height: 340, width: '100%' }} notMerge /></Paper> }
function TabelaEvolucao({ dados }: { dados: PainelAdminResponse }) { return <TableContainer component={Paper}><Typography variant="h6" sx={{ p: 2 }}>Resumo mensal</Typography><Table size="small"><TableHead><TableRow><TableCell>Mês</TableCell><TableCell>Presentes</TableCell><TableCell>Ausentes</TableCell><TableCell>Visitantes</TableCell><TableCell>Presença</TableCell></TableRow></TableHead><TableBody>{dados.evolucao.map((item) => <TableRow key={item.referencia}><TableCell>{item.referencia}</TableCell><TableCell>{item.presentes}</TableCell><TableCell>{item.ausentes}</TableCell><TableCell>{item.visitantes}</TableCell><TableCell>{percentual(item.percentualPresenca)}</TableCell></TableRow>)}</TableBody></Table></TableContainer> }
function TabelaIndicadores({ titulo, primeiraColuna, linhas }: { titulo: string; primeiraColuna: string; linhas: Array<Array<string | number>> }) { return <TableContainer component={Paper}><Typography variant="h6" sx={{ p: 2 }}>{titulo}</Typography><Table size="small"><TableHead><TableRow><TableCell>{primeiraColuna}</TableCell><TableCell>Presentes</TableCell><TableCell>Ausentes</TableCell><TableCell>Presença</TableCell></TableRow></TableHead><TableBody>{linhas.map((linha) => <TableRow key={String(linha[0])}>{linha.map((celula, index) => <TableCell key={index}>{celula}</TableCell>)}</TableRow>)}</TableBody></Table></TableContainer> }
