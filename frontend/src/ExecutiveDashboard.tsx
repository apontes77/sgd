import { Alert, Box, Stack, useMediaQuery } from '@mui/material'
import ReactECharts from 'echarts-for-react'
import { useEffect, useMemo, useState } from 'react'
import { FiltroPeriodo } from './dashboardComponents'
import { formatarMes, normalizarMeses, percentual, periodoPadrao } from './dashboardUtils'
import { painelApi, type IndicadorGerencia, type IndicadorGerenciaMensal, type PainelAdminResponse } from './painelApi'
import { LoadingState, PageHeader, SectionCard } from './ui'

export default function ExecutiveDashboard() {
  const inicial = periodoPadrao()
  const [dataInicio, setDataInicio] = useState(inicial.inicio)
  const [dataFim, setDataFim] = useState(inicial.fim)
  const [periodo, setPeriodo] = useState(inicial)
  const [dados, setDados] = useState<PainelAdminResponse>()
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    let ativo = true
    setCarregando(true)
    setErro('')
    painelApi.consultar(periodo.inicio, periodo.fim)
      .then((response) => { if (ativo) setDados(response) })
      .catch((error: Error) => { if (ativo) { setDados(undefined); setErro(error.message) } })
      .finally(() => { if (ativo) setCarregando(false) })
    return () => { ativo = false }
  }, [periodo])

  return (
    <Stack spacing={3}>
      <PageHeader
        title="Visão executiva"
        description="Painel consolidado de indicadores da organização em uma única tela."
        eyebrow="Dashboards & BI"
      />
      <FiltroPeriodo
        dataInicio={dataInicio}
        dataFim={dataFim}
        onInicio={setDataInicio}
        onFim={setDataFim}
        onAplicar={() => setPeriodo({ inicio: dataInicio, fim: dataFim })}
      />
      {carregando && <LoadingState label="Carregando visão executiva..." />}
      {erro && <Alert severity="error">{erro}</Alert>}
      {!carregando && dados && <GradeExecutiva dados={dados} />}
    </Stack>
  )
}

function GradeExecutiva({ dados }: { dados: PainelAdminResponse }) {
  const meses = useMemo(
    () => normalizarMeses(dados.dataInicio, dados.dataFim, dados.evolucao),
    [dados.dataInicio, dados.dataFim, dados.evolucao],
  )
  const topGerencias = useMemo(
    () => [...dados.gerencias].sort((a, b) => b.percentualPresenca - a.percentualPresenca).slice(0, 8),
    [dados.gerencias],
  )

  return (
    <Box
      sx={{
        display: 'grid',
        gap: 2.5,
        gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))', xl: 'repeat(3, minmax(0, 1fr))' },
      }}
    >
      <SectionCard title="Presença geral" description="Percentual consolidado no período.">
        <GaugePresenca valor={dados.resumo.percentualPresenca} />
      </SectionCard>
      <SectionCard title="Volume mensal" description="Presentes, ausentes e visitantes por mês.">
        <BarrasVolume meses={meses} />
      </SectionCard>
      <SectionCard title="Encontros por situação" description="Realizados versus não realizados.">
        <BarrasSituacao realizados={dados.resumo.encontrosRealizados} naoRealizados={dados.encontrosNaoRealizados} />
      </SectionCard>
      <SectionCard title="Composição de presença" description="Distribuição de presentes, ausentes e visitantes.">
        <RoscaComposicao
          presentes={dados.resumo.presentes}
          ausentes={dados.resumo.ausentes}
          visitantes={dados.resumo.visitantes}
        />
      </SectionCard>
      <SectionCard title="Top gerências por presença" description="Ranking das gerências com maior percentual.">
        <RankingGerencias dados={topGerencias} />
      </SectionCard>
      <SectionCard title="Presença por gerência × mês" description="Mapa de calor da presença mensal.">
        <HeatmapGerencias
          inicio={dados.dataInicio}
          fim={dados.dataFim}
          dados={dados.gerenciasMensal}
        />
      </SectionCard>
    </Box>
  )
}

function GaugePresenca({ valor }: { valor: number }) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  return (
    <Box role="img" aria-label={`Presença geral de ${percentual(valor)} no período.`}>
      <ReactECharts
        style={{ height: mobile ? 260 : 280, width: '100%' }}
        option={{
          aria: { enabled: true },
          series: [{
            type: 'gauge',
            startAngle: 210,
            endAngle: -30,
            min: 0,
            max: 100,
            radius: '90%',
            progress: { show: true, width: 14 },
            axisLine: {
              lineStyle: {
                width: 14,
                color: [
                  [0.6, '#C62828'],
                  [0.8, '#B76E00'],
                  [1, '#2E7D32'],
                ],
              },
            },
            pointer: { length: '62%', width: 5 },
            axisTick: { show: false },
            splitLine: { length: 10, lineStyle: { width: 2, color: '#94A3B8' } },
            axisLabel: { distance: 18, fontSize: 11, color: '#667085' },
            detail: {
              valueAnimation: true,
              formatter: (v: number) => `${v.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%`,
              fontSize: mobile ? 22 : 26,
              fontWeight: 700,
              color: '#172033',
              offsetCenter: [0, '70%'],
            },
            data: [{ value: valor, name: 'Presença' }],
            title: { offsetCenter: [0, '92%'], fontSize: 13, color: '#667085' },
          }],
        }}
        notMerge
      />
    </Box>
  )
}

function BarrasVolume({ meses }: { meses: ReturnType<typeof normalizarMeses> }) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const labels = meses.map((item) => formatarMes(item.referencia))
  return (
    <Box role="img" aria-label="Gráfico de barras do volume mensal de presentes, ausentes e visitantes.">
      <ReactECharts
        style={{ height: mobile ? 260 : 280, width: '100%' }}
        option={{
          aria: { enabled: true },
          tooltip: { trigger: 'axis' },
          legend: { top: 0, textStyle: { fontSize: 11 } },
          grid: { top: 40, left: 12, right: 12, bottom: 28, containLabel: true },
          xAxis: { type: 'category', data: labels, axisLabel: { hideOverlap: true, fontSize: 11 } },
          yAxis: { type: 'value', minInterval: 1 },
          series: [
            {
              name: 'Presentes',
              type: 'bar',
              barMaxWidth: 22,
              data: meses.map((item) => (item.possuiEncontro ? item.presentes : null)),
              itemStyle: { color: '#2E7D32', borderRadius: [3, 3, 0, 0] },
            },
            {
              name: 'Ausentes',
              type: 'bar',
              barMaxWidth: 22,
              data: meses.map((item) => (item.possuiEncontro ? item.ausentes : null)),
              itemStyle: { color: '#C62828', borderRadius: [3, 3, 0, 0] },
            },
            {
              name: 'Visitantes',
              type: 'bar',
              barMaxWidth: 22,
              data: meses.map((item) => (item.possuiEncontro ? item.visitantes : null)),
              itemStyle: { color: '#0F8B8D', borderRadius: [3, 3, 0, 0] },
            },
          ],
        }}
        notMerge
      />
    </Box>
  )
}

function BarrasSituacao({ realizados, naoRealizados }: { realizados: number; naoRealizados: number }) {
  return (
    <Box role="img" aria-label={`Encontros realizados: ${realizados}. Encontros não realizados: ${naoRealizados}.`}>
      <ReactECharts
        style={{ height: 280, width: '100%' }}
        option={{
          aria: { enabled: true },
          tooltip: { trigger: 'axis' },
          grid: { top: 24, left: 24, right: 24, bottom: 32, containLabel: true },
          xAxis: { type: 'category', data: ['Realizado', 'Não realizado'] },
          yAxis: { type: 'value', minInterval: 1 },
          series: [{
            type: 'bar',
            barMaxWidth: 64,
            data: [
              { value: realizados, itemStyle: { color: '#2E7D32', borderRadius: [5, 5, 0, 0] } },
              { value: naoRealizados, itemStyle: { color: '#B76E00', borderRadius: [5, 5, 0, 0] } },
            ],
            label: { show: true, position: 'top' },
          }],
        }}
        notMerge
      />
    </Box>
  )
}

function RoscaComposicao({ presentes, ausentes, visitantes }: { presentes: number; ausentes: number; visitantes: number }) {
  const total = presentes + ausentes + visitantes
  return (
    <Box role="img" aria-label={`Composição: ${presentes} presentes, ${ausentes} ausentes e ${visitantes} visitantes.`}>
      <ReactECharts
        style={{ height: 280, width: '100%' }}
        option={{
          aria: { enabled: true },
          tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
          legend: { bottom: 0, textStyle: { fontSize: 11 } },
          series: [{
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['50%', '46%'],
            avoidLabelOverlap: true,
            label: {
              show: true,
              formatter: total === 0 ? 'Sem dados' : '{b}\n{d}%',
              fontSize: 11,
            },
            data: [
              { name: 'Presentes', value: presentes, itemStyle: { color: '#2E7D32' } },
              { name: 'Ausentes', value: ausentes, itemStyle: { color: '#C62828' } },
              { name: 'Visitantes', value: visitantes, itemStyle: { color: '#0F8B8D' } },
            ],
          }],
        }}
        notMerge
      />
    </Box>
  )
}

function RankingGerencias({ dados }: { dados: IndicadorGerencia[] }) {
  if (dados.length === 0) {
    return (
      <Box sx={{ py: 6, textAlign: 'center', color: 'text.secondary' }}>
        Sem gerências com registros no período.
      </Box>
    )
  }
  const ordenados = [...dados].reverse()
  return (
    <Box role="img" aria-label="Ranking horizontal do percentual de presença por gerência.">
      <ReactECharts
        style={{ height: Math.min(320, Math.max(220, ordenados.length * 42)), width: '100%' }}
        option={{
          aria: { enabled: true },
          tooltip: {
            trigger: 'axis',
            formatter: (params: Array<{ dataIndex: number; value: number }>) => {
              const item = ordenados[params[0]?.dataIndex]
              return item ? `${item.nome}<br/>Presença: ${percentual(item.percentualPresenca)}` : ''
            },
          },
          grid: { left: 8, right: 48, top: 8, bottom: 8, containLabel: true },
          xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
          yAxis: { type: 'category', data: ordenados.map((item) => item.nome), axisLabel: { width: 110, overflow: 'truncate' } },
          series: [{
            type: 'bar',
            data: ordenados.map((item) => item.percentualPresenca),
            label: { show: true, position: 'right', formatter: '{c}%' },
            itemStyle: { color: '#3451B2', borderRadius: [0, 5, 5, 0] },
            barMaxWidth: 22,
          }],
        }}
        notMerge
      />
    </Box>
  )
}

function HeatmapGerencias({ inicio, fim, dados }: { inicio: string; fim: string; dados: IndicadorGerenciaMensal[] }) {
  const { meses, gerencias, cells } = useMemo(() => montarHeatmap(inicio, fim, dados), [inicio, fim, dados])

  if (gerencias.length === 0 || meses.length === 0) {
    return (
      <Box sx={{ py: 6, textAlign: 'center', color: 'text.secondary' }}>
        Sem histórico mensal por gerência no período.
      </Box>
    )
  }

  return (
    <Box role="img" aria-label="Mapa de calor da presença percentual por gerência e mês.">
      <ReactECharts
        style={{ height: Math.min(360, Math.max(240, gerencias.length * 36 + 80)), width: '100%' }}
        option={{
          aria: { enabled: true },
          tooltip: {
            position: 'top',
            formatter: (params: { value: [number, number, number | null]; data: [number, number, number | null] }) => {
              const [x, y, value] = params.value
              const mes = meses[x]
              const gerencia = gerencias[y]
              if (value == null) return `${gerencia}<br/>${formatarMes(mes)}: sem registros`
              return `${gerencia}<br/>${formatarMes(mes)}: ${percentual(value)}`
            },
          },
          grid: { left: 8, right: 24, top: 16, bottom: 48, containLabel: true },
          xAxis: {
            type: 'category',
            data: meses.map(formatarMes),
            splitArea: { show: true },
            axisLabel: { hideOverlap: true, fontSize: 11 },
          },
          yAxis: {
            type: 'category',
            data: gerencias,
            splitArea: { show: true },
            axisLabel: { width: 100, overflow: 'truncate', fontSize: 11 },
          },
          visualMap: {
            min: 0,
            max: 100,
            calculable: true,
            orient: 'horizontal',
            left: 'center',
            bottom: 0,
            inRange: { color: ['#FDECEC', '#FFF4DD', '#EAF6EC', '#2E7D32'] },
            text: ['100%', '0%'],
            textStyle: { fontSize: 11 },
          },
          series: [{
            type: 'heatmap',
            data: cells,
            label: {
              show: true,
              formatter: (params: { value: [number, number, number | null] }) => {
                const value = params.value[2]
                return value == null ? '—' : `${Math.round(value)}%`
              },
              fontSize: 10,
            },
            emphasis: { itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,.25)' } },
          }],
        }}
        notMerge
      />
    </Box>
  )
}

function montarHeatmap(inicio: string, fim: string, dados: IndicadorGerenciaMensal[]) {
  const mesesBase = normalizarMeses(inicio, fim, [])
  const meses = mesesBase.map((item) => item.referencia)
  const gerencias = [...new Set(dados.map((item) => item.gerenciaNome))]
  const mapa = new Map(dados.map((item) => [`${item.gerenciaNome}|${item.referencia}`, item.percentualPresenca]))
  const cells: Array<[number, number, number | null]> = []
  gerencias.forEach((gerencia, y) => {
    meses.forEach((mes, x) => {
      cells.push([x, y, mapa.has(`${gerencia}|${mes}`) ? mapa.get(`${gerencia}|${mes}`)! : null])
    })
  })
  return { meses, gerencias, cells }
}
