import { AppBar, Box, Button, Container, FormControl, InputLabel, MenuItem, Select, Stack, Tab, Tabs, Toolbar, Typography } from '@mui/material'
import { useEffect, useMemo, useState } from 'react'
import AdolescentManagement from './AdolescentManagement'
import FrequencyManagement from './FrequencyManagement'
import OrganizationManagement from './OrganizationManagement'
import UserManagement from './UserManagement'
import { organizationApi, userManagementClient, type Discipulado, type Perfil, type Usuario } from './api'

type Section = 'estrutura' | 'usuarios' | 'adolescentes' | 'frequencia'
const roleLabel: Record<Perfil, string> = { ADMIN: 'Administrador', GERENTE: 'Gerente', DISCIPULADOR: 'Discipulador', CO_LIDER: 'Co-líder' }

export default function AuthenticatedApp({ currentUser, onLogout }: { currentUser: Usuario; onLogout: () => void }) {
  const sections = useMemo(() => {
    const values: Array<{ value: Section; label: string }> = []
    if (currentUser.perfis.includes('ADMIN')) values.push({ value: 'estrutura', label: 'Estrutura' }, { value: 'usuarios', label: 'Usuários' })
    values.push({ value: 'adolescentes', label: 'Adolescentes' })
    if (currentUser.perfis.some((role) => role === 'ADMIN' || role === 'DISCIPULADOR' || role === 'CO_LIDER')) values.push({ value: 'frequencia', label: 'Frequência' })
    return values
  }, [currentUser.perfis])
  const [section, setSection] = useState<Section>(sections[0].value)

  return <Box sx={{ minHeight: '100vh' }}><AppBar position="static"><Toolbar sx={{ gap: 2 }}><Typography variant="h6" sx={{ flexGrow: 1 }}>SGD</Typography><Box sx={{ textAlign: 'right' }}><Typography variant="body2">{currentUser.nome}</Typography><Typography variant="caption">{currentUser.perfis.map((role) => roleLabel[role]).join(', ')}</Typography></Box><Button color="inherit" onClick={onLogout}>Sair</Button></Toolbar></AppBar><Box sx={{ bgcolor: 'background.paper', borderBottom: 1, borderColor: 'divider' }}><Container><Tabs value={section} onChange={(_, value: Section) => setSection(value)} variant="scrollable" scrollButtons="auto">{sections.map((item) => <Tab key={item.value} value={item.value} label={item.label} />)}</Tabs></Container></Box><Container maxWidth="lg" sx={{ py: 4 }}>{section === 'estrutura' && <OrganizationManagement />}{section === 'usuarios' && <UserManagement client={userManagementClient} />}{section === 'adolescentes' && <AdolescentManagement />}{section === 'frequencia' && <FrequencyPage currentUser={currentUser} />}</Container></Box>
}

function FrequencyPage({ currentUser }: { currentUser: Usuario }) {
  const [discipulados, setDiscipulados] = useState<Discipulado[]>([])
  const [discipuladoId, setDiscipuladoId] = useState<number>(0)
  useEffect(() => { organizationApi.listarDiscipulados().then((page) => { setDiscipulados(page.content.filter((item) => item.ativo !== false)); setDiscipuladoId((current) => current || page.content.find((item) => item.ativo !== false)?.id || 0) }).catch(() => { setDiscipulados([]); setDiscipuladoId(0) }) }, [])
  return <Stack spacing={3}><FormControl size="small" sx={{ maxWidth: 420 }}><InputLabel>Discipulado</InputLabel><Select label="Discipulado" value={discipuladoId || ''} onChange={(event) => setDiscipuladoId(Number(event.target.value))}>{discipulados.map((item) => <MenuItem key={item.id} value={item.id}>{item.nome}</MenuItem>)}</Select></FormControl>{discipuladoId ? <FrequencyManagement discipuladoId={discipuladoId} podeAdministrar={currentUser.perfis.includes('ADMIN')} /> : <Typography color="text.secondary">Nenhum discipulado disponível no seu escopo.</Typography>}</Stack>
}
