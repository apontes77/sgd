import { Alert, Box, Button, Chip, FormControl, InputLabel, MenuItem, Select, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material'
import ReactECharts from 'echarts-for-react'
import { useEffect, useMemo, useState } from 'react'
import { FiltroPeriodo, KpisPresenca, PainelEvolucao } from './dashboardComponents'
import { normalizarMeses, percentual, periodoPadrao } from './dashboardUtils'
import { painelApi, type DiscipuladoPainel, type PainelGerenciaResponse } from './painelApi'
import { AnalyticsCard, LoadingState, PageHeader, SectionCard } from './ui'

const REFERENCIA_INFORMATIVA = 70

export default function ManagerDashboard() {
  const inicial = periodoPadrao()
  const [dataInicio, setDataInicio] = useState(inicial.inicio)
  const [dataFim, setDataFim] = useState(inicial.fim)
  const [periodo, setPeriodo] = useState(inicial)
  const [dados, setDados] = useState<PainelGerenciaResponse>()
  const [selecionadoId, setSelecionadoId] = useState(0)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  useEffect(() => { let ativo = true; setCarregando(true); setErro(''); painelApi.consultarGerencia(periodo.inicio, periodo.fim).then((resposta) => { if (!ativo) return; setDados(resposta); const preferido = resposta.discipulados.find((item) => item.ativo) ?? resposta.discipulados[0]; setSelecionadoId(preferido?.id ?? 0) }).catch((error: Error) => { if (ativo) { setDados(undefined); setErro(error.message) } }).finally(() => { if (ativo) setCarregando(false) }); return () => { ativo = false } }, [periodo])
  const selecionado = useMemo(() => dados?.discipulados.find((item) => item.id === selecionadoId), [dados, selecionadoId])
  return <Stack spacing={3}>
    <PageHeader title="Minha gerência" description={dados?.gerencia.nome ?? 'Acompanhamento dos discipulados sob sua gerência.'} eyebrow="Visão gerencial" />
    <FiltroPeriodo dataInicio={dataInicio} dataFim={dataFim} onInicio={setDataInicio} onFim={setDataFim} onAplicar={() => setPeriodo({ inicio: dataInicio, fim: dataFim })} />
    {carregando && <LoadingState label="Carregando painel..." />}
    {erro && <Alert severity="error">{erro}</Alert>}
    {!carregando && dados && <><KpisPresenca resumo={dados.resumo} />{dados.resumo.encontrosRealizados === 0 && <Alert severity="info">Não há encontros realizados no período selecionado.</Alert>}<PainelEvolucao titulo="Evolução mensal da gerência" tabelaTitulo="Resumo mensal da gerência" dados={normalizarMeses(dados.dataInicio, dados.dataFim, dados.evolucao)} />
      {dados.discipulados.length === 0 ? <Alert severity="info">Não há discipulados com histórico no período.</Alert> : <><ResumoAtencao dados={dados.discipulados} /><SupervisaoNaoRealizados dados={dados.encontrosNaoRealizados} /><AnalyticsCard title="Presença por discipulado" description="Selecione uma barra ou uma linha para abrir o histórico detalhado." chart={<GraficoComparacao dados={dados.discipulados} onSelecionar={setSelecionadoId} />} table={<TabelaComparacao dados={dados.discipulados} onSelecionar={setSelecionadoId} />} />{selecionado && <SectionCard title="Detalhe do discipulado" action={<FormControl sx={{ minWidth: { xs: 220, sm: 340 } }}><InputLabel id="discipulado-painel-label">Discipulado</InputLabel><Select labelId="discipulado-painel-label" value={selecionadoId || ''} label="Discipulado" onChange={(event) => setSelecionadoId(Number(event.target.value))}>{dados.discipulados.map((item) => <MenuItem value={item.id} key={item.id}>{item.nome}{item.ativo ? '' : ' (inativo)'}</MenuItem>)}</Select></FormControl>}><Detalhe discipulado={selecionado} inicio={dados.dataInicio} fim={dados.dataFim} /></SectionCard>}</>}
    </>}
  </Stack>
}

function ResumoAtencao({ dados }: { dados: DiscipuladoPainel[] }) {
  const abaixo = dados.filter((item) => item.resumo.encontrosRealizados > 0 && item.resumo.percentualPresenca < REFERENCIA_INFORMATIVA).length
  const semEncontros = dados.filter((item) => item.resumo.encontrosRealizados === 0).length
  const quedas = dados.filter(temQuedaRecente).length
  return <Alert severity={abaixo || semEncontros || quedas ? 'warning' : 'info'}><strong>Leitura do período:</strong> {abaixo} de {dados.length} discipulados abaixo da referência informativa de {REFERENCIA_INFORMATIVA}%; {semEncontros} sem encontros e {quedas} com queda no último mês disponível. Essa referência não representa uma meta pastoral.</Alert>
}

function temQuedaRecente(item: DiscipuladoPainel) { const meses = item.evolucao.filter((mes) => mes.presentes + mes.ausentes > 0); return meses.length >= 2 && meses.at(-1)!.percentualPresenca < meses.at(-2)!.percentualPresenca }

function SupervisaoNaoRealizados({ dados }: { dados: PainelGerenciaResponse['encontrosNaoRealizados'] }) {
  return <SectionCard title={`Encontros não realizados (${dados.length})`} description="Ocorrências registradas por administradores no período selecionado.">{dados.length ? <TableContainer><Table size="small" aria-label="Encontros não realizados"><TableHead><TableRow><TableCell scope="col">Data</TableCell><TableCell scope="col">Discipulado</TableCell><TableCell scope="col">Justificativa</TableCell></TableRow></TableHead><TableBody>{dados.map((item) => <TableRow key={item.encontroId}><TableCell>{formatarData(item.data)}</TableCell><TableCell>{item.discipuladoNome}</TableCell><TableCell>{item.justificativa}</TableCell></TableRow>)}</TableBody></Table></TableContainer> : <Alert severity="success">Nenhum encontro foi marcado como não realizado no período.</Alert>}</SectionCard>
}

function GraficoComparacao({ dados, onSelecionar }: { dados: DiscipuladoPainel[]; onSelecionar: (id: number) => void }) {
  const ordenados = [...dados].sort((a, b) => a.resumo.percentualPresenca - b.resumo.percentualPresenca || a.nome.localeCompare(b.nome, 'pt-BR'))
  return <Box role="img" aria-label="Gráfico de barras do percentual de presença por discipulado."><ReactECharts onEvents={{ click: (param: { dataIndex: number }) => { const item = ordenados[param.dataIndex]; if (item) onSelecionar(item.id) } }} style={{ height: Math.min(650, Math.max(300, ordenados.length * 48)) }} option={{ aria: { enabled: true }, tooltip: { trigger: 'axis', valueFormatter: (valor: number) => percentual(valor) }, grid: { left: 140, right: 60, bottom: 30, containLabel: true }, xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } }, yAxis: { type: 'category', data: ordenados.map((item) => item.nome) }, series: [{ type: 'bar', name: 'Presença', data: ordenados.map((item) => ({ value: item.resumo.percentualPresenca, itemStyle: { color: item.resumo.encontrosRealizados === 0 ? '#78909C' : item.resumo.percentualPresenca < REFERENCIA_INFORMATIVA ? '#B76E00' : '#2E7D32', borderRadius: [0, 5, 5, 0] } })), label: { show: true, position: 'right', formatter: '{c}%' } }] }} /></Box>
}

function TabelaComparacao({ dados, onSelecionar }: { dados: DiscipuladoPainel[]; onSelecionar: (id: number) => void }) { return <TableContainer><Table size="small" aria-label="Resumo por discipulado"><TableHead><TableRow><TableCell scope="col">Discipulado</TableCell><TableCell scope="col">Encontros</TableCell><TableCell scope="col">Presentes</TableCell><TableCell scope="col">Ausentes</TableCell><TableCell scope="col">Presença</TableCell><TableCell scope="col">Sinais</TableCell></TableRow></TableHead><TableBody>{dados.map((item) => <TableRow key={item.id} hover><TableCell component="th" scope="row"><Button color="inherit" onClick={() => onSelecionar(item.id)}>{item.nome}{!item.ativo && ' (inativo)'}</Button></TableCell><TableCell>{item.resumo.encontrosRealizados}</TableCell><TableCell>{item.resumo.presentes}</TableCell><TableCell>{item.resumo.ausentes}</TableCell><TableCell>{percentual(item.resumo.percentualPresenca)}</TableCell><TableCell><Stack direction="row" spacing={0.5} flexWrap="wrap">{item.resumo.encontrosRealizados === 0 && <Chip size="small" label="Sem encontros" />}{temQuedaRecente(item) && <Chip size="small" color="warning" label="Queda recente" />}</Stack></TableCell></TableRow>)}</TableBody></Table></TableContainer> }

function Detalhe({ discipulado, inicio, fim }: { discipulado: DiscipuladoPainel; inicio: string; fim: string }) { return <Stack spacing={2}><Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap"><Typography variant="h5">{discipulado.nome}</Typography>{!discipulado.ativo && <Chip color="warning" label="Inativo" size="small" />}{temQuedaRecente(discipulado) && <Chip color="warning" variant="outlined" label="Queda no último mês disponível" size="small" />}</Stack><KpisPresenca resumo={discipulado.resumo} /><PainelEvolucao titulo="Evolução mensal do discipulado" tabelaTitulo="Histórico mensal do discipulado" dados={normalizarMeses(inicio, fim, discipulado.evolucao)} /></Stack> }

function formatarData(data: string) { return new Intl.DateTimeFormat('pt-BR').format(new Date(`${data}T12:00:00`)) }
