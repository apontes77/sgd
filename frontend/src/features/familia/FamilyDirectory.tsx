import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { useCallback, useEffect, useState } from 'react'

import { familiaApi, type FamiliaInput, familiaNaoConsta, type FamiliaResumo } from '@/features/familia/api'
import { FamilyFormFields } from '@/features/familia/FamilyFormFields'
import { DataTableCard, EmptyState, PageHeader, SectionCard } from '@/shared/ui'

const PAGE_SIZE = 100

export default function FamilyDirectory() {
  const [itens, setItens] = useState<FamiliaResumo[]>([])
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [carregando, setCarregando] = useState(true)
  const [alvo, setAlvo] = useState<FamiliaResumo | null>(null)
  const [form, setForm] = useState<FamiliaInput>(familiaNaoConsta())

  const carregar = useCallback(async () => {
    setCarregando(true)
    try {
      setErro('')
      const todos: FamiliaResumo[] = []
      let page = 0
      let totalPages = 1
      while (page < totalPages) {
        const pagina = await familiaApi.listar(page, PAGE_SIZE)
        todos.push(...pagina.content)
        totalPages = Math.max(pagina.totalPages, 1)
        page += 1
        if (page > 50) break
      }
      setItens(todos)
    } catch (e) {
      setItens([])
      setErro(
        e instanceof Error
          ? e.message
          : 'Não foi possível carregar as famílias. Confirme que a API está na versão com ficha de família.',
      )
    } finally {
      setCarregando(false)
    }
  }, [])

  useEffect(() => {
    void carregar()
  }, [carregar])

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
      await carregar()
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
      <SectionCard>
        <DataTableCard>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Adolescente</TableCell>
                <TableCell>Discipulado</TableCell>
                <TableCell>Situação da ficha</TableCell>
                <TableCell align="right">Ações</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {itens.map((item) => (
                <TableRow key={item.id} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight={650}>
                      {item.adolescenteNome}
                    </Typography>
                  </TableCell>
                  <TableCell>{item.discipuladoNome}</TableCell>
                  <TableCell>{item.situacaoFicha === 'PREENCHIDA' ? 'Preenchida' : 'Não consta'}</TableCell>
                  <TableCell align="right">
                    <Button size="small" variant="outlined" onClick={() => void abrir(item)}>
                      Abrir ficha
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {!carregando && itens.length === 0 && !erro && (
            <EmptyState
              title="Nenhuma ficha de família"
              description="Cadastre adolescentes em Cadastros > Adolescentes para gerar as fichas 1:1."
            />
          )}
          {carregando && (
            <Typography variant="body2" color="text.secondary" sx={{ p: 2 }}>
              Carregando fichas…
            </Typography>
          )}
        </DataTableCard>
      </SectionCard>

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
