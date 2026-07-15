import { Alert, Chip, Stack } from '@mui/material'
import { useEffect, useState } from 'react'
import { FiltroPeriodo, KpisPresenca, PainelEvolucao } from './dashboardComponents'
import { normalizarMeses, periodoPadrao } from './dashboardUtils'
import { painelApi, type PainelLiderResponse } from './painelApi'
import { LoadingState, PageHeader } from './ui'

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
    <PageHeader title="Meu discipulado" description={dados?.discipulado.nome ?? 'Histórico de presença do grupo em que você exerce liderança.'} eyebrow="Visão da liderança" action={dados && !dados.discipulado.ativo ? <Chip color="warning" label="Inativo" size="small" /> : undefined} />
    <FiltroPeriodo dataInicio={dataInicio} dataFim={dataFim} onInicio={setDataInicio} onFim={setDataFim} onAplicar={() => setPeriodo({ inicio: dataInicio, fim: dataFim })} />
    {carregando && <LoadingState label="Carregando histórico..." />}
    {erro && <Alert severity="error">{erro}</Alert>}
    {!carregando && dados && <><KpisPresenca resumo={dados.resumo} />{dados.resumo.encontrosRealizados === 0 && <Alert severity="info">Não há encontros realizados no período selecionado.</Alert>}<PainelEvolucao titulo="Evolução mensal do discipulado" tabelaTitulo="Histórico mensal do discipulado" dados={meses} /></>}
  </Stack>
}
