import {
  AccountTreeRounded, AssessmentRounded, DashboardRounded, Diversity3Rounded, FactCheckRounded,
  GroupsRounded, LogoutRounded, MenuRounded, MoreVertRounded, PeopleAltRounded,
} from '@mui/icons-material'
import {
  AppBar, Avatar, Box, Divider, Drawer, FormControl, IconButton, InputLabel,
  List, ListItemButton, ListItemIcon, ListItemText, Menu, MenuItem, Select, Stack,
  Toolbar, Tooltip, Typography,
} from '@mui/material'
import { lazy, ReactNode, Suspense, useEffect, useMemo, useState } from 'react'
import AdolescentManagement from './AdolescentManagement'
import FrequencyManagement from './FrequencyManagement'
import OrganizationManagement from './OrganizationManagement'
import UserManagement from './UserManagement'
import { organizationApi, userManagementClient, type Discipulado, type Perfil, type Usuario } from './api'
import { EmptyState, LoadingState, PageHeader } from './ui'

const AdminDashboard = lazy(() => import('./AdminDashboard'))
const ManagerDashboard = lazy(() => import('./ManagerDashboard'))
const LeaderDashboard = lazy(() => import('./LeaderDashboard'))
const FrequencyReport = lazy(() => import('./FrequencyReport'))

type Section = 'painel' | 'minha-gerencia' | 'meu-discipulado' | 'estrutura' | 'usuarios' | 'adolescentes' | 'frequencia' | 'relatorios'
type NavGroup = 'Visão geral' | 'Gestão' | 'Operação' | 'Análises'
type NavItem = { value: Section; label: string; group: NavGroup; icon: ReactNode }
const drawerWidth = 264
const roleLabel: Record<Perfil, string> = { ADMIN: 'Administrador', GERENTE: 'Gerente', DISCIPULADOR: 'Discipulador', CO_LIDER: 'Co-líder' }

export default function AuthenticatedApp({ currentUser, onLogout }: { currentUser: Usuario; onLogout: () => void }) {
  const sections = useMemo(() => {
    const values: NavItem[] = []
    if (currentUser.perfis.includes('ADMIN')) values.push({ value: 'painel', label: 'Painel', group: 'Visão geral', icon: <DashboardRounded /> })
    if (currentUser.perfis.includes('GERENTE')) values.push({ value: 'minha-gerencia', label: 'Minha gerência', group: 'Visão geral', icon: <DashboardRounded /> })
    if (currentUser.perfis.some((role) => role === 'DISCIPULADOR' || role === 'CO_LIDER')) values.push({ value: 'meu-discipulado', label: 'Meu discipulado', group: 'Visão geral', icon: <DashboardRounded /> })
    if (currentUser.perfis.includes('ADMIN')) values.push({ value: 'estrutura', label: 'Estrutura', group: 'Gestão', icon: <AccountTreeRounded /> }, { value: 'usuarios', label: 'Usuários', group: 'Gestão', icon: <PeopleAltRounded /> })
    values.push({ value: 'adolescentes', label: 'Adolescentes', group: 'Gestão', icon: <GroupsRounded /> })
    if (currentUser.perfis.some((role) => role === 'ADMIN' || role === 'DISCIPULADOR' || role === 'CO_LIDER')) values.push({ value: 'frequencia', label: currentUser.perfis.includes('ADMIN') ? 'Encontros e frequência' : 'Registrar frequência', group: 'Operação', icon: <FactCheckRounded /> })
    values.push({ value: 'relatorios', label: 'Relatórios', group: 'Análises', icon: <AssessmentRounded /> })
    return values
  }, [currentUser.perfis])
  const [section, setSection] = useState<Section>(sections[0].value)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null)
  const currentSection = sections.find((item) => item.value === section) ?? sections[0]
  function navigate(value: Section) { setSection(value); setMobileOpen(false) }

  const navigation = <Navigation items={sections} current={section} currentUser={currentUser} onNavigate={navigate} />
  return <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
    <AppBar position="fixed" color="inherit" elevation={0} sx={{ width: { md: `calc(100% - ${drawerWidth}px)` }, ml: { md: `${drawerWidth}px` }, borderBottom: '1px solid', borderColor: 'divider', bgcolor: 'rgba(255,255,255,.94)', backdropFilter: 'blur(12px)', zIndex: (theme) => theme.zIndex.drawer - 1 }}>
      <Toolbar sx={{ minHeight: { xs: 64, md: 68 }, px: { xs: 2, md: 3 } }}>
        <IconButton edge="start" onClick={() => setMobileOpen(true)} sx={{ mr: 1.5, display: { md: 'none' } }} aria-label="Abrir menu"><MenuRounded /></IconButton>
        <Box sx={{ flexGrow: 1 }}><Typography variant="body2" color="text.secondary">SGD</Typography><Typography variant="h6">{currentSection.label}</Typography></Box>
        <Tooltip title="Menu do usuário"><IconButton onClick={(event) => setMenuAnchor(event.currentTarget)} aria-label="Menu do usuário"><MoreVertRounded /></IconButton></Tooltip>
        <Menu anchorEl={menuAnchor} open={Boolean(menuAnchor)} onClose={() => setMenuAnchor(null)} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }} transformOrigin={{ vertical: 'top', horizontal: 'right' }}>
          <Box sx={{ px: 2, py: 1, minWidth: 220 }}><Typography variant="subtitle2">{currentUser.nome}</Typography><Typography variant="caption" color="text.secondary">{currentUser.perfis.map((role) => roleLabel[role]).join(', ')}</Typography></Box>
          <Divider /><MenuItem onClick={() => { setMenuAnchor(null); onLogout() }}><ListItemIcon><LogoutRounded fontSize="small" /></ListItemIcon>Sair</MenuItem>
        </Menu>
      </Toolbar>
    </AppBar>
    <Box component="nav" aria-label="Navegação principal">
      <Drawer variant="permanent" sx={{ display: { xs: 'none', md: 'block' }, '& .MuiDrawer-paper': { width: drawerWidth, boxSizing: 'border-box', borderRightColor: 'divider' } }} open>{navigation}</Drawer>
      <Drawer variant="temporary" open={mobileOpen} onClose={() => setMobileOpen(false)} ModalProps={{ keepMounted: false }} sx={{ display: { xs: 'block', md: 'none' }, '& .MuiDrawer-paper': { width: drawerWidth, boxSizing: 'border-box' } }}>{navigation}</Drawer>
    </Box>
    <Box component="main" sx={{ ml: { md: `${drawerWidth}px` }, pt: { xs: '64px', md: '68px' }, minHeight: '100vh' }}>
      <Box sx={{ width: '100%', maxWidth: 1600, mx: 'auto', p: { xs: 2, sm: 3, lg: 4 } }}>
        <Suspense fallback={<LoadingState label="Carregando módulo..." />}>
          {section === 'painel' && <AdminDashboard />}
          {section === 'minha-gerencia' && <ManagerDashboard />}
          {section === 'meu-discipulado' && <LeaderDashboard />}
          {section === 'estrutura' && <OrganizationManagement />}
          {section === 'usuarios' && <UserManagement client={userManagementClient} />}
          {section === 'adolescentes' && <AdolescentManagement />}
          {section === 'frequencia' && <FrequencyPage currentUser={currentUser} />}
          {section === 'relatorios' && <FrequencyReport />}
        </Suspense>
      </Box>
    </Box>
  </Box>
}

function Navigation({ items, current, currentUser, onNavigate }: { items: NavItem[]; current: Section; currentUser: Usuario; onNavigate: (value: Section) => void }) {
  const groups: NavGroup[] = ['Visão geral', 'Gestão', 'Operação', 'Análises']
  return <Stack sx={{ height: '100%', bgcolor: 'background.paper' }}>
    <Stack direction="row" alignItems="center" spacing={1.4} sx={{ height: 76, px: 2.5, borderBottom: '1px solid', borderColor: 'divider' }}><Box sx={{ width: 42, height: 42, borderRadius: 2.5, display: 'grid', placeItems: 'center', color: '#fff', bgcolor: 'primary.main' }}><Diversity3Rounded /></Box><Box><Typography variant="h6" color="primary.dark">SGD</Typography><Typography variant="caption" color="text.secondary">Gestão de discipulados</Typography></Box></Stack>
    <List role="tablist" sx={{ flexGrow: 1, overflowY: 'auto', px: 1.5, py: 2 }}>
      {groups.map((group) => {
        const groupItems = items.filter((item) => item.group === group)
        return groupItems.length ? <Box component="li" key={group} sx={{ listStyle: 'none', mb: 1.5 }}><Typography variant="overline" color="text.secondary" sx={{ px: 1.5, fontSize: '.66rem', fontWeight: 700 }}>{group}</Typography>{groupItems.map((item) => <ListItemButton component="button" role="tab" aria-selected={current === item.value} key={item.value} selected={current === item.value} onClick={() => onNavigate(item.value)} sx={{ width: '100%', borderRadius: 2, mt: 0.4, minHeight: 44, '&.Mui-selected': { bgcolor: '#EEF1FF', color: 'primary.dark', '&:hover': { bgcolor: '#E5E9FF' } } }}><ListItemIcon sx={{ minWidth: 38, color: current === item.value ? 'primary.main' : 'text.secondary', '& svg': { fontSize: 21 } }}>{item.icon}</ListItemIcon><ListItemText primary={item.label} primaryTypographyProps={{ fontSize: '.875rem', fontWeight: current === item.value ? 650 : 500 }} /></ListItemButton>)}</Box> : null
      })}
    </List>
    <Box sx={{ p: 2, borderTop: '1px solid', borderColor: 'divider' }}><Stack direction="row" alignItems="center" spacing={1.25}><Avatar sx={{ width: 38, height: 38, bgcolor: '#EEF1FF', color: 'primary.main', fontSize: '.9rem', fontWeight: 700 }}>{initials(currentUser.nome)}</Avatar><Box minWidth={0}><Typography variant="body2" fontWeight={650} noWrap>{currentUser.nome}</Typography><Typography variant="caption" color="text.secondary" noWrap display="block">{currentUser.perfis.map((role) => roleLabel[role]).join(', ')}</Typography></Box></Stack></Box>
  </Stack>
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
  const podeAdministrar = currentUser.perfis.includes('ADMIN')
  return <Stack spacing={3}><PageHeader title={podeAdministrar?'Encontros e frequência':'Registrar frequência'} description={podeAdministrar?'Registre chamadas ou justifique encontros que não foram realizados.':'Crie encontros e registre a presença do discipulado.'} action={<FormControl sx={{ minWidth: { xs: 240, sm: 320 } }}><InputLabel>Discipulado</InputLabel><Select label="Discipulado" value={discipuladoId || ''} onChange={(event) => setDiscipuladoId(Number(event.target.value))}>{discipulados.map((item) => <MenuItem key={item.id} value={item.id}>{item.nome}</MenuItem>)}</Select></FormControl>} />{discipuladoId ? <FrequencyManagement discipuladoId={discipuladoId} podeAdministrar={podeAdministrar} /> : <EmptyState title="Nenhum discipulado disponível" description="Não há discipulados ativos no seu escopo." />}</Stack>
}

function initials(name: string) { return name.split(' ').filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase() }
