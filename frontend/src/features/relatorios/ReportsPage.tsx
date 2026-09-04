import { Tab, Tabs } from '@mui/material'
import { useState } from 'react'

import AdolescentExportReport from '@/features/relatorios/AdolescentExportReport'
import FrequencyReport from '@/features/relatorios/FrequencyReport'
import LeadershipAttendanceReport from '@/features/relatorios/LeadershipAttendanceReport'
import type { Usuario } from '@/shared/api/types'

type TipoRelatorio = 'frequencia' | 'frequencia-formacao' | 'adolescentes' | 'lideranca'

export default function ReportsPage({ currentUser }: { currentUser: Usuario }) {
  const isAdmin = currentUser.perfis.includes('ADMIN')
  const isDiscipulador = currentUser.perfis.includes('DISCIPULADOR')
  const [tipo, setTipo] = useState<TipoRelatorio>('frequencia')

  if (!isAdmin && !isDiscipulador) {
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
        <Tab value="frequencia-formacao" label="Frequência em formação" />
        {isAdmin && <Tab value="adolescentes" label="Adolescentes" />}
        {isAdmin && <Tab value="lideranca" label="Liderança" />}
      </Tabs>
      {tipo === 'frequencia' && <FrequencyReport currentUser={currentUser} />}
      {tipo === 'frequencia-formacao' && <FrequencyReport currentUser={currentUser} emFormacao />}
      {isAdmin && tipo === 'adolescentes' && <AdolescentExportReport />}
      {isAdmin && tipo === 'lideranca' && <LeadershipAttendanceReport />}
    </>
  )
}
