import { Tab, Tabs } from '@mui/material'
import { useState } from 'react'

import AdolescentExportReport from '@/features/relatorios/AdolescentExportReport'
import FrequencyReport from '@/features/relatorios/FrequencyReport'
import type { Usuario } from '@/shared/api/types'

type TipoRelatorio = 'frequencia' | 'adolescentes'

export default function ReportsPage({ currentUser }: { currentUser: Usuario }) {
  const isAdmin = currentUser.perfis.includes('ADMIN')
  const [tipo, setTipo] = useState<TipoRelatorio>('frequencia')

  if (!isAdmin) {
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
        <Tab value="adolescentes" label="Adolescentes" />
      </Tabs>
      {tipo === 'frequencia' ? <FrequencyReport currentUser={currentUser} /> : <AdolescentExportReport />}
    </>
  )
}
