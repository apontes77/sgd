import { Checkbox, FormControl, FormControlLabel, InputLabel, MenuItem, Select, TextField } from '@mui/material'

import { CATEGORIA_LABEL, type CategoriaAdolescente } from '@/features/adolescentes/api'
import { mensagemTelefoneInvalido, telefoneValido } from '@/shared/validation/telefone'

export interface DadosPessoaisAdolescente {
  nome: string
  dataNascimento: string
  telefone?: string
  naoPossuiTelefone?: boolean
  instagram?: string
  consentimentoEm: string
  categoria: CategoriaAdolescente
  estrutura?: string
  motivoAfastamento?: string
}

interface Props {
  value: DadosPessoaisAdolescente
  onChange: (patch: Partial<DadosPessoaisAdolescente>) => void
  disabled?: boolean
  autoFocus?: boolean
  listagemSimples?: boolean
}

function erroTelefone(valor: string | undefined, rotulo: string) {
  if (!valor?.trim()) return undefined
  return telefoneValido(valor) ? undefined : mensagemTelefoneInvalido(rotulo)
}

export function AdolescenteFormFields({ value, onChange, disabled, autoFocus = true, listagemSimples = false }: Props) {
  const goe = !listagemSimples && value.categoria === 'DISCIPULO_GOE'
  const semTelefone = Boolean(value.naoPossuiTelefone)

  return (
    <>
      {!listagemSimples && (
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
      )}
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
        required={goe && !semTelefone}
        label="Telefone"
        value={semTelefone ? '' : (value.telefone ?? '')}
        disabled={disabled || semTelefone}
        error={Boolean(!semTelefone && erroTelefone(value.telefone, 'telefone do adolescente'))}
        helperText={
          semTelefone
            ? 'Cadastro permitido sem telefone. É possível informar depois na edição.'
            : (erroTelefone(value.telefone, 'telefone do adolescente') ??
              (goe ? 'Obrigatório para Discípulo GOE, salvo se não possuir telefone.' : undefined))
        }
        onChange={(e) => onChange({ telefone: e.target.value, naoPossuiTelefone: false })}
      />
      <FormControlLabel
        control={
          <Checkbox
            checked={semTelefone}
            disabled={disabled}
            onChange={(e) =>
              onChange({
                naoPossuiTelefone: e.target.checked,
                telefone: e.target.checked ? '' : value.telefone,
              })
            }
          />
        }
        label="Não possui telefone"
      />
      <TextField
        label="Instagram"
        value={value.instagram ?? ''}
        disabled={disabled}
        onChange={(e) => onChange({ instagram: e.target.value })}
      />
      <TextField
        required
        type="date"
        label="Consentimento em"
        InputLabelProps={{ shrink: true }}
        value={value.consentimentoEm}
        disabled={disabled}
        onChange={(e) => onChange({ consentimentoEm: e.target.value })}
      />
      {!listagemSimples && (
        <TextField
          label="Estrutura"
          value={value.estrutura ?? ''}
          disabled={disabled}
          onChange={(e) => onChange({ estrutura: e.target.value })}
        />
      )}
      {goe && (
        <TextField
          required
          multiline
          minRows={2}
          label="Motivo do afastamento"
          value={value.motivoAfastamento ?? ''}
          disabled={disabled}
          onChange={(e) => onChange({ motivoAfastamento: e.target.value })}
        />
      )}
    </>
  )
}
