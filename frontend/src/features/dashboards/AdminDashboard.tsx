import {
  Alert,
  Box,
  Button,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
  useMediaQuery,
} from '@mui/material'
import ReactECharts from 'echarts-for-react'
import { useEffect, useMemo, useState } from 'react'

import { type IndicadorGerencia, type PainelAdminResponse, painelApi } from '@/features/dashboards/api'
import { axisLabelStyle, seriesLabelStyle, useChartColors } from '@/shared/charts/chartTheme'
import { FiltroPeriodo, KpisPresenca, PainelEvolucao } from '@/shared/dashboard-ui'
import { normalizarMeses, percentual, periodoPadrao } from '@/shared/dashboard-utils'
import { AnalyticsCard, DataTableCard, LoadingState, PageHeader } from '@/shared/ui'

type Ordenacao = 'nome' | 'percentual'

export default function AdminDashboard() {
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
    painelApi
      .consultar(periodo.inicio, periodo.fim)
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
  }, [periodo])
  return (
    <Stack spacing={3}>
      <PageHeader
        title="Painel administrativo"
        description="Visão consolidada da frequência em toda a organização."
        eyebrow="Visão geral"
      />
      <FiltroPeriodo
        dataInicio={dataInicio}
        dataFim={dataFim}
        onInicio={setDataInicio}
        onFim={setDataFim}
        onAplicar={() => setPeriodo({ inicio: dataInicio, fim: dataFim })}
      />
      {carregando && <LoadingState label="Carregando painel..." />}
      {erro && <Alert severity="error">{erro}</Alert>}
      {!carregando && dados && <Conteudo dados={dados} />}
    </Stack>
  )
}

function Conteudo({ dados }: { dados: PainelAdminResponse }) {
  const [ordenacao, setOrdenacao] = useState<Ordenacao>('percentual')
  const gerencias = useMemo(() => ordenarGerencias(dados.gerencias, ordenacao), [dados.gerencias, ordenacao])
  const meses = normalizarMeses(dados.dataInicio, dados.dataFim, dados.evolucao)
  return (
    <Stack spacing={3}>
      <KpisPresenca resumo={dados.resumo} />
      <Typography color="text.secondary" variant="body2">
        A presença considera somente participantes presentes e ausentes; visitantes e encontros não realizados não
        entram no percentual.
      </Typography>
      {dados.resumo.encontrosRealizados === 0 && (
        <Alert severity="info">Não há encontros realizados no período selecionado.</Alert>
      )}
      <PainelEvolucao titulo="Evolução mensal" tabelaTitulo="Resumo mensal" dados={meses} />
      <Box>
        <Typography variant="overline" color="primary.main" fontWeight={700}>
          Organização
        </Typography>
        <Typography variant="h5" component="h2">
          Recortes organizacionais
        </Typography>
      </Box>
      <Box
        sx={{
          display: 'grid',
          gap: 3,
          gridTemplateColumns: { xs: '1fr', xl: 'minmax(0, 1.35fr) minmax(380px, .65fr)' },
        }}
      >
        <AnalyticsCard
          title="Presença por gerência"
          description="Percentual de presença com base em presentes e ausentes. Total = presentes + visitantes."
          chart={<GraficoGerencias dados={gerencias} />}
          table={<TabelaGerencias dados={gerencias} ordenacao={ordenacao} onOrdenacao={setOrdenacao} />}
        />
        <AnalyticsCard
          title="Presença por sexo do discipulado"
          description="Comparação de percentuais de presença."
          chart={<GraficoSexos dados={dados} />}
          table={<TabelaSexos dados={dados} />}
        />
      </Box>
    </Stack>
  )
}

function ordenarGerencias(dados: IndicadorGerencia[], ordenacao: Ordenacao) {
  return [...dados].sort((a, b) =>
    ordenacao === 'nome' ? a.nome.localeCompare(b.nome, 'pt-BR') : b.percentualPresenca - a.percentualPresenca,
  )
}

function GraficoGerencias({ dados }: { dados: IndicadorGerencia[] }) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const colors = useChartColors()
  const nomeWidth = mobile ? 108 : 140
  return (
    <Box role="img" aria-label="Gráfico de barras do percentual de presença por gerência." sx={{ minWidth: 0 }}>
      <ReactECharts
        style={{
          height: Math.min(mobile ? 480 : 600, Math.max(mobile ? 260 : 300, dados.length * (mobile ? 40 : 48))),
        }}
        option={{
          aria: { enabled: true },
          textStyle: { color: colors.text },
          tooltip: {
            trigger: 'axis',
            formatter: (params: Array<{ dataIndex: number; value: number }>) => {
              const item = dados[params[0]?.dataIndex]
              return item
                ? `${item.nome}<br/>Presença: ${percentual(item.percentualPresenca)}<br/>Presentes: ${item.presentes}<br/>Visitantes: ${item.visitantes}<br/>Total: ${item.presentes + item.visitantes}`
                : ''
            },
          },
          grid: { left: 8, right: mobile ? 48 : 60, bottom: 30, containLabel: true },
          xAxis: {
            type: 'value',
            max: 100,
            axisLabel: { ...axisLabelStyle(colors, mobile ? 11 : 12), formatter: '{value}%' },
            splitLine: { lineStyle: { color: colors.splitLine } },
          },
          yAxis: {
            type: 'category',
            data: dados.map((item) => item.nome),
            axisLabel: {
              ...axisLabelStyle(colors, mobile ? 11 : 12),
              width: nomeWidth,
              overflow: 'truncate',
            },
            axisLine: { lineStyle: { color: colors.axisLine } },
          },
          series: [
            {
              name: 'Presença',
              type: 'bar',
              data: dados.map((item) => item.percentualPresenca),
              label: {
                show: true,
                position: 'right',
                formatter: '{c}%',
                ...seriesLabelStyle(colors, mobile ? 11 : 12),
              },
              itemStyle: { color: colors.primary, borderRadius: [0, 5, 5, 0] },
            },
          ],
        }}
      />
    </Box>
  )
}

function TabelaGerencias({
  dados,
  ordenacao,
  onOrdenacao,
}: {
  dados: IndicadorGerencia[]
  ordenacao: Ordenacao
  onOrdenacao: (valor: Ordenacao) => void
}) {
  return (
    <DataTableCard>
      <Table size="small" aria-label="Resumo por gerência">
        <TableHead>
          <TableRow>
            <TableCell>
              <BotaoOrdenacao ativo={ordenacao === 'nome'} onClick={() => onOrdenacao('nome')}>
                Gerência
              </BotaoOrdenacao>
            </TableCell>
            <TableCell>Presentes</TableCell>
            <TableCell>Ausentes</TableCell>
            <TableCell>Visitantes</TableCell>
            <TableCell>Total</TableCell>
            <TableCell>
              <BotaoOrdenacao ativo={ordenacao === 'percentual'} onClick={() => onOrdenacao('percentual')}>
                Presença
              </BotaoOrdenacao>
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {dados.map((item) => (
            <TableRow key={item.id} hover>
              <TableCell component="th" scope="row">
                {item.nome}
              </TableCell>
              <TableCell>{item.presentes}</TableCell>
              <TableCell>{item.ausentes}</TableCell>
              <TableCell>{item.visitantes}</TableCell>
              <TableCell>{item.presentes + item.visitantes}</TableCell>
              <TableCell>{percentual(item.percentualPresenca)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </DataTableCard>
  )
}

function BotaoOrdenacao({ ativo, onClick, children }: { ativo: boolean; onClick: () => void; children: string }) {
  return (
    <Button size="small" color="inherit" onClick={onClick} aria-pressed={ativo}>
      {children}
      {ativo ? ' ↓' : ''}
    </Button>
  )
}

function GraficoSexos({ dados }: { dados: PainelAdminResponse }) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const colors = useChartColors()
  const labels = dados.sexos.map((item) => (item.sexo === 'MASCULINO' ? 'Masculino' : 'Feminino'))
  return (
    <Box
      role="img"
      aria-label="Gráfico de barras comparando o percentual de presença por sexo do discipulado."
      sx={{ minWidth: 0 }}
    >
      <ReactECharts
        style={{ height: mobile ? 260 : 300 }}
        option={{
          aria: { enabled: true },
          textStyle: { color: colors.text },
          tooltip: {
            trigger: 'axis',
            formatter: (params: Array<{ dataIndex: number }>) => {
              const item = dados.sexos[params[0]?.dataIndex]
              return item
                ? `${labels[params[0].dataIndex]}<br/>Presença: ${percentual(item.percentualPresenca)}<br/>Presentes: ${item.presentes}<br/>Ausentes: ${item.ausentes}`
                : ''
            },
          },
          grid: { left: 8, right: mobile ? 48 : 60, bottom: 30, containLabel: true },
          xAxis: {
            type: 'value',
            min: 0,
            max: 100,
            axisLabel: { ...axisLabelStyle(colors, mobile ? 11 : 12), formatter: '{value}%' },
            splitLine: { lineStyle: { color: colors.splitLine } },
          },
          yAxis: {
            type: 'category',
            data: labels,
            axisLabel: axisLabelStyle(colors, mobile ? 12 : 13),
            axisLine: { lineStyle: { color: colors.axisLine } },
          },
          series: [
            {
              type: 'bar',
              data: dados.sexos.map((item) => item.percentualPresenca),
              label: {
                show: true,
                position: 'right',
                formatter: '{c}%',
                ...seriesLabelStyle(colors, mobile ? 11 : 12),
              },
              itemStyle: { color: colors.secondary, borderRadius: [0, 5, 5, 0] },
            },
          ],
        }}
      />
    </Box>
  )
}

function TabelaSexos({ dados }: { dados: PainelAdminResponse }) {
  return (
    <DataTableCard>
      <Table size="small" aria-label="Resumo por sexo">
        <TableHead>
          <TableRow>
            <TableCell scope="col">Sexo</TableCell>
            <TableCell scope="col">Presentes</TableCell>
            <TableCell scope="col">Ausentes</TableCell>
            <TableCell scope="col">Presença</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {dados.sexos.map((item) => (
            <TableRow key={item.sexo}>
              <TableCell component="th" scope="row">
                {item.sexo === 'MASCULINO' ? 'Masculino' : 'Feminino'}
              </TableCell>
              <TableCell>{item.presentes}</TableCell>
              <TableCell>{item.ausentes}</TableCell>
              <TableCell>{percentual(item.percentualPresenca)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </DataTableCard>
  )
}
