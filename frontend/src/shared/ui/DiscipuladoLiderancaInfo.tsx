import { Stack, Typography } from '@mui/material'

import type { FaixaEtaria } from '@/shared/api/types'

const FAIXA_ETARIA_LABEL: Record<FaixaEtaria, string> = {
  DE_09_A_11: '09 a 11',
  DE_11_A_13: '11 a 13',
  DE_13_A_15: '13 a 15',
  DE_15_MAIS: '15+',
}

export interface LiderancaPessoa {
  id?: number
  nome: string
}

export interface DiscipuladoLiderancaInfoProps {
  discipuladorNome?: string | null
  coLideres?: LiderancaPessoa[] | null
  faixaEtaria?: FaixaEtaria | null
  showFaixaEtaria?: boolean
}

function textoOuTraco(valor?: string | null) {
  const limpo = valor?.trim()
  return limpo ? limpo : '—'
}

/** coLideres[0] = co-líder; coLideres[1] = co-líder em treinamento. */
export function DiscipuladoLiderancaInfo({
  discipuladorNome,
  coLideres = [],
  faixaEtaria,
  showFaixaEtaria = false,
}: DiscipuladoLiderancaInfoProps) {
  const lista = coLideres ?? []
  const coLider = lista[0]?.nome
  const coLiderTreinamento = lista[1]?.nome
  const faixa = faixaEtaria ? (FAIXA_ETARIA_LABEL[faixaEtaria] ?? faixaEtaria) : undefined

  return (
    <Stack
      spacing={0.5}
      sx={{
        width: '100%',
        pt: 0.5,
      }}
    >
      <Linha label="Discipulador" valor={textoOuTraco(discipuladorNome)} />
      <Linha label="Co-líder" valor={textoOuTraco(coLider)} />
      <Linha label="Co-líder em treinamento" valor={textoOuTraco(coLiderTreinamento)} />
      {showFaixaEtaria && <Linha label="Idade do discipulado" valor={textoOuTraco(faixa)} />}
    </Stack>
  )
}

function Linha({ label, valor }: { label: string; valor: string }) {
  return (
    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.45 }}>
      <Typography component="span" variant="body2" color="text.primary" fontWeight={600}>
        {label}:
      </Typography>{' '}
      {valor}
    </Typography>
  )
}
