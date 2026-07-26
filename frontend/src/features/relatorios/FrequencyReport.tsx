import { PrintRounded, SearchRounded } from '@mui/icons-material'
import {
  Alert,
  Box,
  Button,
  Chip,
  FormControl,
  GlobalStyles,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useEffect, useState } from 'react'

import { organizationApi } from '@/features/organizacao/api'
import { relatorioApi, type RelatorioEncontro, type RelatorioPeriodoResponse } from '@/features/relatorios/api'
import type { Discipulado, Usuario } from '@/shared/api/types'
import { FilterToolbar, PageHeader } from '@/shared/ui'

export default function FrequencyReport({ currentUser }: { currentUser: Usuario }) {
  const podeFiltrarDiscipulado = currentUser.perfis.includes('GERENTE') || currentUser.perfis.includes('ADMIN')
  const hoje = dataAtual()
  const [dataInicio, setDataInicio] = useState(hoje)
  const [dataFim, setDataFim] = useState(hoje)
  const [discipuladoId, setDiscipuladoId] = useState('')
  const [discipulados, setDiscipulados] = useState<Discipulado[]>([])
  const [dados, setDados] = useState<RelatorioPeriodoResponse>()
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)

  useEffect(() => {
    if (!podeFiltrarDiscipulado) return
    let ativo = true
    void organizationApi
      .listarDiscipulados()
      .then((pagina) => {
        if (ativo) setDiscipulados(pagina.content)
      })
      .catch(() => {
        if (ativo) setDiscipulados([])
      })
    return () => {
      ativo = false
    }
  }, [podeFiltrarDiscipulado])

  async function consultar(event: FormEvent) {
    event.preventDefault()
    setErro('')
    if (dataInicio > dataFim) {
      setDados(undefined)
      setErro('A data inicial não pode ser posterior à data final.')
      return
    }
    if (dataFim > somarMeses(dataInicio, 12)) {
      setDados(undefined)
      setErro('O período do relatório deve ser de no máximo 12 meses.')
      return
    }
    setCarregando(true)
    try {
      const filtroDiscipulado = discipuladoId ? Number(discipuladoId) : undefined
      setDados(await relatorioApi.consultarFrequencia(dataInicio, dataFim, filtroDiscipulado))
    } catch (e) {
      setDados(undefined)
      setErro(e instanceof Error ? e.message : 'Não foi possível consultar o relatório.')
    } finally {
      setCarregando(false)
    }
  }
  return (
    <Stack spacing={3}>
      <GlobalStyles
        styles={{
          '@page': { size: 'A4 portrait', margin: '12mm' },
          '@media print': {
            'body *': { visibility: 'hidden' },
            '#relatorio-frequencia-impressao, #relatorio-frequencia-impressao *': { visibility: 'visible' },
            '#relatorio-frequencia-impressao': { position: 'absolute', inset: 0, width: '100%' },
            '.relatorio-frequencia-pagina': {
              boxShadow: 'none !important',
              margin: 0,
              padding: '0 !important',
              maxWidth: 'none !important',
              breakAfter: 'page',
              pageBreakAfter: 'always',
            },
            '.relatorio-frequencia-pagina:last-child': { breakAfter: 'auto', pageBreakAfter: 'auto' },
            '.frequencia-presente': { fontWeight: '700 !important' },
            '.frequencia-ausente': { fontWeight: '400 !important' },
          },
        }}
      />
      <PageHeader
        title="Relatórios de frequência"
        description="Consulte o histórico de encontros realizados e de dias em que o discipulado não ocorreu."
        eyebrow="Análises"
      />
      <FilterToolbar component="form" onSubmit={consultar}>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1.5}
          alignItems={{ sm: 'center' }}
          flexWrap="wrap"
          useFlexGap
        >
          <TextField
            required
            type="date"
            label="Data inicial"
            value={dataInicio}
            onChange={(e) => setDataInicio(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          <TextField
            required
            type="date"
            label="Data final"
            value={dataFim}
            onChange={(e) => setDataFim(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          {podeFiltrarDiscipulado && (
            <FormControl sx={{ minWidth: 220 }}>
              <InputLabel id="relatorio-discipulado-label">Discipulado</InputLabel>
              <Select
                labelId="relatorio-discipulado-label"
                label="Discipulado"
                value={discipuladoId}
                onChange={(event) => setDiscipuladoId(event.target.value)}
              >
                <MenuItem value="">Todos</MenuItem>
                {discipulados.map((item) => (
                  <MenuItem key={item.id} value={String(item.id)}>
                    {item.nome}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}
          <Button type="submit" variant="contained" startIcon={<SearchRounded />} disabled={carregando}>
            {carregando ? 'Consultando...' : 'Consultar'}
          </Button>
          <Button
            variant="outlined"
            startIcon={<PrintRounded />}
            disabled={!dados?.relatorios.length}
            onClick={() => window.print()}
          >
            Imprimir / salvar como PDF
          </Button>
        </Stack>
      </FilterToolbar>
      {erro && <Alert severity="error">{erro}</Alert>}
      {dados && dados.relatorios.length === 0 && (
        <Alert severity="info">
          Não há registros de frequência no seu escopo no período de {formatarData(dados.dataInicio)} a{' '}
          {formatarData(dados.dataFim)}.
        </Alert>
      )}
      {dados?.relatorios.length ? (
        <Box id="relatorio-frequencia-impressao">
          <Stack spacing={3}>
            {dados.relatorios.map((item) => (
              <PaginaRelatorio key={item.encontroId} item={item} emitidoEm={dados.emitidoEm} />
            ))}
          </Stack>
        </Box>
      ) : null}
    </Stack>
  )
}

function PaginaRelatorio({ item, emitidoEm }: { item: RelatorioEncontro; emitidoEm: string }) {
  const coLideres = item.coLideres.map((l) => l.nome).join(', ') || 'Nenhum'
  const naoRealizado = item.situacao === 'NAO_REALIZADO'
  return (
    <Paper
      className="relatorio-frequencia-pagina"
      sx={{
        p: { xs: 2, sm: 4 },
        mx: 'auto',
        width: '100%',
        maxWidth: '190mm',
        bgcolor: 'background.paper',
        color: 'text.primary',
      }}
    >
      <Stack spacing={2}>
        <Box textAlign="center">
          <Typography variant="overline">SGD — Sistema de Gerenciamento de Discipulados</Typography>
          <Typography component="h2" variant="h4">
            {naoRealizado ? 'Registro de ausência do discipulado' : 'Relatório de frequência'}
          </Typography>
        </Box>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 1 }}>
          <Typography>
            <strong>Data:</strong> {formatarData(item.data)}
          </Typography>
          <Typography>
            <strong>Gerência:</strong> {item.gerencia.nome}
          </Typography>
          <Typography>
            <strong>Discipulado:</strong> {item.discipulado.nome}
          </Typography>
          <Typography>
            <strong>Discipulador:</strong> {item.discipulador.nome}
          </Typography>
          <Typography>
            <strong>Co-líderes:</strong> {coLideres}
          </Typography>
          <Typography>
            <strong>Situação:</strong> {naoRealizado ? 'Não houve discipulado' : 'Houve discipulado'}
          </Typography>
        </Box>
        {naoRealizado ? (
          <Alert severity="warning">
            <strong>Justificativa:</strong> {item.justificativa || 'Não informada'}
          </Alert>
        ) : (
          <TableContainer>
            <Table size="small" aria-label={`Frequência do ${item.discipulado.nome} em ${formatarData(item.data)}`}>
              <TableHead>
                <TableRow>
                  <TableCell>Adolescente</TableCell>
                  <TableCell>Telefone</TableCell>
                  <TableCell>Data do encontro</TableCell>
                  <TableCell align="right">Frequência</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {item.participantes.length ? (
                  item.participantes.map((p) => (
                    <TableRow key={p.adolescenteId}>
                      <TableCell>{p.nome}</TableCell>
                      <TableCell>{p.telefone || 'Não informado'}</TableCell>
                      <TableCell>{formatarData(item.data)}</TableCell>
                      <TableCell align="right">
                        <Chip
                          size="small"
                          color={p.situacao === 'PRESENTE' ? 'success' : 'default'}
                          label={
                            <Box
                              component="span"
                              className={p.situacao === 'PRESENTE' ? 'frequencia-presente' : 'frequencia-ausente'}
                            >
                              {p.situacao === 'PRESENTE' ? 'Presente' : 'Ausente'}
                            </Box>
                          }
                        />
                      </TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={4}>Nenhuma frequência registrada neste encontro.</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        )}
        {!naoRealizado && (
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr 1fr', sm: 'repeat(5,1fr)' },
              gap: 1,
              p: 2,
              border: '1px solid',
              borderColor: 'divider',
              borderRadius: 1,
            }}
          >
            <Resumo label="Presentes" valor={item.resumo.presentes} />
            <Resumo label="Ausentes" valor={item.resumo.ausentes} />
            <Resumo label="Participantes" valor={item.resumo.participantes} />
            <Resumo label="Visitantes" valor={item.visitantes} />
            <Resumo label="Presença" valor={percentual(item.resumo.percentualPresenca)} />
          </Box>
        )}
        <Typography variant="caption" color="text.secondary" textAlign="right">
          Emitido em {formatarDataHora(emitidoEm)}
        </Typography>
      </Stack>
    </Paper>
  )
}

function Resumo({ label, valor }: { label: string; valor: string | number }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography fontWeight={700}>{valor}</Typography>
    </Box>
  )
}
function percentual(valor: number) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'percent',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(valor / 100)
}
function formatarData(data: string) {
  return new Intl.DateTimeFormat('pt-BR').format(new Date(`${data}T12:00:00`))
}
function formatarDataHora(data: string) {
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: 'America/Sao_Paulo',
  }).format(new Date(data))
}
function dataAtual() {
  const partes = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date())
  const valor = (tipo: Intl.DateTimeFormatPartTypes) => partes.find((p) => p.type === tipo)?.value ?? ''
  return `${valor('year')}-${valor('month')}-${valor('day')}`
}
function somarMeses(data: string, meses: number) {
  const [ano, mes, dia] = data.split('-').map(Number)
  const base = new Date(Date.UTC(ano, mes - 1 + meses, 1))
  const ultimoDia = new Date(Date.UTC(base.getUTCFullYear(), base.getUTCMonth() + 1, 0)).getUTCDate()
  return `${base.getUTCFullYear()}-${String(base.getUTCMonth() + 1).padStart(2, '0')}-${String(Math.min(dia, ultimoDia)).padStart(2, '0')}`
}
