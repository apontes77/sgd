import { OpenInNewRounded, PrintRounded } from '@mui/icons-material'
import {
  Alert,
  Box,
  Button,
  GlobalStyles,
  Skeleton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  useMediaQuery,
} from '@mui/material'
import ReactECharts from 'echarts-for-react'
import { useEffect, useMemo, useState } from 'react'

import {
  type EvolucaoMensal,
  type PainelAdminResponse,
  painelApi,
  type PainelGerenciaResponse,
  type ResumoPainel,
} from '@/features/dashboards/api'
import {
  axisLabelStyle,
  GAUGE_ZONES,
  gaugeZoneColors,
  legendTextStyle,
  seriesLabelStyle,
  useChartColors,
} from '@/shared/charts/chartTheme'
import { FiltroPeriodo } from '@/shared/dashboard-ui'
import { formatarMes, normalizarMeses, percentual, periodoPadrao } from '@/shared/dashboard-utils'
import { AnalyticsCard, DataTableCard, EmptyState, PageHeader, SectionCard } from '@/shared/ui'

export { GAUGE_ZONES }

type Escopo = 'admin' | 'gerencia'
type RankingItem = { id: number; nome: string; percentualPresenca: number }
type HeatmapItem = { id: number; nome: string; referencia: string; percentualPresenca: number }

type VisaoDados = {
  dataInicio: string
  dataFim: string
  resumo: ResumoPainel
  evolucao: EvolucaoMensal[]
  encontrosNaoRealizados: number
  ranking: RankingItem[]
  heatmap: HeatmapItem[]
  rankingLabel: string
  heatmapLabel: string
  detalheLabel: string
}

export default function ExecutiveDashboard({
  escopo = 'admin',
  onAbrirDetalhe,
}: {
  escopo?: Escopo
  onAbrirDetalhe?: () => void
}) {
  const inicial = periodoPadrao()
  const [dataInicio, setDataInicio] = useState(inicial.inicio)
  const [dataFim, setDataFim] = useState(inicial.fim)
  const [periodo, setPeriodo] = useState(inicial)
  const [dados, setDados] = useState<VisaoDados>()
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    let ativo = true
    setCarregando(true)
    setErro('')
    const consulta =
      escopo === 'admin'
        ? painelApi.consultar(periodo.inicio, periodo.fim).then(mapAdmin)
        : painelApi.consultarGerencia(periodo.inicio, periodo.fim).then(mapGerencia)
    consulta
      .then((response) => {
        if (ativo) setDados(response)
      })
      .catch((error: Error) => {
        if (ativo) {
          setDados(undefined)
          setErro(error.message)
        }
      })
      .finally(() => {
        if (ativo) setCarregando(false)
      })
    return () => {
      ativo = false
    }
  }, [periodo, escopo])

  return (
    <Stack spacing={3} className="executive-dashboard">
      <GlobalStyles
        styles={{
          '@media print': {
            'nav, .MuiAppBar-root, .MuiDrawer-root, .MuiBottomNavigation-root, .executive-no-print': {
              display: 'none !important',
            },
            main: { margin: '0 !important', padding: '0 !important' },
            body: { background: '#fff' },
          },
        }}
      />
      <PageHeader
        title="Visão executiva"
        description={
          escopo === 'admin'
            ? 'Painel consolidado de indicadores da organização em uma única tela.'
            : 'Painel consolidado dos indicadores da sua gerência em uma única tela.'
        }
        eyebrow="Dashboards & BI"
        action={
          <Stack className="executive-no-print" direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            {onAbrirDetalhe && (
              <Button variant="outlined" startIcon={<OpenInNewRounded />} onClick={onAbrirDetalhe}>
                {dados?.detalheLabel ?? 'Ver detalhe'}
              </Button>
            )}
            <Button variant="contained" startIcon={<PrintRounded />} onClick={() => window.print()}>
              Imprimir / PDF
            </Button>
          </Stack>
        }
      />
      <Box className="executive-no-print">
        <FiltroPeriodo
          dataInicio={dataInicio}
          dataFim={dataFim}
          onInicio={setDataInicio}
          onFim={setDataFim}
          onAplicar={() => setPeriodo({ inicio: dataInicio, fim: dataFim })}
        />
      </Box>
      {erro && <Alert severity="error">{erro}</Alert>}
      {carregando && <GradeSkeleton />}
      {!carregando && dados && <GradeExecutiva dados={dados} onAbrirDetalhe={onAbrirDetalhe} />}
    </Stack>
  )
}

function mapAdmin(response: PainelAdminResponse): VisaoDados {
  return {
    dataInicio: response.dataInicio,
    dataFim: response.dataFim,
    resumo: response.resumo,
    evolucao: response.evolucao,
    encontrosNaoRealizados: response.encontrosNaoRealizados,
    ranking: response.gerencias.map((item) => ({
      id: item.id,
      nome: item.nome,
      percentualPresenca: item.percentualPresenca,
    })),
    heatmap: response.gerenciasMensal.map((item) => ({
      id: item.gerenciaId,
      nome: item.gerenciaNome,
      referencia: item.referencia,
      percentualPresenca: item.percentualPresenca,
    })),
    rankingLabel: 'Top gerências por presença',
    heatmapLabel: 'Presença por gerência × mês',
    detalheLabel: 'Abrir painel detalhado',
  }
}

function mapGerencia(response: PainelGerenciaResponse): VisaoDados {
  return {
    dataInicio: response.dataInicio,
    dataFim: response.dataFim,
    resumo: response.resumo,
    evolucao: response.evolucao,
    encontrosNaoRealizados: response.encontrosNaoRealizados.length,
    ranking: response.discipulados.map((item) => ({
      id: item.id,
      nome: item.nome,
      percentualPresenca: item.resumo.percentualPresenca,
    })),
    heatmap: response.discipulados.flatMap((item) =>
      item.evolucao.map((mes) => ({
        id: item.id,
        nome: item.nome,
        referencia: mes.referencia,
        percentualPresenca: mes.percentualPresenca,
      })),
    ),
    rankingLabel: 'Top discipulados por presença',
    heatmapLabel: 'Presença por discipulado × mês',
    detalheLabel: 'Abrir minha gerência',
  }
}

function GradeSkeleton() {
  return (
    <Box
      role="status"
      aria-label="Carregando visão executiva..."
      sx={{
        display: 'grid',
        gap: 2.5,
        gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))', xl: 'repeat(3, minmax(0, 1fr))' },
      }}
    >
      {Array.from({ length: 6 }, (_, index) => (
        <SectionCard key={index}>
          <Stack spacing={1.5}>
            <Skeleton variant="text" width="55%" height={28} />
            <Skeleton variant="text" width="80%" height={18} />
            <Skeleton variant="rounded" height={240} />
          </Stack>
        </SectionCard>
      ))}
    </Box>
  )
}

function GradeExecutiva({ dados, onAbrirDetalhe }: { dados: VisaoDados; onAbrirDetalhe?: () => void }) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const meses = useMemo(
    () => normalizarMeses(dados.dataInicio, dados.dataFim, dados.evolucao),
    [dados.dataInicio, dados.dataFim, dados.evolucao],
  )
  const topRanking = useMemo(
    () => [...dados.ranking].sort((a, b) => b.percentualPresenca - a.percentualPresenca).slice(0, 8),
    [dados.ranking],
  )
  const semVolume = dados.resumo.encontrosRealizados === 0 && dados.encontrosNaoRealizados === 0
  const semComposicao = dados.resumo.presentes + dados.resumo.ausentes + dados.resumo.visitantes === 0

  return (
    <Box
      data-testid="grade-executiva"
      sx={{
        display: 'grid',
        gap: 2.5,
        gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))', xl: 'repeat(3, minmax(0, 1fr))' },
      }}
    >
      <SectionCard title="Presença geral" description="Percentual consolidado no período.">
        {semComposicao ? (
          <EmptyState
            title="Sem registros de presença"
            description="Não há presentes ou ausentes no período selecionado."
          />
        ) : (
          <GaugePresenca valor={dados.resumo.percentualPresenca} />
        )}
      </SectionCard>
      <AnalyticsCard
        title="Volume mensal"
        description="Presentes, ausentes e visitantes por mês."
        chart={
          meses.every((item) => !item.possuiEncontro) ? (
            <EmptyState title="Sem volume mensal" description="Não há encontros realizados no período." />
          ) : (
            <BarrasVolume meses={meses} />
          )
        }
        table={<TabelaVolume meses={meses} />}
      />
      <SectionCard title="Encontros por situação" description="Realizados versus não realizados.">
        {semVolume ? (
          <EmptyState
            title="Sem encontros no período"
            description="Nenhum encontro realizado ou não realizado foi encontrado."
          />
        ) : (
          <BarrasSituacao realizados={dados.resumo.encontrosRealizados} naoRealizados={dados.encontrosNaoRealizados} />
        )}
      </SectionCard>
      <SectionCard title="Composição de presença" description="Distribuição de presentes, ausentes e visitantes.">
        {semComposicao ? (
          <EmptyState title="Sem composição" description="Não há dados de presença, ausência ou visitantes." />
        ) : (
          <RoscaComposicao
            presentes={dados.resumo.presentes}
            ausentes={dados.resumo.ausentes}
            visitantes={dados.resumo.visitantes}
          />
        )}
      </SectionCard>
      <AnalyticsCard
        title={dados.rankingLabel}
        description={
          onAbrirDetalhe ? 'Clique em uma barra para abrir o detalhe.' : 'Ranking por percentual de presença.'
        }
        chart={
          <RankingBarras dados={topRanking} vazio={`Sem itens com registros no período.`} onSelect={onAbrirDetalhe} />
        }
        table={
          <TabelaRanking dados={topRanking} vazio={`Sem itens com registros no período.`} onSelect={onAbrirDetalhe} />
        }
      />
      <AnalyticsCard
        title={dados.heatmapLabel}
        description={
          onAbrirDetalhe ? 'Clique em uma célula para abrir o detalhe.' : 'Mapa de calor da presença mensal.'
        }
        defaultView={mobile ? 'table' : 'chart'}
        chart={
          <HeatmapSeries
            inicio={dados.dataInicio}
            fim={dados.dataFim}
            dados={dados.heatmap}
            vazio="Sem histórico mensal no período."
            onSelect={onAbrirDetalhe}
          />
        }
        table={
          <TabelaHeatmap
            inicio={dados.dataInicio}
            fim={dados.dataFim}
            dados={dados.heatmap}
            vazio="Sem histórico mensal no período."
            onSelect={onAbrirDetalhe}
          />
        }
      />
    </Box>
  )
}

function GaugePresenca({ valor }: { valor: number }) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const colors = useChartColors()
  return (
    <Box role="img" aria-label={`Presença geral de ${percentual(valor)} no período.`}>
      <ReactECharts
        style={{ height: mobile ? 260 : 280, width: '100%' }}
        option={{
          aria: { enabled: true },
          textStyle: { color: colors.text },
          series: [
            {
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
                  color: gaugeZoneColors(),
                },
              },
              pointer: { length: '62%', width: 5 },
              axisTick: { show: false },
              splitLine: { length: 10, lineStyle: { width: 2, color: colors.neutral } },
              axisLabel: { distance: 18, fontSize: 11, color: colors.textSecondary },
              detail: {
                valueAnimation: true,
                formatter: (v: number) =>
                  `${v.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%`,
                fontSize: mobile ? 22 : 26,
                fontWeight: 700,
                color: colors.text,
                offsetCenter: [0, '70%'],
              },
              data: [{ value: valor, name: 'Presença' }],
              title: { offsetCenter: [0, '92%'], fontSize: 13, color: colors.textSecondary },
            },
          ],
        }}
        notMerge
      />
    </Box>
  )
}

function BarrasVolume({ meses }: { meses: ReturnType<typeof normalizarMeses> }) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const colors = useChartColors()
  const labels = meses.map((item) => formatarMes(item.referencia))
  return (
    <Box role="img" aria-label="Gráfico de barras do volume mensal de presentes, ausentes e visitantes.">
      <ReactECharts
        style={{ height: mobile ? 260 : 280, width: '100%' }}
        option={{
          aria: { enabled: true },
          textStyle: { color: colors.text },
          tooltip: { trigger: 'axis' },
          legend: { top: 0, textStyle: legendTextStyle(colors, 11) },
          grid: { top: 40, left: 12, right: 12, bottom: 28, containLabel: true },
          xAxis: {
            type: 'category',
            data: labels,
            axisLabel: { ...axisLabelStyle(colors, 11), hideOverlap: true },
            axisLine: { lineStyle: { color: colors.axisLine } },
          },
          yAxis: {
            type: 'value',
            minInterval: 1,
            axisLabel: axisLabelStyle(colors, 11),
            splitLine: { lineStyle: { color: colors.splitLine } },
          },
          series: [
            {
              name: 'Presentes',
              type: 'bar',
              barMaxWidth: 22,
              data: meses.map((item) => (item.possuiEncontro ? item.presentes : null)),
              itemStyle: { color: colors.success, borderRadius: [3, 3, 0, 0] },
            },
            {
              name: 'Ausentes',
              type: 'bar',
              barMaxWidth: 22,
              data: meses.map((item) => (item.possuiEncontro ? item.ausentes : null)),
              itemStyle: { color: colors.error, borderRadius: [3, 3, 0, 0] },
            },
            {
              name: 'Visitantes',
              type: 'bar',
              barMaxWidth: 22,
              data: meses.map((item) => (item.possuiEncontro ? item.visitantes : null)),
              itemStyle: { color: colors.secondary, borderRadius: [3, 3, 0, 0] },
            },
          ],
        }}
        notMerge
      />
    </Box>
  )
}

function BarrasSituacao({ realizados, naoRealizados }: { realizados: number; naoRealizados: number }) {
  const colors = useChartColors()
  return (
    <Box role="img" aria-label={`Encontros realizados: ${realizados}. Encontros não realizados: ${naoRealizados}.`}>
      <ReactECharts
        style={{ height: 280, width: '100%' }}
        option={{
          aria: { enabled: true },
          textStyle: { color: colors.text },
          tooltip: { trigger: 'axis' },
          grid: { top: 24, left: 24, right: 24, bottom: 32, containLabel: true },
          xAxis: {
            type: 'category',
            data: ['Realizado', 'Não realizado'],
            axisLabel: axisLabelStyle(colors, 12),
            axisLine: { lineStyle: { color: colors.axisLine } },
          },
          yAxis: {
            type: 'value',
            minInterval: 1,
            axisLabel: axisLabelStyle(colors, 11),
            splitLine: { lineStyle: { color: colors.splitLine } },
          },
          series: [
            {
              type: 'bar',
              barMaxWidth: 64,
              data: [
                { value: realizados, itemStyle: { color: colors.success, borderRadius: [5, 5, 0, 0] } },
                { value: naoRealizados, itemStyle: { color: colors.warning, borderRadius: [5, 5, 0, 0] } },
              ],
              label: { show: true, position: 'top', ...seriesLabelStyle(colors, 12) },
            },
          ],
        }}
        notMerge
      />
    </Box>
  )
}

function RoscaComposicao({
  presentes,
  ausentes,
  visitantes,
}: {
  presentes: number
  ausentes: number
  visitantes: number
}) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const colors = useChartColors()
  const total = presentes + ausentes + visitantes
  return (
    <Box role="img" aria-label={`Composição: ${presentes} presentes, ${ausentes} ausentes e ${visitantes} visitantes.`}>
      <ReactECharts
        style={{ height: 280, width: '100%' }}
        option={{
          aria: { enabled: true },
          textStyle: { color: colors.text },
          tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
          legend: { bottom: 0, textStyle: legendTextStyle(colors, 11) },
          series: [
            {
              type: 'pie',
              radius: ['45%', '70%'],
              center: ['50%', '46%'],
              avoidLabelOverlap: true,
              label: {
                show: !mobile,
                formatter: total === 0 ? 'Sem dados' : '{b}\n{d}%',
                fontSize: 11,
                color: colors.text,
                textBorderColor: 'transparent',
                textBorderWidth: 0,
              },
              labelLine: { show: !mobile, lineStyle: { color: colors.textSecondary } },
              data: [
                { name: 'Presentes', value: presentes, itemStyle: { color: colors.success } },
                { name: 'Ausentes', value: ausentes, itemStyle: { color: colors.error } },
                { name: 'Visitantes', value: visitantes, itemStyle: { color: colors.secondary } },
              ],
            },
          ],
        }}
        notMerge
      />
    </Box>
  )
}

function RankingBarras({ dados, vazio, onSelect }: { dados: RankingItem[]; vazio: string; onSelect?: () => void }) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const colors = useChartColors()
  if (dados.length === 0) return <EmptyState title="Sem ranking" description={vazio} />
  const ordenados = [...dados].reverse()
  const nomeWidth = mobile ? 108 : 132
  return (
    <Box
      role="img"
      aria-label="Ranking horizontal do percentual de presença."
      className="executive-no-print-hint"
      sx={{ minWidth: 0 }}
    >
      <ReactECharts
        onEvents={onSelect ? { click: () => onSelect() } : undefined}
        style={{ height: Math.min(320, Math.max(220, ordenados.length * (mobile ? 36 : 42))), width: '100%' }}
        option={{
          aria: { enabled: true },
          textStyle: { color: colors.text },
          tooltip: {
            trigger: 'axis',
            formatter: (params: Array<{ dataIndex: number; value: number }>) => {
              const item = ordenados[params[0]?.dataIndex]
              return item ? `${item.nome}<br/>Presença: ${percentual(item.percentualPresenca)}` : ''
            },
          },
          grid: { left: 8, right: mobile ? 44 : 52, top: 8, bottom: 8, containLabel: true },
          xAxis: {
            type: 'value',
            max: 100,
            axisLabel: { ...axisLabelStyle(colors, mobile ? 11 : 12), formatter: '{value}%' },
            splitLine: { lineStyle: { color: colors.splitLine } },
          },
          yAxis: {
            type: 'category',
            data: ordenados.map((item) => item.nome),
            axisLabel: {
              ...axisLabelStyle(colors, mobile ? 11 : 12),
              width: nomeWidth,
              overflow: 'truncate',
            },
            axisLine: { lineStyle: { color: colors.axisLine } },
          },
          series: [
            {
              type: 'bar',
              data: ordenados.map((item) => item.percentualPresenca),
              label: {
                show: true,
                position: 'right',
                formatter: '{c}%',
                ...seriesLabelStyle(colors, mobile ? 11 : 12),
              },
              itemStyle: { color: colors.primary, borderRadius: [0, 5, 5, 0] },
              barMaxWidth: 22,
              cursor: onSelect ? 'pointer' : 'default',
            },
          ],
        }}
        notMerge
      />
    </Box>
  )
}

function TabelaRanking({ dados, vazio, onSelect }: { dados: RankingItem[]; vazio: string; onSelect?: () => void }) {
  if (dados.length === 0) return <EmptyState title="Sem ranking" description={vazio} />
  return (
    <DataTableCard>
      <Table size="small" aria-label="Ranking por percentual de presença">
        <TableHead>
          <TableRow>
            <TableCell scope="col">#</TableCell>
            <TableCell scope="col">Nome</TableCell>
            <TableCell scope="col" align="right">
              Presença
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {dados.map((item, index) => (
            <TableRow
              key={item.id}
              hover={Boolean(onSelect)}
              onClick={onSelect}
              sx={onSelect ? { cursor: 'pointer' } : undefined}
            >
              <TableCell>{index + 1}</TableCell>
              <TableCell component="th" scope="row">
                {item.nome}
              </TableCell>
              <TableCell align="right">{percentual(item.percentualPresenca)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </DataTableCard>
  )
}

function TabelaVolume({ meses }: { meses: ReturnType<typeof normalizarMeses> }) {
  return (
    <DataTableCard>
      <Table size="small" aria-label="Volume mensal de presença">
        <TableHead>
          <TableRow>
            <TableCell scope="col">Mês</TableCell>
            <TableCell scope="col" align="right">
              Presentes
            </TableCell>
            <TableCell scope="col" align="right">
              Ausentes
            </TableCell>
            <TableCell scope="col" align="right">
              Visitantes
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {meses.map((item) => (
            <TableRow key={item.referencia}>
              <TableCell component="th" scope="row">
                {formatarMes(item.referencia)}
              </TableCell>
              {item.possuiEncontro ? (
                <>
                  <TableCell align="right">{item.presentes}</TableCell>
                  <TableCell align="right">{item.ausentes}</TableCell>
                  <TableCell align="right">{item.visitantes}</TableCell>
                </>
              ) : (
                <TableCell colSpan={3} sx={{ color: 'text.secondary' }}>
                  Sem encontros realizados
                </TableCell>
              )}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </DataTableCard>
  )
}

function HeatmapSeries({
  inicio,
  fim,
  dados,
  vazio,
  onSelect,
}: {
  inicio: string
  fim: string
  dados: HeatmapItem[]
  vazio: string
  onSelect?: () => void
}) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const colors = useChartColors()
  const { meses, nomes, cells } = useMemo(() => montarHeatmap(inicio, fim, dados), [inicio, fim, dados])

  if (nomes.length === 0 || meses.length === 0) {
    return <EmptyState title="Sem mapa de calor" description={vazio} />
  }

  return (
    <Box role="img" aria-label="Mapa de calor da presença percentual por item e mês." sx={{ minWidth: 0 }}>
      <ReactECharts
        onEvents={onSelect ? { click: () => onSelect() } : undefined}
        style={{ height: Math.min(360, Math.max(240, nomes.length * (mobile ? 32 : 36) + 80)), width: '100%' }}
        option={{
          aria: { enabled: true },
          textStyle: { color: colors.text },
          tooltip: {
            position: 'top',
            formatter: (params: { value: [number, number, number | null] }) => {
              const [x, y, value] = params.value
              const mes = meses[x]
              const nome = nomes[y]
              if (value == null) return `${nome}<br/>${formatarMes(mes)}: sem registros`
              return `${nome}<br/>${formatarMes(mes)}: ${percentual(value)}`
            },
          },
          grid: { left: 8, right: mobile ? 12 : 24, top: 16, bottom: mobile ? 56 : 48, containLabel: true },
          xAxis: {
            type: 'category',
            data: meses.map(formatarMes),
            splitArea: { show: true },
            axisLabel: { ...axisLabelStyle(colors, mobile ? 10 : 11), hideOverlap: true },
          },
          yAxis: {
            type: 'category',
            data: nomes,
            splitArea: { show: true },
            axisLabel: {
              ...axisLabelStyle(colors, mobile ? 10 : 11),
              width: mobile ? 96 : 110,
              overflow: 'truncate',
            },
          },
          visualMap: {
            min: 0,
            max: 100,
            calculable: true,
            orient: 'horizontal',
            left: 'center',
            bottom: 0,
            inRange: { color: colors.heatmap },
            text: ['100%', '0%'],
            textStyle: legendTextStyle(colors, mobile ? 10 : 11),
          },
          series: [
            {
              type: 'heatmap',
              data: cells,
              label: {
                show: !mobile || meses.length <= 6,
                formatter: (params: { value: [number, number, number | null] }) => {
                  const value = params.value[2]
                  return value == null ? '—' : `${Math.round(value)}%`
                },
                ...seriesLabelStyle(colors, mobile ? 9 : 10),
              },
              emphasis: { itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,.25)' } },
              cursor: onSelect ? 'pointer' : 'default',
            },
          ],
        }}
        notMerge
      />
    </Box>
  )
}

function TabelaHeatmap({
  inicio,
  fim,
  dados,
  vazio,
  onSelect,
}: {
  inicio: string
  fim: string
  dados: HeatmapItem[]
  vazio: string
  onSelect?: () => void
}) {
  const { meses, nomes } = useMemo(() => montarHeatmap(inicio, fim, dados), [inicio, fim, dados])
  const mapa = useMemo(
    () => new Map(dados.map((item) => [`${item.nome}|${item.referencia}`, item.percentualPresenca])),
    [dados],
  )

  if (nomes.length === 0 || meses.length === 0) {
    return <EmptyState title="Sem mapa de calor" description={vazio} />
  }

  return (
    <DataTableCard>
      <Table size="small" aria-label="Presença percentual por item e mês">
        <TableHead>
          <TableRow>
            <TableCell scope="col">Nome</TableCell>
            {meses.map((mes) => (
              <TableCell key={mes} scope="col" align="right">
                {formatarMes(mes)}
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {nomes.map((nome) => (
            <TableRow
              key={nome}
              hover={Boolean(onSelect)}
              onClick={onSelect}
              sx={onSelect ? { cursor: 'pointer' } : undefined}
            >
              <TableCell component="th" scope="row">
                {nome}
              </TableCell>
              {meses.map((mes) => {
                const valor = mapa.get(`${nome}|${mes}`)
                return (
                  <TableCell key={mes} align="right">
                    {valor == null ? '—' : percentual(valor)}
                  </TableCell>
                )
              })}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </DataTableCard>
  )
}

function montarHeatmap(inicio: string, fim: string, dados: HeatmapItem[]) {
  const mesesBase = normalizarMeses(inicio, fim, [])
  const meses = mesesBase.map((item) => item.referencia)
  const nomes = [...new Set(dados.map((item) => item.nome))]
  const mapa = new Map(dados.map((item) => [`${item.nome}|${item.referencia}`, item.percentualPresenca]))
  const cells: Array<[number, number, number | null]> = []
  nomes.forEach((nome, y) => {
    meses.forEach((mes, x) => {
      cells.push([x, y, mapa.has(`${nome}|${mes}`) ? mapa.get(`${nome}|${mes}`)! : null])
    })
  })
  return { meses, nomes, cells }
}
