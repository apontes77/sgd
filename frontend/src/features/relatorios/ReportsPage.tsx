import { Tab, Tabs } from '@mui/material'
import { useState } from 'react'

import AdolescentExportReport from '@/features/relatorios/AdolescentExportReport'
import FrequencyReport from '@/features/relatorios/FrequencyReport'
import LeadershipAttendanceReport from '@/features/relatorios/LeadershipAttendanceReport'
import type { Usuario } from '@/shared/api/types'

type TipoRelatorio = 'frequencia' | 'frequencia-formacao' | 'adolescentes' | 'lideranca'

export default function ReportsPage({
  currentUser,
  podeFrequenciaFormacao = false,
}: {
  currentUser: Usuario
  podeFrequenciaFormacao?: boolean
}) {
  const isAdmin = currentUser.perfis.includes('ADMIN')
  const [tipo, setTipo] = useState<TipoRelatorio>('frequencia')

  if (!isAdmin && !podeFrequenciaFormacao) {
    return <FrequencyReport currentUser={currentUser} />
  }

  return (
    <>
      <Tabs
        value={tipo}
        onChange={(_, value: TipoRelatorio) => setTipo(value)}
        variant="scrollable"
        allowScrollButtonsMobile
        sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}
      >
        <Tab value="frequencia" label="Frequência" />
        {podeFrequenciaFormacao && <Tab value="frequencia-formacao" label="Frequência em formação" />}
        {isAdmin && <Tab value="adolescentes" label="Adolescentes" />}
        {isAdmin && <Tab value="lideranca" label="Liderança" />}
      </Tabs>
      {tipo === 'frequencia' && <FrequencyReport currentUser={currentUser} />}
      {podeFrequenciaFormacao && tipo === 'frequencia-formacao' && (
        <FrequencyReport currentUser={currentUser} emFormacao />
      )}
      {isAdmin && tipo === 'adolescentes' && <AdolescentExportReport />}
      {isAdmin && tipo === 'lideranca' && <LeadershipAttendanceReport />}
    </>
  )
}
