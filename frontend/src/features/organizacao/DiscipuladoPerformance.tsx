/** Aba Estrutura → Desempenho dos discipulados (presença e tamanho do grupo). */
import {
  Alert,
  Box,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
  useMediaQuery,
} from '@mui/material'
import ReactECharts from 'echarts-for-react'
import { useEffect, useMemo, useState } from 'react'

import {
  type DiscipuladoDesempenho,
  type FrequenciaMensalDesempenho,
  painelApi,
  type PainelDesempenhoResponse,
  type QuantidadeMensalDesempenho,
} from '@/features/dashboards/api'
import type { FaixaEtaria } from '@/shared/api/types'
import { axisLabelStyle, legendTextStyle, seriesLabelStyle, useChartColors } from '@/shared/charts/chartTheme'
import { FiltroPeriodo } from '@/shared/dashboard-ui'
import { formatarMes, periodoPadrao } from '@/shared/dashboard-utils'
import { AnalyticsCard, DataTableCard, LoadingState, SectionCard } from '@/shared/ui'

type Recorte = 'discipulado' | 'sexo' | 'faixa' | 'gerencia'

const faixaEtariaLabel: Record<FaixaEtaria, string> = {
  DE_09_A_11: '09 a 11',
  DE_11_A_13: '11 a 13',
  DE_13_A_15: '13 a 15',
  DE_15_MAIS: '15+',
}

const sexoLabel = (sexo: string) => (sexo === 'MASCULINO' ? 'Masculino' : 'Feminino')

function labelDesempenho(item: { nome: string; discipuladorNome?: string; ativo?: boolean }) {
  const base = item.discipuladorNome ? `${item.nome} - ${item.discipuladorNome}` : item.nome
  return item.ativo === false ? `${base} (inativo)` : base
}

function frequenciaVazia(referencia: string): FrequenciaMensalDesempenho {
  return {
    referencia,
    presentes: 0,
    presentesDiscipulos: 0,
    presentesVisitantes: 0,
    presentesGoe: 0,
    ausentes: 0,
  }
}

function referenciasPeriodo(inicio: string, fim: string): string[] {
  const [anoInicio, mesInicio] = inicio.slice(0, 7).split('-').map(Number)
  const [anoFim, mesFim] = fim.slice(0, 7).split('-').map(Number)
  const cursor = new Date(Date.UTC(anoInicio, mesInicio - 1, 1))
  const limite = new Date(Date.UTC(anoFim, mesFim - 1, 1))
  const meses: string[] = []
  while (cursor <= limite) {
    meses.push(`${cursor.getUTCFullYear()}-${String(cursor.getUTCMonth() + 1).padStart(2, '0')}`)
    cursor.setUTCMonth(cursor.getUTCMonth() + 1)
  }
  return meses
}

function normalizarFrequencia(
  inicio: string,
  fim: string,
  dados: FrequenciaMensalDesempenho[],
): Array<FrequenciaMensalDesempenho & { possuiEncontro: boolean }> {
  const mapa = new Map(dados.map((item) => [item.referencia, item]))
  return referenciasPeriodo(inicio, fim).map((referencia) => {
    const item = mapa.get(referencia)
    return item ? { ...item, possuiEncontro: true } : { ...frequenciaVazia(referencia), possuiEncontro: false }
  })
}

function normalizarQuantidade(
  inicio: string,
  fim: string,
  dados: QuantidadeMensalDesempenho[],
): QuantidadeMensalDesempenho[] {
  const mapa = new Map(dados.map((item) => [item.referencia, item.quantidade]))
  return referenciasPeriodo(inicio, fim).map((referencia) => ({
    referencia,
    quantidade: mapa.get(referencia) ?? 0,
  }))
}

function somarFrequencias(itens: DiscipuladoDesempenho[]): FrequenciaMensalDesempenho[] {
  const mapa = new Map<string, FrequenciaMensalDesempenho>()
  for (const item of itens) {
    for (const mes of item.frequencia) {
      const atual = mapa.get(mes.referencia) ?? frequenciaVazia(mes.referencia)
      mapa.set(mes.referencia, {
        referencia: mes.referencia,
        presentes: atual.presentes + mes.presentes,
        presentesDiscipulos: atual.presentesDiscipulos + mes.presentesDiscipulos,
        presentesVisitantes: atual.presentesVisitantes + mes.presentesVisitantes,
        presentesGoe: atual.presentesGoe + mes.presentesGoe,
        ausentes: atual.ausentes + mes.ausentes,
      })
    }
  }
  return [...mapa.values()].sort((a, b) => a.referencia.localeCompare(b.referencia))
}

function somarQuantidades(itens: DiscipuladoDesempenho[]): QuantidadeMensalDesempenho[] {
  const mapa = new Map<string, number>()
  for (const item of itens) {
    for (const mes of item.discipulos) {
      mapa.set(mes.referencia, (mapa.get(mes.referencia) ?? 0) + mes.quantidade)
    }
  }
  return [...mapa.entries()]
    .map(([referencia, quantidade]) => ({ referencia, quantidade }))
    .sort((a, b) => a.referencia.localeCompare(b.referencia))
}

function totaisFrequencia(itens: DiscipuladoDesempenho[]) {
  return itens.reduce(
    (acc, item) => {
      for (const mes of item.frequencia) {
        acc.presentes += mes.presentes
        acc.ausentes += mes.ausentes
      }
      return acc
    },
    { presentes: 0, ausentes: 0 },
  )
}

export default function DiscipuladoPerformance({ embedded = false }: { embedded?: boolean }) {
  const inicial = periodoPadrao()
  const [dataInicio, setDataInicio] = useState(inicial.inicio)
  const [dataFim, setDataFim] = useState(inicial.fim)
  const [periodo, setPeriodo] = useState(inicial)
  const [dados, setDados] = useState<PainelDesempenhoResponse>()
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [recorte, setRecorte] = useState<Recorte>('discipulado')
  const [discipuladoId, setDiscipuladoId] = useState(0)
  const [gerenciaId, setGerenciaId] = useState(0)

  useEffect(() => {
    let ativo = true
    setCarregando(true)
    setErro('')
    painelApi
      .consultarDesempenho(periodo.inicio, periodo.fim)
      .then((resposta) => {
        if (!ativo) return
        setDados(resposta)
        const preferido = resposta.discipulados.find((item) => item.ativo) ?? resposta.discipulados[0]
        setDiscipuladoId(preferido?.id ?? 0)
        const gerenciaPreferida = preferido?.gerenciaId ?? resposta.discipulados[0]?.gerenciaId ?? 0
        setGerenciaId(gerenciaPreferida)
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

  const selecionado = useMemo(
    () => dados?.discipulados.find((item) => item.id === discipuladoId),
    [dados, discipuladoId],
  )

  const gerencias = useMemo(() => {
    if (!dados) return []
    const mapa = new Map<number, { id: number; nome: string; itens: DiscipuladoDesempenho[] }>()
    for (const item of dados.discipulados) {
      const grupo = mapa.get(item.gerenciaId) ?? { id: item.gerenciaId, nome: item.gerenciaNome, itens: [] }
      grupo.itens.push(item)
      mapa.set(item.gerenciaId, grupo)
    }
    return [...mapa.values()].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
  }, [dados])

  const gerenciaSelecionada = useMemo(
    () => gerencias.find((item) => item.id === gerenciaId) ?? gerencias[0],
    [gerencias, gerenciaId],
  )

  return (
    <Box sx={{ p: embedded ? 0 : 3 }}>
      <Stack spacing={3}>
        <Box>
          <Typography variant="h6" component="h2">
            Desempenho dos discipulados
          </Typography>
          <Typography color="text.secondary" variant="body2">
            Presentes e ausentes por mês, e evolução da quantidade de pessoas vinculadas a cada discipulado.
          </Typography>
        </Box>
        <FiltroPeriodo
          dataInicio={dataInicio}
          dataFim={dataFim}
          onInicio={setDataInicio}
          onFim={setDataFim}
          onAplicar={() => setPeriodo({ inicio: dataInicio, fim: dataFim })}
        />
        <ToggleButtonGroup
          exclusive
          size="small"
          color="primary"
          value={recorte}
          onChange={(_, value: Recorte | null) => {
            if (value) setRecorte(value)
          }}
          aria-label="Recorte do desempenho"
          sx={{ flexWrap: 'wrap' }}
        >
          <ToggleButton value="discipulado">Discipulado</ToggleButton>
          <ToggleButton value="sexo">Sexo</ToggleButton>
          <ToggleButton value="faixa">Faixa etária</ToggleButton>
          <ToggleButton value="gerencia">Gerência</ToggleButton>
        </ToggleButtonGroup>
        {carregando && <LoadingState label="Carregando desempenho..." />}
        {erro && <Alert severity="error">{erro}</Alert>}
        {!carregando && dados && dados.discipulados.length === 0 && (
          <Alert severity="info">Não há discipulados padrão para analisar no período.</Alert>
        )}
        {!carregando && dados && dados.discipulados.length > 0 && recorte === 'discipulado' && (
          <VisaoDiscipulado
            dados={dados}
            selecionado={selecionado}
            selecionadoId={discipuladoId}
            onSelecionar={setDiscipuladoId}
          />
        )}
        {!carregando && dados && dados.discipulados.length > 0 && recorte === 'sexo' && (
          <VisaoComparativa
            titulo="Por sexo do discipulado"
            series={['MASCULINO', 'FEMININO'].map((sexo) => {
              const itens = dados.discipulados.filter((item) => item.sexo === sexo)
              return {
                chave: sexo,
                nome: sexoLabel(sexo),
                frequencia: somarFrequencias(itens),
                discipulos: somarQuantidades(itens),
              }
            })}
            inicio={dados.dataInicio}
            fim={dados.dataFim}
          />
        )}
        {!carregando && dados && dados.discipulados.length > 0 && recorte === 'faixa' && (
          <VisaoComparativa
            titulo="Por faixa etária"
            series={(Object.keys(faixaEtariaLabel) as FaixaEtaria[]).map((faixa) => {
              const itens = dados.discipulados.filter((item) => item.faixaEtaria === faixa)
              return {
                chave: faixa,
                nome: faixaEtariaLabel[faixa],
                frequencia: somarFrequencias(itens),
                discipulos: somarQuantidades(itens),
              }
            })}
            inicio={dados.dataInicio}
            fim={dados.dataFim}
          />
        )}
        {!carregando && dados && dados.discipulados.length > 0 && recorte === 'gerencia' && gerenciaSelecionada && (
          <VisaoGerencia
            gerencias={gerencias}
            selecionada={gerenciaSelecionada}
            onSelecionar={setGerenciaId}
            inicio={dados.dataInicio}
            fim={dados.dataFim}
          />
        )}
      </Stack>
    </Box>
  )
}

function VisaoDiscipulado({
  dados,
  selecionado,
  selecionadoId,
  onSelecionar,
}: {
  dados: PainelDesempenhoResponse
  selecionado?: DiscipuladoDesempenho
  selecionadoId: number
  onSelecionar: (id: number) => void
}) {
  return (
    <SectionCard
      title="Detalhe do discipulado"
      action={
        <FormControl sx={{ minWidth: { xs: '100%', sm: 340 }, width: { xs: '100%', sm: 'auto' } }}>
          <InputLabel id="desempenho-discipulado-label">Discipulado</InputLabel>
          <Select
            labelId="desempenho-discipulado-label"
            value={selecionadoId || ''}
            label="Discipulado"
            onChange={(event) => onSelecionar(Number(event.target.value))}
          >
            {dados.discipulados.map((item) => (
              <MenuItem value={item.id} key={item.id}>
                {labelDesempenho(item)}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      }
    >
      {selecionado ? (
        <Stack spacing={3}>
          <Typography variant="body2" color="text.secondary">
            {sexoLabel(selecionado.sexo)} · {faixaEtariaLabel[selecionado.faixaEtaria]} · {selecionado.gerenciaNome}
            {selecionado.ativo ? '' : ' · Inativo'}
          </Typography>
          <PainelFrequencia
            titulo={`Presentes e ausentes — ${selecionado.nome}`}
            inicio={dados.dataInicio}
            fim={dados.dataFim}
            frequencia={selecionado.frequencia}
          />
          <PainelQuantidade
            titulo={`Quantidade de pessoas no grupo — ${selecionado.nome}`}
            inicio={dados.dataInicio}
            fim={dados.dataFim}
            discipulos={selecionado.discipulos}
          />
        </Stack>
      ) : (
        <Alert severity="info">Selecione um discipulado para ver o desempenho.</Alert>
      )}
    </SectionCard>
  )
}

function VisaoComparativa({
  titulo,
  series,
  inicio,
  fim,
}: {
  titulo: string
  series: Array<{
    chave: string
    nome: string
    frequencia: FrequenciaMensalDesempenho[]
    discipulos: QuantidadeMensalDesempenho[]
  }>
  inicio: string
  fim: string
}) {
  const referencias = referenciasPeriodo(inicio, fim)
  return (
    <Stack spacing={3}>
      <AnalyticsCard
        title={`Presentes e ausentes — ${titulo}`}
        description="Barras agrupadas por mês para cada valor do recorte."
        chart={<GraficoFrequenciaComparativa series={series} referencias={referencias} />}
        table={<TabelaFrequenciaComparativa series={series} referencias={referencias} />}
      />
      <AnalyticsCard
        title={`Quantidade de pessoas — ${titulo}`}
        description="Vínculos vigentes no último dia de cada mês (discípulo, visitante e GOE)."
        chart={<GraficoQuantidadeComparativa series={series} referencias={referencias} />}
        table={<TabelaQuantidadeComparativa series={series} referencias={referencias} />}
      />
    </Stack>
  )
}

function VisaoGerencia({
  gerencias,
  selecionada,
  onSelecionar,
  inicio,
  fim,
}: {
  gerencias: Array<{ id: number; nome: string; itens: DiscipuladoDesempenho[] }>
  selecionada: { id: number; nome: string; itens: DiscipuladoDesempenho[] }
  onSelecionar: (id: number) => void
  inicio: string
  fim: string
}) {
  const comparativo = gerencias.map((item) => ({
    id: item.id,
    nome: item.nome,
    ...totaisFrequencia(item.itens),
  }))
  return (
    <Stack spacing={3}>
      <AnalyticsCard
        title="Totais do período por gerência"
        description="Soma de presentes e ausentes no período selecionado."
        chart={<GraficoTotaisGerencia dados={comparativo} />}
        table={
          <DataTableCard>
            <Table size="small" aria-label="Totais por gerência">
              <TableHead>
                <TableRow>
                  <TableCell>Gerência</TableCell>
                  <TableCell>Presentes</TableCell>
                  <TableCell>Ausentes</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {comparativo.map((item) => (
                  <TableRow key={item.id} hover selected={item.id === selecionada.id}>
                    <TableCell component="th" scope="row">
                      {item.nome}
                    </TableCell>
                    <TableCell>{item.presentes}</TableCell>
                    <TableCell>{item.ausentes}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </DataTableCard>
        }
      />
      <SectionCard
        title="Detalhe da gerência"
        action={
          <FormControl sx={{ minWidth: { xs: '100%', sm: 340 }, width: { xs: '100%', sm: 'auto' } }}>
            <InputLabel id="desempenho-gerencia-label">Gerência</InputLabel>
            <Select
              labelId="desempenho-gerencia-label"
              value={selecionada.id}
              label="Gerência"
              onChange={(event) => onSelecionar(Number(event.target.value))}
            >
              {gerencias.map((item) => (
                <MenuItem value={item.id} key={item.id}>
                  {item.nome}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        }
      >
        <Stack spacing={3}>
          <PainelFrequencia
            titulo={`Presentes e ausentes — ${selecionada.nome}`}
            inicio={inicio}
            fim={fim}
            frequencia={somarFrequencias(selecionada.itens)}
          />
          <PainelQuantidade
            titulo={`Quantidade de pessoas — ${selecionada.nome}`}
            inicio={inicio}
            fim={fim}
            discipulos={somarQuantidades(selecionada.itens)}
          />
        </Stack>
      </SectionCard>
    </Stack>
  )
}

function PainelFrequencia({
  titulo,
  inicio,
  fim,
  frequencia,
}: {
  titulo: string
  inicio: string
  fim: string
  frequencia: FrequenciaMensalDesempenho[]
}) {
  const dados = normalizarFrequencia(inicio, fim, frequencia)
  return (
    <AnalyticsCard
      title={titulo}
      description="Somente encontros realizados. Visitantes não entram nesta série."
      chart={<GraficoFrequencia dados={dados} />}
      table={<TabelaFrequencia dados={dados} />}
    />
  )
}

function PainelQuantidade({
  titulo,
  inicio,
  fim,
  discipulos,
}: {
  titulo: string
  inicio: string
  fim: string
  discipulos: QuantidadeMensalDesempenho[]
}) {
  const dados = normalizarQuantidade(inicio, fim, discipulos)
  return (
    <AnalyticsCard
      title={titulo}
      description="Contagem de vínculos vigentes no último dia de cada mês (discípulo, visitante e GOE)."
      chart={<GraficoQuantidade dados={dados} />}
      table={<TabelaQuantidade dados={dados} />}
    />
  )
}

function GraficoFrequencia({ dados }: { dados: Array<FrequenciaMensalDesempenho & { possuiEncontro: boolean }> }) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const colors = useChartColors()
  const labels = dados.map((item) => formatarMes(item.referencia))
  return (
    <Box role="img" aria-label="Gráfico de barras de presentes e ausentes por mês." sx={{ minWidth: 0 }}>
      <ReactECharts
        style={{ height: mobile ? 300 : 340, width: '100%' }}
        option={{
          aria: { enabled: true },
          textStyle: { color: colors.text },
          tooltip: { trigger: 'axis' },
          legend: {
            top: 0,
            left: 'center',
            textStyle: legendTextStyle(colors, mobile ? 11 : 12),
            data: ['Presentes', 'Ausentes'],
          },
          grid: { top: 48, left: mobile ? 12 : 40, right: 16, bottom: 36, containLabel: true },
          xAxis: {
            type: 'category',
            data: labels,
            axisLabel: axisLabelStyle(colors, mobile ? 11 : 12),
            axisLine: { lineStyle: { color: colors.axisLine } },
          },
          yAxis: {
            type: 'value',
            minInterval: 1,
            axisLabel: axisLabelStyle(colors, mobile ? 11 : 12),
            splitLine: { lineStyle: { color: colors.splitLine } },
          },
          series: [
            {
              name: 'Presentes',
              type: 'bar',
              data: dados.map((item) => (item.possuiEncontro ? item.presentes : null)),
              itemStyle: { color: colors.success, borderRadius: [4, 4, 0, 0] },
            },
            {
              name: 'Ausentes',
              type: 'bar',
              data: dados.map((item) => (item.possuiEncontro ? item.ausentes : null)),
              itemStyle: { color: colors.error, borderRadius: [4, 4, 0, 0] },
            },
          ],
        }}
      />
    </Box>
  )
}

function TabelaFrequencia({ dados }: { dados: Array<FrequenciaMensalDesempenho & { possuiEncontro: boolean }> }) {
  return (
    <DataTableCard>
      <Table size="small" aria-label="Resumo mensal de presença e ausência">
        <TableHead>
          <TableRow>
            <TableCell>Mês</TableCell>
            <TableCell>Discípulos</TableCell>
            <TableCell>Visitantes</TableCell>
            <TableCell>Discípulos GOE</TableCell>
            <TableCell>Ausentes</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {dados.map((item) => (
            <TableRow key={item.referencia}>
              <TableCell>{formatarMes(item.referencia)}</TableCell>
              <TableCell>{item.possuiEncontro ? item.presentesDiscipulos : '—'}</TableCell>
              <TableCell>{item.possuiEncontro ? item.presentesVisitantes : '—'}</TableCell>
              <TableCell>{item.possuiEncontro ? item.presentesGoe : '—'}</TableCell>
              <TableCell>{item.possuiEncontro ? item.ausentes : '—'}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </DataTableCard>
  )
}

function GraficoQuantidade({ dados }: { dados: QuantidadeMensalDesempenho[] }) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const colors = useChartColors()
  return (
    <Box role="img" aria-label="Gráfico de linha da quantidade de pessoas no grupo por mês." sx={{ minWidth: 0 }}>
      <ReactECharts
        style={{ height: mobile ? 280 : 320, width: '100%' }}
        option={{
          aria: { enabled: true },
          textStyle: { color: colors.text },
          tooltip: { trigger: 'axis' },
          grid: { top: 24, left: mobile ? 12 : 40, right: 16, bottom: 36, containLabel: true },
          xAxis: {
            type: 'category',
            data: dados.map((item) => formatarMes(item.referencia)),
            axisLabel: axisLabelStyle(colors, mobile ? 11 : 12),
            axisLine: { lineStyle: { color: colors.axisLine } },
          },
          yAxis: {
            type: 'value',
            minInterval: 1,
            axisLabel: axisLabelStyle(colors, mobile ? 11 : 12),
            splitLine: { lineStyle: { color: colors.splitLine } },
          },
          series: [
            {
              name: 'Quantidade',
              type: 'line',
              smooth: true,
              data: dados.map((item) => item.quantidade),
              itemStyle: { color: colors.primary },
              areaStyle: { color: colors.primaryLight, opacity: 0.12 },
              label: {
                show: !mobile,
                ...seriesLabelStyle(colors, 11),
              },
            },
          ],
        }}
      />
    </Box>
  )
}

function TabelaQuantidade({ dados }: { dados: QuantidadeMensalDesempenho[] }) {
  return (
    <DataTableCard>
      <Table size="small" aria-label="Resumo mensal da quantidade de pessoas">
        <TableHead>
          <TableRow>
            <TableCell>Mês</TableCell>
            <TableCell>Quantidade</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {dados.map((item) => (
            <TableRow key={item.referencia}>
              <TableCell>{formatarMes(item.referencia)}</TableCell>
              <TableCell>{item.quantidade}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </DataTableCard>
  )
}

function GraficoFrequenciaComparativa({
  series,
  referencias,
}: {
  series: Array<{ nome: string; frequencia: FrequenciaMensalDesempenho[] }>
  referencias: string[]
}) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const colors = useChartColors()
  const paleta = [colors.primary, colors.secondary, colors.success, colors.warning, colors.error, colors.primaryLight]
  return (
    <Box role="img" aria-label="Gráfico comparativo de presentes por recorte." sx={{ minWidth: 0 }}>
      <ReactECharts
        style={{ height: mobile ? 320 : 360, width: '100%' }}
        option={{
          aria: { enabled: true },
          textStyle: { color: colors.text },
          tooltip: { trigger: 'axis' },
          legend: {
            top: 0,
            left: 'center',
            width: mobile ? '92%' : undefined,
            textStyle: legendTextStyle(colors, mobile ? 11 : 12),
          },
          grid: { top: 56, left: mobile ? 12 : 40, right: 16, bottom: 36, containLabel: true },
          xAxis: {
            type: 'category',
            data: referencias.map(formatarMes),
            axisLabel: axisLabelStyle(colors, mobile ? 11 : 12),
          },
          yAxis: {
            type: 'value',
            minInterval: 1,
            axisLabel: axisLabelStyle(colors, mobile ? 11 : 12),
            splitLine: { lineStyle: { color: colors.splitLine } },
          },
          series: series.flatMap((item, index) => {
            const mapa = new Map(item.frequencia.map((mes) => [mes.referencia, mes]))
            const cor = paleta[index % paleta.length]
            return [
              {
                name: `${item.nome} · presentes`,
                type: 'bar',
                data: referencias.map((referencia) => mapa.get(referencia)?.presentes ?? null),
                itemStyle: { color: cor },
              },
              {
                name: `${item.nome} · ausentes`,
                type: 'bar',
                data: referencias.map((referencia) => mapa.get(referencia)?.ausentes ?? null),
                itemStyle: { color: cor, opacity: 0.45 },
              },
            ]
          }),
        }}
      />
    </Box>
  )
}

function TabelaFrequenciaComparativa({
  series,
  referencias,
}: {
  series: Array<{ nome: string; frequencia: FrequenciaMensalDesempenho[] }>
  referencias: string[]
}) {
  return (
    <DataTableCard>
      <Table size="small" aria-label="Comparativo mensal de presença por recorte">
        <TableHead>
          <TableRow>
            <TableCell>Mês</TableCell>
            {series.map((item) => (
              <TableCell key={item.nome}>{item.nome}</TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {referencias.map((referencia) => (
            <TableRow key={referencia}>
              <TableCell>{formatarMes(referencia)}</TableCell>
              {series.map((item) => {
                const mes = item.frequencia.find((entrada) => entrada.referencia === referencia)
                return <TableCell key={item.nome}>{mes ? `${mes.presentes} / ${mes.ausentes}` : '—'}</TableCell>
              })}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </DataTableCard>
  )
}

function GraficoQuantidadeComparativa({
  series,
  referencias,
}: {
  series: Array<{ nome: string; discipulos: QuantidadeMensalDesempenho[] }>
  referencias: string[]
}) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const colors = useChartColors()
  const paleta = [colors.primary, colors.secondary, colors.success, colors.warning, colors.error, colors.primaryLight]
  return (
    <Box role="img" aria-label="Gráfico comparativo da quantidade de pessoas por recorte." sx={{ minWidth: 0 }}>
      <ReactECharts
        style={{ height: mobile ? 300 : 340, width: '100%' }}
        option={{
          aria: { enabled: true },
          textStyle: { color: colors.text },
          tooltip: { trigger: 'axis' },
          legend: {
            top: 0,
            left: 'center',
            textStyle: legendTextStyle(colors, mobile ? 11 : 12),
          },
          grid: { top: 48, left: mobile ? 12 : 40, right: 16, bottom: 36, containLabel: true },
          xAxis: {
            type: 'category',
            data: referencias.map(formatarMes),
            axisLabel: axisLabelStyle(colors, mobile ? 11 : 12),
          },
          yAxis: {
            type: 'value',
            minInterval: 1,
            axisLabel: axisLabelStyle(colors, mobile ? 11 : 12),
            splitLine: { lineStyle: { color: colors.splitLine } },
          },
          series: series.map((item, index) => {
            const mapa = new Map(item.discipulos.map((mes) => [mes.referencia, mes.quantidade]))
            return {
              name: item.nome,
              type: 'line',
              smooth: true,
              data: referencias.map((referencia) => mapa.get(referencia) ?? 0),
              itemStyle: { color: paleta[index % paleta.length] },
            }
          }),
        }}
      />
    </Box>
  )
}

function TabelaQuantidadeComparativa({
  series,
  referencias,
}: {
  series: Array<{ nome: string; discipulos: QuantidadeMensalDesempenho[] }>
  referencias: string[]
}) {
  return (
    <DataTableCard>
      <Table size="small" aria-label="Comparativo mensal da quantidade de pessoas">
        <TableHead>
          <TableRow>
            <TableCell>Mês</TableCell>
            {series.map((item) => (
              <TableCell key={item.nome}>{item.nome}</TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {referencias.map((referencia) => (
            <TableRow key={referencia}>
              <TableCell>{formatarMes(referencia)}</TableCell>
              {series.map((item) => {
                const mes = item.discipulos.find((entrada) => entrada.referencia === referencia)
                return <TableCell key={item.nome}>{mes?.quantidade ?? 0}</TableCell>
              })}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </DataTableCard>
  )
}

function GraficoTotaisGerencia({
  dados,
}: {
  dados: Array<{ id: number; nome: string; presentes: number; ausentes: number }>
}) {
  const mobile = useMediaQuery('(max-width:599.95px)')
  const colors = useChartColors()
  return (
    <Box role="img" aria-label="Gráfico de barras dos totais por gerência." sx={{ minWidth: 0 }}>
      <ReactECharts
        style={{
          height: Math.min(mobile ? 420 : 520, Math.max(mobile ? 240 : 280, dados.length * (mobile ? 44 : 48))),
          width: '100%',
        }}
        option={{
          aria: { enabled: true },
          textStyle: { color: colors.text },
          tooltip: { trigger: 'axis' },
          legend: {
            top: 0,
            left: 'center',
            textStyle: legendTextStyle(colors, mobile ? 11 : 12),
            data: ['Presentes', 'Ausentes'],
          },
          grid: { top: 40, left: 8, right: mobile ? 24 : 40, bottom: 16, containLabel: true },
          xAxis: {
            type: 'value',
            minInterval: 1,
            axisLabel: axisLabelStyle(colors, mobile ? 11 : 12),
            splitLine: { lineStyle: { color: colors.splitLine } },
          },
          yAxis: {
            type: 'category',
            data: dados.map((item) => item.nome),
            axisLabel: {
              ...axisLabelStyle(colors, mobile ? 11 : 12),
              width: mobile ? 100 : 140,
              overflow: 'truncate',
            },
          },
          series: [
            {
              name: 'Presentes',
              type: 'bar',
              stack: 'total',
              data: dados.map((item) => item.presentes),
              itemStyle: { color: colors.success },
            },
            {
              name: 'Ausentes',
              type: 'bar',
              stack: 'total',
              data: dados.map((item) => item.ausentes),
              itemStyle: { color: colors.error },
            },
          ],
        }}
      />
    </Box>
  )
}
