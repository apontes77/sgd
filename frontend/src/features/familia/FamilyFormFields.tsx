import ExpandMoreRounded from '@mui/icons-material/ExpandMoreRounded'
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Button,
  Checkbox,
  FormControl,
  FormControlLabel,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material'

import {
  type FamiliaInput,
  familiaNaoConsta,
  NAO_CONSTA,
  type ResponsavelFamiliaInput,
  responsavelNaoConsta,
  SITUACAO_IGREJA_LABEL,
  SITUACAO_PAIS_LABEL,
  type SituacaoIgrejaFamilia,
  type SituacaoPaisFamilia,
} from '@/features/familia/api'
import { mensagemTelefoneInvalido, telefoneValido } from '@/shared/validation/telefone'

interface Props {
  value: FamiliaInput
  onChange: (next: FamiliaInput) => void
  disabled?: boolean
  compact?: boolean
}

function erroTelefone(valor: string) {
  if (!valor.trim() || valor.trim() === NAO_CONSTA) return undefined
  return telefoneValido(valor) ? undefined : mensagemTelefoneInvalido('telefone')
}

function ResponsavelFields({
  titulo,
  value,
  onChange,
  disabled,
}: {
  titulo: string
  value: ResponsavelFamiliaInput
  onChange: (next: ResponsavelFamiliaInput) => void
  disabled?: boolean
}) {
  return (
    <Stack spacing={1.5}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="subtitle2" fontWeight={700}>
          {titulo}
        </Typography>
        <Button size="small" disabled={disabled} onClick={() => onChange(responsavelNaoConsta())}>
          Não consta
        </Button>
      </Stack>
      <TextField
        required
        label="Nome completo"
        value={value.nome}
        disabled={disabled}
        onChange={(e) => onChange({ ...value, nome: e.target.value })}
      />
      <TextField
        required
        label="Parentesco"
        value={value.parentesco}
        disabled={disabled}
        onChange={(e) => onChange({ ...value, parentesco: e.target.value })}
      />
      <TextField
        type="date"
        label="Data de nascimento"
        InputLabelProps={{ shrink: true }}
        value={value.dataNascimento ?? ''}
        disabled={disabled}
        helperText="Deixe em branco se for “Não consta”."
        onChange={(e) => onChange({ ...value, dataNascimento: e.target.value })}
      />
      <TextField
        required
        label="Estado civil"
        value={value.estadoCivil}
        disabled={disabled}
        onChange={(e) => onChange({ ...value, estadoCivil: e.target.value })}
      />
      <TextField
        required
        label="Profissão"
        value={value.profissao}
        disabled={disabled}
        onChange={(e) => onChange({ ...value, profissao: e.target.value })}
      />
      <TextField
        required
        label="Telefone (WhatsApp)"
        value={value.telefone}
        disabled={disabled}
        error={Boolean(erroTelefone(value.telefone))}
        helperText={erroTelefone(value.telefone)}
        onChange={(e) => onChange({ ...value, telefone: e.target.value })}
      />
      <TextField
        required
        label="E-mail"
        value={value.email}
        disabled={disabled}
        onChange={(e) => onChange({ ...value, email: e.target.value })}
      />
      <TextField
        required
        label="Interesse pessoal"
        value={value.interessePessoal}
        disabled={disabled}
        onChange={(e) => onChange({ ...value, interessePessoal: e.target.value })}
      />
    </Stack>
  )
}

export function FamilyFormFields({ value, onChange, disabled, compact }: Props) {
  function patch(partial: Partial<FamiliaInput>) {
    onChange({ ...value, ...partial })
  }

  return (
    <Stack spacing={1.5}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="subtitle1" fontWeight={700}>
          Ficha de família
        </Typography>
        <Button size="small" disabled={disabled} onClick={() => onChange(familiaNaoConsta())}>
          Preencher tudo como “Não consta”
        </Button>
      </Stack>

      <Accordion defaultExpanded={!compact} disableGutters>
        <AccordionSummary expandIcon={<ExpandMoreRounded />}>
          <Typography fontWeight={650}>Responsáveis</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Stack spacing={2}>
            <ResponsavelFields
              titulo="Responsável 1"
              value={value.responsavel1}
              disabled={disabled}
              onChange={(responsavel1) => patch({ responsavel1 })}
            />
            <ResponsavelFields
              titulo="Responsável 2"
              value={value.responsavel2}
              disabled={disabled}
              onChange={(responsavel2) => patch({ responsavel2 })}
            />
          </Stack>
        </AccordionDetails>
      </Accordion>

      <Accordion defaultExpanded={!compact} disableGutters>
        <AccordionSummary expandIcon={<ExpandMoreRounded />}>
          <Typography fontWeight={650}>Endereço</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Stack spacing={1.5}>
            <TextField
              required
              label="CEP"
              value={value.cep}
              disabled={disabled}
              onChange={(e) => patch({ cep: e.target.value })}
            />
            <TextField
              required
              label="Rua"
              value={value.rua}
              disabled={disabled}
              onChange={(e) => patch({ rua: e.target.value })}
            />
            <TextField
              required
              label="Número"
              value={value.numero}
              disabled={disabled}
              onChange={(e) => patch({ numero: e.target.value })}
            />
            <TextField
              required
              label="Complemento"
              value={value.complemento}
              disabled={disabled}
              onChange={(e) => patch({ complemento: e.target.value })}
            />
            <TextField
              required
              label="Bairro"
              value={value.bairro}
              disabled={disabled}
              onChange={(e) => patch({ bairro: e.target.value })}
            />
            <TextField
              required
              label="Cidade"
              value={value.cidade}
              disabled={disabled}
              onChange={(e) => patch({ cidade: e.target.value })}
            />
          </Stack>
        </AccordionDetails>
      </Accordion>

      <Accordion defaultExpanded={!compact} disableGutters>
        <AccordionSummary expandIcon={<ExpandMoreRounded />}>
          <Typography fontWeight={650}>Situação na igreja e dos pais</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Stack spacing={1.5}>
            <FormControl required fullWidth>
              <InputLabel>Situação da família na igreja</InputLabel>
              <Select
                label="Situação da família na igreja"
                value={value.situacaoIgreja}
                disabled={disabled}
                onChange={(e) =>
                  patch({
                    situacaoIgreja: e.target.value as SituacaoIgrejaFamilia,
                    atuaOnde: e.target.value === 'FDV_ATUANTES' ? value.atuaOnde : NAO_CONSTA,
                  })
                }
              >
                {(Object.keys(SITUACAO_IGREJA_LABEL) as SituacaoIgrejaFamilia[]).map((key) => (
                  <MenuItem key={key} value={key}>
                    {SITUACAO_IGREJA_LABEL[key]}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              required={value.situacaoIgreja === 'FDV_ATUANTES'}
              label="Atua onde / departamento"
              value={value.atuaOnde}
              disabled={disabled || value.situacaoIgreja !== 'FDV_ATUANTES'}
              onChange={(e) => patch({ atuaOnde: e.target.value })}
            />
            <FormControl required fullWidth>
              <InputLabel>Situação dos pais</InputLabel>
              <Select
                label="Situação dos pais"
                value={value.situacaoPais}
                disabled={disabled}
                onChange={(e) => patch({ situacaoPais: e.target.value as SituacaoPaisFamilia })}
              >
                {(Object.keys(SITUACAO_PAIS_LABEL) as SituacaoPaisFamilia[]).map((key) => (
                  <MenuItem key={key} value={key}>
                    {SITUACAO_PAIS_LABEL[key]}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>
        </AccordionDetails>
      </Accordion>

      <Accordion defaultExpanded={!compact} disableGutters>
        <AccordionSummary expandIcon={<ExpandMoreRounded />}>
          <Typography fontWeight={650}>Conhecendo a família</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Stack spacing={1.5}>
            <TextField
              required
              multiline
              minRows={2}
              label="Descrição em poucas palavras"
              value={value.descricao}
              disabled={disabled}
              onChange={(e) => patch({ descricao: e.target.value })}
            />
            <FormControlLabel
              control={
                <Checkbox
                  checked={value.desafioFinanceiro}
                  disabled={disabled}
                  onChange={(e) => patch({ desafioFinanceiro: e.target.checked })}
                />
              }
              label="Desafio financeiro"
            />
            <FormControlLabel
              control={
                <Checkbox
                  checked={value.desafioEmocional}
                  disabled={disabled}
                  onChange={(e) => patch({ desafioEmocional: e.target.checked })}
                />
              }
              label="Desafio emocional"
            />
            <FormControlLabel
              control={
                <Checkbox
                  checked={value.desafioEspiritual}
                  disabled={disabled}
                  onChange={(e) => patch({ desafioEspiritual: e.target.checked })}
                />
              }
              label="Desafio espiritual"
            />
            <TextField
              required
              multiline
              minRows={2}
              label="Descrição dos desafios"
              value={value.desafiosDescricao}
              disabled={disabled}
              onChange={(e) => patch({ desafiosDescricao: e.target.value })}
            />
            <TextField
              required
              multiline
              minRows={2}
              label="Atividades que gostam de fazer juntos"
              value={value.atividadesJuntas}
              disabled={disabled}
              onChange={(e) => patch({ atividadesJuntas: e.target.value })}
            />
            <TextField
              required
              multiline
              minRows={2}
              label="Rotina da semana"
              value={value.rotinaSemana}
              disabled={disabled}
              onChange={(e) => patch({ rotinaSemana: e.target.value })}
            />
            <TextField
              required
              label="Tem irmão no Dokmos? (nome)"
              value={value.irmaoDokmos}
              disabled={disabled}
              onChange={(e) => patch({ irmaoDokmos: e.target.value })}
            />
            <TextField
              required
              multiline
              minRows={2}
              label="Pedido de oração específico"
              value={value.pedidoOracao}
              disabled={disabled}
              onChange={(e) => patch({ pedidoOracao: e.target.value })}
            />
          </Stack>
        </AccordionDetails>
      </Accordion>

      <Accordion defaultExpanded={!compact} disableGutters>
        <AccordionSummary expandIcon={<ExpandMoreRounded />}>
          <Typography fontWeight={650}>Intervenção e observações</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Stack spacing={1.5}>
            <TextField
              required
              multiline
              minRows={2}
              label="Intervenção"
              value={value.intervencao}
              disabled={disabled}
              onChange={(e) => patch({ intervencao: e.target.value })}
            />
            <TextField
              required
              multiline
              minRows={2}
              label="Observação do discipulador"
              value={value.observacaoDiscipulador}
              disabled={disabled}
              onChange={(e) => patch({ observacaoDiscipulador: e.target.value })}
            />
            <TextField
              required
              multiline
              minRows={2}
              label="Observação do gerente"
              value={value.observacaoGerente}
              disabled={disabled}
              onChange={(e) => patch({ observacaoGerente: e.target.value })}
            />
          </Stack>
        </AccordionDetails>
      </Accordion>
    </Stack>
  )
}
