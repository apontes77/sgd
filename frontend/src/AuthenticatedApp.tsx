import { AppBar, Box, Button, Container, FormControl, InputLabel, MenuItem, Select, Stack, Tab, Tabs, Toolbar, Typography } from '@mui/material'
import { lazy, Suspense, useEffect, useMemo, useState } from 'react'
import AdolescentManagement from './AdolescentManagement'
import FrequencyManagement from './FrequencyManagement'
import OrganizationManagement from './OrganizationManagement'
import UserManagement from './UserManagement'
import { organizationApi, userManagementClient, type Discipulado, type Perfil, type Usuario } from './api'

const AdminDashboard = lazy(() => import('./AdminDashboard'))
const ManagerDashboard = lazy(() => import('./ManagerDashboard'))
const LeaderDashboard = lazy(() => import('./LeaderDashboard'))

type Section = 'painel' | 'minha-gerencia' | 'meu-discipulado' | 'estrutura' | 'usuarios' | 'adolescentes' | 'frequencia'
const roleLabel: Record<Perfil, string> = { ADMIN: 'Administrador', GERENTE: 'Gerente', DISCIPULADOR: 'Discipulador', CO_LIDER: 'Co-líder' }

export default function AuthenticatedApp({ currentUser, onLogout }: { currentUser: Usuario; onLogout: () => void }) {
  const sections = useMemo(() => {
    const values: Array<{ value: Section; label: string }> = []
    if (currentUser.perfis.includes('ADMIN')) values.push({ value: 'painel', label: 'Painel' }, { value: 'estrutura', label: 'Estrutura' }, { value: 'usuarios', label: 'Usuários' })
    if (currentUser.perfis.includes('GERENTE')) values.push({ value: 'minha-gerencia', label: 'Minha gerência' })
    if (currentUser.perfis.some((role) => role === 'DISCIPULADOR' || role === 'CO_LIDER')) values.push({ value: 'meu-discipulado', label: 'Meu discipulado' })
    values.push({ value: 'adolescentes', label: 'Adolescentes' })
    if (currentUser.perfis.some((role) => role === 'ADMIN' || role === 'DISCIPULADOR' || role === 'CO_LIDER')) values.push({ value: 'frequencia', label: 'Registrar frequência' })
    return values
  }, [currentUser.perfis])
  const [section, setSection] = useState<Section>(sections[0].value)

  return <Box sx={{ minHeight: '100vh' }}><AppBar position="static"><Toolbar sx={{ gap: 2 }}><Typography variant="h6" sx={{ flexGrow: 1 }}>SGD</Typography><Box sx={{ textAlign: 'right' }}><Typography variant="body2">{currentUser.nome}</Typography><Typography variant="caption">{currentUser.perfis.map((role) => roleLabel[role]).join(', ')}</Typography></Box><Button color="inherit" onClick={onLogout}>Sair</Button></Toolbar></AppBar><Box sx={{ bgcolor: 'background.paper', borderBottom: 1, borderColor: 'divider' }}><Container><Tabs value={section} onChange={(_, value: Section) => setSection(value)} variant="scrollable" scrollButtons="auto">{sections.map((item) => <Tab key={item.value} value={item.value} label={item.label} />)}</Tabs></Container></Box><Container maxWidth="lg" sx={{ py: 4 }}>{section === 'painel' && <Suspense fallback={<Typography>Carregando painel...</Typography>}><AdminDashboard /></Suspense>}{section === 'minha-gerencia' && <Suspense fallback={<Typography>Carregando painel...</Typography>}><ManagerDashboard /></Suspense>}{section === 'meu-discipulado' && <Suspense fallback={<Typography>Carregando histórico...</Typography>}><LeaderDashboard /></Suspense>}{section === 'estrutura' && <OrganizationManagement />}{section === 'usuarios' && <UserManagement client={userManagementClient} />}{section === 'adolescentes' && <AdolescentManagement />}{section === 'frequencia' && <FrequencyPage currentUser={currentUser} />}</Container></Box>
}

function FrequencyPage({ currentUser }: { currentUser: Usuario }) {
  const [discipulados, setDiscipulados] = useState<Discipulado[]>([])
  const [discipuladoId, setDiscipuladoId] = useState<number>(0)
  useEffect(() => {
    const consulta = currentUser.perfis.includes('ADMIN')
      ? organizationApi.listarDiscipulados(true).then((page) => page.content)
      : organizationApi.listarDiscipuladosLiderados(true)
    consulta.then((items) => {
      setDiscipulados(items)
      setDiscipuladoId((current) => items.some((item) => item.id === current) ? current : items[0]?.id ?? 0)
    }).catch(() => { setDiscipulados([]); setDiscipuladoId(0) })
  }, [currentUser.perfis])
  return <Stack spacing={3}><FormControl size="small" sx={{ maxWidth: 420 }}><InputLabel>Discipulado</InputLabel><Select label="Discipulado" value={discipuladoId || ''} onChange={(event) => setDiscipuladoId(Number(event.target.value))}>{discipulados.map((item) => <MenuItem key={item.id} value={item.id}>{item.nome}</MenuItem>)}</Select></FormControl>{discipuladoId ? <FrequencyManagement discipuladoId={discipuladoId} podeAdministrar={currentUser.perfis.includes('ADMIN')} /> : <Typography color="text.secondary">Nenhum discipulado disponível no seu escopo.</Typography>}</Stack>
}
