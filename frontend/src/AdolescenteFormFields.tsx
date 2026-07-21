import { TextField } from '@mui/material'

export interface DadosPessoaisAdolescente { nome: string; dataNascimento: string; telefone?: string; instagram?: string }

interface Props {
  value: DadosPessoaisAdolescente
  onChange: (patch: Partial<DadosPessoaisAdolescente>) => void
  disabled?: boolean
  autoFocus?: boolean
}

export function AdolescenteFormFields({ value, onChange, disabled, autoFocus = true }: Props) {
  return <>
    <TextField required autoFocus={autoFocus} label="Nome" value={value.nome} disabled={disabled} onChange={e => onChange({ nome: e.target.value })} />
    <TextField required type="date" label="Data de nascimento" InputLabelProps={{ shrink: true }} value={value.dataNascimento} disabled={disabled} onChange={e => onChange({ dataNascimento: e.target.value })} />
    <TextField label="Telefone" value={value.telefone ?? ''} disabled={disabled} onChange={e => onChange({ telefone: e.target.value })} />
    <TextField label="Instagram" value={value.instagram ?? ''} disabled={disabled} onChange={e => onChange({ instagram: e.target.value })} />
  </>
}
