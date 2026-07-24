import { Alert, Divider, TextField, Typography } from '@mui/material'

export interface DadosPessoaisAdolescente {
  nome: string
  dataNascimento: string
  telefone?: string
  instagram?: string
  responsavelNome: string
  responsavelTelefone?: string
  consentimentoEm: string
}

interface Props {
  value: DadosPessoaisAdolescente
  onChange: (patch: Partial<DadosPessoaisAdolescente>) => void
  disabled?: boolean
  autoFocus?: boolean
}

export function AdolescenteFormFields({ value, onChange, disabled, autoFocus = true }: Props) {
  return (
    <>
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
        label="Telefone"
        value={value.telefone ?? ''}
        disabled={disabled}
        onChange={(e) => onChange({ telefone: e.target.value })}
      />
      <TextField
        label="Instagram"
        value={value.instagram ?? ''}
        disabled={disabled}
        onChange={(e) => onChange({ instagram: e.target.value })}
      />
      <Divider textAlign="left">
        <Typography variant="overline" color="text.secondary">
          Responsável e consentimento
        </Typography>
      </Divider>
      <Alert severity="info" variant="outlined">
        Os dados do adolescente são coletados apenas para registrar e acompanhar a frequência no discipulado, ficando
        acessíveis somente à liderança do grupo. Por se tratar de menor de idade, é necessário o consentimento de um
        responsável (LGPD, art. 14). O responsável pode solicitar a exclusão dos dados a qualquer momento.
      </Alert>
      <TextField
        required
        label="Nome do responsável"
        value={value.responsavelNome}
        disabled={disabled}
        onChange={(e) => onChange({ responsavelNome: e.target.value })}
      />
      <TextField
        label="Telefone do responsável"
        value={value.responsavelTelefone ?? ''}
        disabled={disabled}
        onChange={(e) => onChange({ responsavelTelefone: e.target.value })}
      />
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
