import { Alert, Divider, FormControl, InputLabel, MenuItem, Select, TextField, Typography } from '@mui/material'

import { CATEGORIA_LABEL, type CategoriaAdolescente } from '@/features/adolescentes/api'
import { mensagemTelefoneInvalido, telefoneValido } from '@/shared/validation/telefone'

export interface DadosPessoaisAdolescente {
  nome: string
  dataNascimento: string
  telefone?: string
  instagram?: string
  responsavelNome: string
  responsavelTelefone?: string
  consentimentoEm: string
  categoria: CategoriaAdolescente
  nomeMae?: string
  telefoneMae?: string
  nomePai?: string
  telefonePai?: string
  estrutura?: string
  motivoAfastamento?: string
}

interface Props {
  value: DadosPessoaisAdolescente
  onChange: (patch: Partial<DadosPessoaisAdolescente>) => void
  disabled?: boolean
  autoFocus?: boolean
}

function temDadosMae(value: DadosPessoaisAdolescente) {
  return Boolean(value.nomeMae?.trim() || value.telefoneMae?.trim())
}

function temDadosPai(value: DadosPessoaisAdolescente) {
  return Boolean(value.nomePai?.trim() || value.telefonePai?.trim())
}

function maeCompleta(value: DadosPessoaisAdolescente) {
  return Boolean(value.nomeMae?.trim() && value.telefoneMae?.trim() && telefoneValido(value.telefoneMae))
}

function paiCompleto(value: DadosPessoaisAdolescente) {
  return Boolean(value.nomePai?.trim() && value.telefonePai?.trim() && telefoneValido(value.telefonePai))
}

function deveExibirResponsavel(value: DadosPessoaisAdolescente) {
  return !maeCompleta(value) && !paiCompleto(value)
}

function erroTelefone(valor: string | undefined, rotulo: string) {
  if (!valor?.trim()) return undefined
  return telefoneValido(valor) ? undefined : mensagemTelefoneInvalido(rotulo)
}

export function AdolescenteFormFields({ value, onChange, disabled, autoFocus = true }: Props) {
  const goe = value.categoria === 'DISCIPULO_GOE'
  const exibirResponsavel = deveExibirResponsavel(value)

  function alterarContatoFamiliar(patch: Partial<DadosPessoaisAdolescente>) {
    const next = { ...value, ...patch }
    if (maeCompleta(next) || paiCompleto(next)) {
      onChange({ ...patch, responsavelNome: '', responsavelTelefone: '' })
      return
    }
    onChange(patch)
  }

  return (
    <>
      <FormControl required fullWidth>
        <InputLabel>Categoria</InputLabel>
        <Select
          label="Categoria"
          value={value.categoria}
          disabled={disabled}
          onChange={(e) => {
            const categoria = e.target.value as CategoriaAdolescente
            onChange({
              categoria,
              motivoAfastamento: categoria === 'DISCIPULO_GOE' ? value.motivoAfastamento : '',
            })
          }}
        >
          {(Object.keys(CATEGORIA_LABEL) as CategoriaAdolescente[]).map((key) => (
            <MenuItem key={key} value={key}>
              {CATEGORIA_LABEL[key]}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
      <TextField
        required
        autoFocus={autoFocus}
        label="Nome"
        value={value.nome}
        disabled={disabled}
        onChange={(e) => onChange({ nome: e.target.value })}
      />
      <TextField
        required
        type="date"
        label="Data de nascimento"
        InputLabelProps={{ shrink: true }}
        value={value.dataNascimento}
        disabled={disabled}
        onChange={(e) => onChange({ dataNascimento: e.target.value })}
      />
      <TextField
        required={goe}
        label="Telefone"
        value={value.telefone ?? ''}
        disabled={disabled}
        error={Boolean(erroTelefone(value.telefone, 'telefone do adolescente'))}
        helperText={
          erroTelefone(value.telefone, 'telefone do adolescente') ??
          (goe ? 'Obrigatório para Discípulo GOE.' : undefined)
        }
        onChange={(e) => onChange({ telefone: e.target.value })}
      />
      <TextField
        label="Estrutura"
        value={value.estrutura ?? ''}
        disabled={disabled}
        onChange={(e) => onChange({ estrutura: e.target.value })}
      />
      <TextField
        label="Instagram"
        value={value.instagram ?? ''}
        disabled={disabled}
        onChange={(e) => onChange({ instagram: e.target.value })}
      />
      {goe && (
        <TextField
          required
          label="Motivo do afastamento"
          value={value.motivoAfastamento ?? ''}
          disabled={disabled}
          multiline
          minRows={2}
          onChange={(e) => onChange({ motivoAfastamento: e.target.value })}
        />
      )}

      <Divider textAlign="left">
        <Typography variant="overline" color="text.secondary">
          Filiação e contatos
        </Typography>
      </Divider>
      <Alert severity="info" variant="outlined">
        {goe
          ? 'Para Discípulo GOE, o telefone do adolescente é suficiente. Contatos de mãe, pai ou responsável são opcionais.'
          : 'Informe nome e telefone da mãe, ou do pai, ou de um responsável. É necessário pelo menos um desses pares completos para salvar o cadastro.'}
      </Alert>
      <TextField
        label="Nome da mãe"
        value={value.nomeMae ?? ''}
        disabled={disabled}
        required={temDadosMae(value)}
        onChange={(e) => alterarContatoFamiliar({ nomeMae: e.target.value })}
      />
      <TextField
        label="Tel. mãe"
        value={value.telefoneMae ?? ''}
        disabled={disabled}
        required={temDadosMae(value)}
        error={Boolean(erroTelefone(value.telefoneMae, 'telefone da mãe'))}
        helperText={erroTelefone(value.telefoneMae, 'telefone da mãe')}
        onChange={(e) => alterarContatoFamiliar({ telefoneMae: e.target.value })}
      />
      <TextField
        label="Nome do pai"
        value={value.nomePai ?? ''}
        disabled={disabled}
        required={temDadosPai(value)}
        onChange={(e) => alterarContatoFamiliar({ nomePai: e.target.value })}
      />
      <TextField
        label="Tel. pai"
        value={value.telefonePai ?? ''}
        disabled={disabled}
        required={temDadosPai(value)}
        error={Boolean(erroTelefone(value.telefonePai, 'telefone do pai'))}
        helperText={erroTelefone(value.telefonePai, 'telefone do pai')}
        onChange={(e) => alterarContatoFamiliar({ telefonePai: e.target.value })}
      />

      {exibirResponsavel && (
        <>
          <Divider textAlign="left">
            <Typography variant="overline" color="text.secondary">
              Responsável
            </Typography>
          </Divider>
          <TextField
            required={!goe}
            label="Nome do responsável"
            value={value.responsavelNome}
            disabled={disabled}
            onChange={(e) => onChange({ responsavelNome: e.target.value })}
          />
          <TextField
            required={!goe}
            label="Telefone do responsável"
            value={value.responsavelTelefone ?? ''}
            disabled={disabled}
            error={Boolean(erroTelefone(value.responsavelTelefone, 'telefone do responsável'))}
            helperText={erroTelefone(value.responsavelTelefone, 'telefone do responsável')}
            onChange={(e) => onChange({ responsavelTelefone: e.target.value })}
          />
        </>
      )}

      <Divider textAlign="left">
        <Typography variant="overline" color="text.secondary">
          Consentimento (LGPD)
        </Typography>
      </Divider>
      <Alert severity="info" variant="outlined">
        Os dados do adolescente são coletados apenas para registrar e acompanhar a frequência no discipulado, ficando
        acessíveis somente à liderança do grupo. Por se tratar de menor de idade, é necessário o consentimento (LGPD,
        art. 14). O responsável pode solicitar a exclusão dos dados a qualquer momento.
      </Alert>
      <TextField
        required
        type="date"
        label="Consentimento obtido em"
        InputLabelProps={{ shrink: true }}
        value={value.consentimentoEm}
        disabled={disabled}
        onChange={(e) => onChange({ consentimentoEm: e.target.value })}
      />
    </>
  )
}
