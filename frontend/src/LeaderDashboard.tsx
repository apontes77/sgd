import { Alert, Box, Chip, CircularProgress, Stack, Typography } from '@mui/material'
import { useEffect, useState } from 'react'
import { FiltroPeriodo, GraficoEvolucao, KpisPresenca, TabelaEvolucao } from './dashboardComponents'
import { normalizarMeses, periodoPadrao } from './dashboardUtils'
import { painelApi, type PainelLiderResponse } from './painelApi'

export default function LeaderDashboard() {
  const inicial = periodoPadrao()
  const [dataInicio, setDataInicio] = useState(inicial.inicio)
  const [dataFim, setDataFim] = useState(inicial.fim)
  const [periodo, setPeriodo] = useState(inicial)
  const [dados, setDados] = useState<PainelLiderResponse>()
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)
  useEffect(() => { let ativo = true; setCarregando(true); setErro(''); painelApi.consultarLider(periodo.inicio, periodo.fim).then((resposta) => { if (ativo) setDados(resposta) }).catch((error: Error) => { if (ativo) { setDados(undefined); setErro(error.message) } }).finally(() => { if (ativo) setCarregando(false) }); return () => { ativo = false } }, [periodo])
  const meses = dados ? normalizarMeses(dados.dataInicio, dados.dataFim, dados.evolucao) : []
  return <Stack spacing={3}>
    <Box><Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap"><Typography variant="h4">Meu discipulado</Typography>{dados && !dados.discipulado.ativo && <Chip color="warning" label="Inativo" size="small" />}</Stack><Typography color="text.secondary">{dados?.discipulado.nome ?? 'Histórico de presença do grupo em que você exerce liderança.'}</Typography></Box>
    <FiltroPeriodo dataInicio={dataInicio} dataFim={dataFim} onInicio={setDataInicio} onFim={setDataFim} onAplicar={() => setPeriodo({ inicio: dataInicio, fim: dataFim })} />
    {carregando && <Box role="status" sx={{ py: 8, textAlign: 'center' }}><CircularProgress /><Typography>Carregando histórico...</Typography></Box>}
    {erro && <Alert severity="error">{erro}</Alert>}
    {!carregando && dados && <><KpisPresenca resumo={dados.resumo} />{dados.resumo.encontrosRealizados === 0 && <Alert severity="info">Não há encontros realizados no período selecionado.</Alert>}<GraficoEvolucao titulo="Evolução mensal do discipulado" dados={meses} /><TabelaEvolucao titulo="Histórico mensal do discipulado" dados={meses} /></>}
  </Stack>
}
