import { SearchRounded } from '@mui/icons-material'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  LinearProgress,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useCallback, useEffect, useState } from 'react'

import {
  familiaApi,
  type FamiliaInput,
  familiaNaoConsta,
  type FamiliaResumo,
  SITUACAO_IGREJA_LABEL,
  SITUACAO_PAIS_LABEL,
  type SituacaoIgrejaFamilia,
  type SituacaoPaisFamilia,
} from '@/features/familia/api'
import { FamilyFormFields } from '@/features/familia/FamilyFormFields'
import type { Pagina } from '@/shared/api/types'
import { DataTableCard, EmptyState, FilterToolbar, PageHeader, SectionCard } from '@/shared/ui'

const PAGE_SIZE = 20
const SITUACOES_IGREJA = Object.keys(SITUACAO_IGREJA_LABEL) as SituacaoIgrejaFamilia[]
const SITUACOES_PAIS = Object.keys(SITUACAO_PAIS_LABEL) as SituacaoPaisFamilia[]

const paginaVazia: Pagina<FamiliaResumo> = {
  content: [],
  page: 0,
  size: PAGE_SIZE,
  totalElements: 0,
  totalPages: 0,
}

export default function FamilyDirectory() {
  const [resultado, setResultado] = useState<Pagina<FamiliaResumo>>(paginaVazia)
  const [busca, setBusca] = useState('')
  const [buscaAplicada, setBuscaAplicada] = useState('')
  const [situacaoIgreja, setSituacaoIgreja] = useState<SituacaoIgrejaFamilia | ''>('')
  const [situacaoPais, setSituacaoPais] = useState<SituacaoPaisFamilia | ''>('')
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [carregando, setCarregando] = useState(true)
  const [alvo, setAlvo] = useState<FamiliaResumo | null>(null)
  const [form, setForm] = useState<FamiliaInput>(familiaNaoConsta())

  const carregar = useCallback(
    async (page = 0) => {
      setCarregando(true)
      try {
        setErro('')
        setResultado(
          await familiaApi.listar(page, PAGE_SIZE, {
            busca: buscaAplicada,
            situacaoIgreja,
            situacaoPais,
          }),
        )
      } catch (e) {
        setResultado(paginaVazia)
        setErro(
          e instanceof Error
            ? e.message
            : 'Não foi possível carregar as famílias. Confirme que a API está na versão com ficha de família.',
        )
      } finally {
        setCarregando(false)
      }
    },
    [buscaAplicada, situacaoIgreja, situacaoPais],
  )

  useEffect(() => {
    void carregar(0)
  }, [carregar])

  function aplicarFiltros(event: FormEvent) {
    event.preventDefault()
    setBuscaAplicada(busca.trim())
  }

  async function abrir(item: FamiliaResumo) {
    setAlvo(item)
    setErro('')
    try {
      const ficha = await familiaApi.obter(item.adolescenteId)
      setForm({
        cep: ficha.cep,
        rua: ficha.rua,
        numero: ficha.numero,
        complemento: ficha.complemento,
        bairro: ficha.bairro,
        cidade: ficha.cidade,
        situacaoIgreja: ficha.situacaoIgreja,
        atuaOnde: ficha.atuaOnde,
        situacaoPais: ficha.situacaoPais,
        descricao: ficha.descricao,
        desafioFinanceiro: ficha.desafioFinanceiro,
        desafioEmocional: ficha.desafioEmocional,
        desafioEspiritual: ficha.desafioEspiritual,
        desafiosDescricao: ficha.desafiosDescricao,
        atividadesJuntas: ficha.atividadesJuntas,
        rotinaSemana: ficha.rotinaSemana,
        irmaoDokmos: ficha.irmaoDokmos,
        pedidoOracao: ficha.pedidoOracao,
        intervencao: ficha.intervencao,
        observacaoDiscipulador: ficha.observacaoDiscipulador,
        observacaoGerente: ficha.observacaoGerente,
        responsavel1: ficha.responsavel1,
        responsavel2: ficha.responsavel2,
      })
    } catch (e) {
      setForm(familiaNaoConsta())
      setErro(e instanceof Error ? e.message : 'Não foi possível carregar a ficha. Você ainda pode preencher e salvar.')
    }
  }

  async function salvar() {
    if (!alvo) return
    setSalvando(true)
    setErro('')
    try {
      await familiaApi.salvar(alvo.adolescenteId, form)
      setAlvo(null)
      setSucesso('Ficha de família atualizada.')
      await carregar(resultado.page)
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível salvar a ficha.')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <Stack spacing={3}>
      <PageHeader
        title="Famílias"
        description="Acompanhe e edite as fichas de família dos adolescentes no seu escopo (admin: todas; gerente: sua gerência)."
        eyebrow="Cadastros"
      />
      {erro && (
        <Alert severity="error" onClose={() => setErro('')}>
          {erro}
        </Alert>
      )}
      {sucesso && (
        <Alert severity="success" onClose={() => setSucesso('')}>
          {sucesso}
        </Alert>
      )}
      <FilterToolbar component="form" onSubmit={aplicarFiltros}>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1.5}
          alignItems={{ sm: 'center' }}
          flexWrap="wrap"
          useFlexGap
        >
          <TextField
            label="Nome ou discipulado"
            placeholder="Buscar por adolescente ou discipulado"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            sx={{ minWidth: { xs: '100%', sm: 260 }, flex: { sm: 1 } }}
          />
          <TextField
            select
            label="Situação na igreja"
            value={situacaoIgreja}
            onChange={(e) => setSituacaoIgreja(e.target.value as SituacaoIgrejaFamilia | '')}
            sx={{ minWidth: { xs: '100%', sm: 240 } }}
          >
            <MenuItem value="">Todas</MenuItem>
            {SITUACOES_IGREJA.map((valor) => (
              <MenuItem key={valor} value={valor}>
                {SITUACAO_IGREJA_LABEL[valor]}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            select
            label="Situação dos pais"
            value={situacaoPais}
            onChange={(e) => setSituacaoPais(e.target.value as SituacaoPaisFamilia | '')}
            sx={{ minWidth: { xs: '100%', sm: 200 } }}
          >
            <MenuItem value="">Todas</MenuItem>
            {SITUACOES_PAIS.map((valor) => (
              <MenuItem key={valor} value={valor}>
                {SITUACAO_PAIS_LABEL[valor]}
              </MenuItem>
            ))}
          </TextField>
          <Button type="submit" variant="contained" startIcon={<SearchRounded />} disabled={carregando}>
            Buscar
          </Button>
          <Typography variant="body2" color="text.secondary" sx={{ ml: { sm: 'auto' } }}>
            {resultado.totalElements} ficha{resultado.totalElements === 1 ? '' : 's'}
          </Typography>
        </Stack>
      </FilterToolbar>
      <SectionCard>
        <DataTableCard>
          {carregando && <LinearProgress />}
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Adolescente</TableCell>
                <TableCell>Discipulado</TableCell>
                <TableCell>Situação da ficha</TableCell>
                <TableCell>Situação na igreja</TableCell>
                <TableCell>Situação dos pais</TableCell>
                <TableCell align="right">Ações</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {resultado.content.map((item) => (
                <TableRow key={item.id} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight={650}>
                      {item.adolescenteNome}
                    </Typography>
                  </TableCell>
                  <TableCell>{item.discipuladoNome}</TableCell>
                  <TableCell>{item.situacaoFicha === 'PREENCHIDA' ? 'Preenchida' : 'Não consta'}</TableCell>
                  <TableCell>{SITUACAO_IGREJA_LABEL[item.situacaoIgreja]}</TableCell>
                  <TableCell>{SITUACAO_PAIS_LABEL[item.situacaoPais]}</TableCell>
                  <TableCell align="right">
                    <Button size="small" variant="outlined" onClick={() => void abrir(item)}>
                      Abrir ficha
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {!carregando && resultado.content.length === 0 && !erro && (
            <EmptyState
              title="Nenhuma ficha encontrada"
              description="Ajuste os filtros ou cadastre adolescentes em Cadastros > Adolescentes."
            />
          )}
        </DataTableCard>
      </SectionCard>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Button disabled={resultado.page === 0 || carregando} onClick={() => void carregar(resultado.page - 1)}>
          Anterior
        </Button>
        <Typography variant="body2" color="text.secondary">
          Página {resultado.page + 1} de {Math.max(1, resultado.totalPages)}
        </Typography>
        <Button
          disabled={resultado.page + 1 >= resultado.totalPages || carregando}
          onClick={() => void carregar(resultado.page + 1)}
        >
          Próxima
        </Button>
      </Stack>

      <Dialog open={Boolean(alvo)} onClose={() => !salvando && setAlvo(null)} fullWidth maxWidth="md">
        <DialogTitle>Família de {alvo?.adolescenteNome}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <FamilyFormFields value={form} onChange={setForm} disabled={salvando} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAlvo(null)}>Cancelar</Button>
          <Button variant="contained" disabled={salvando} onClick={() => void salvar()}>
            {salvando ? 'Salvando...' : 'Salvar'}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
