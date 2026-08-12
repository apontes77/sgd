import { DownloadRounded } from '@mui/icons-material'
import { Alert, Autocomplete, Button, FormControl, InputLabel, MenuItem, Select, Stack, TextField } from '@mui/material'
import type { FormEvent } from 'react'
import { useEffect, useMemo, useState } from 'react'

import { organizationApi } from '@/features/organizacao/api'
import { type FiltroAtivoExport, relatorioApi } from '@/features/relatorios/api'
import { type Discipulado, labelDiscipulado } from '@/shared/api/types'
import { FilterToolbar, PageHeader, SectionCard } from '@/shared/ui'

type OpcaoDiscipulado = Discipulado | { id: 0; nome: string }

const OPCAO_TODOS: OpcaoDiscipulado = { id: 0, nome: 'Todos' }

const STATUS_OPCOES: { value: FiltroAtivoExport; label: string }[] = [
  { value: 'ativos', label: 'Ativos' },
  { value: 'inativos', label: 'Inativos' },
  { value: 'todos', label: 'Todos' },
]

export default function AdolescentExportReport() {
  const [discipuladoId, setDiscipuladoId] = useState(0)
  const [discipulados, setDiscipulados] = useState<Discipulado[]>([])
  const [status, setStatus] = useState<FiltroAtivoExport>('ativos')
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [carregando, setCarregando] = useState(false)

  const opcoesDiscipulado = useMemo<OpcaoDiscipulado[]>(() => [OPCAO_TODOS, ...discipulados], [discipulados])
  const discipuladoSelecionado = useMemo(
    () => opcoesDiscipulado.find((item) => item.id === discipuladoId) ?? OPCAO_TODOS,
    [opcoesDiscipulado, discipuladoId],
  )

  useEffect(() => {
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
  }, [])

  async function baixar(event: FormEvent) {
    event.preventDefault()
    setErro('')
    setSucesso('')
    setCarregando(true)
    try {
      await relatorioApi.exportarAdolescentes({
        discipuladoId: discipuladoId || undefined,
        ativo: status,
      })
      setSucesso('Planilha gerada. O download deve iniciar automaticamente.')
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível exportar a planilha.')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <Stack spacing={3}>
      <PageHeader
        title="Relatório de adolescentes"
        description="Exporte os dados cadastrais em planilha de todos os discipulados ou de um discipulado específico."
        eyebrow="Análises"
      />
      <FilterToolbar component="form" onSubmit={baixar}>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1.5}
          alignItems={{ sm: 'center' }}
          flexWrap="wrap"
          useFlexGap
        >
          <Autocomplete
            sx={{ minWidth: { xs: '100%', sm: 280 }, width: { xs: '100%', sm: 'auto' }, flex: { sm: 1 } }}
            options={opcoesDiscipulado}
            value={discipuladoSelecionado}
            onChange={(_, value) => setDiscipuladoId(value?.id ?? 0)}
            getOptionLabel={(item) => (item.id === 0 ? item.nome : labelDiscipulado(item))}
            isOptionEqualToValue={(option, value) => option.id === value.id}
            noOptionsText="Nenhum discipulado encontrado"
            renderInput={(params) => (
              <TextField {...params} label="Discipulado" placeholder="Pesquisar discipulado ou discipulador" />
            )}
          />
          <FormControl sx={{ minWidth: { xs: '100%', sm: 160 } }}>
            <InputLabel id="export-status-label">Status</InputLabel>
            <Select
              labelId="export-status-label"
              label="Status"
              value={status}
              onChange={(e) => setStatus(e.target.value as FiltroAtivoExport)}
            >
              {STATUS_OPCOES.map((opcao) => (
                <MenuItem key={opcao.value} value={opcao.value}>
                  {opcao.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button
            type="submit"
            variant="contained"
            startIcon={<DownloadRounded />}
            disabled={carregando}
            fullWidth
            sx={{ width: { xs: '100%', sm: 'auto' } }}
          >
            {carregando ? 'Gerando...' : 'Baixar planilha'}
          </Button>
        </Stack>
      </FilterToolbar>
      {erro && <Alert severity="error">{erro}</Alert>}
      {sucesso && <Alert severity="success">{sucesso}</Alert>}
      <SectionCard title="Conteúdo da planilha">
        A planilha inclui nome, data de nascimento, idade, contatos, categoria, status, vínculos familiares,
        discipulado, discipulador e gerência. IDs internos do sistema não são exportados.
      </SectionCard>
    </Stack>
  )
}
