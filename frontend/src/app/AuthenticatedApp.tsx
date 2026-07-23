import {
  AccountTreeRounded,
  AssessmentRounded,
  DashboardRounded,
  Diversity3Rounded,
  FactCheckRounded,
  GroupsRounded,
  InsightsRounded,
  LogoutRounded,
  MoreHorizRounded,
  PeopleAltRounded,
} from '@mui/icons-material'
import {
  AppBar,
  Avatar,
  BottomNavigation,
  BottomNavigationAction,
  Box,
  Divider,
  Drawer,
  FormControl,
  InputLabel,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  MenuItem,
  Select,
  Stack,
  Toolbar,
  Typography,
} from '@mui/material'
import type { ReactNode } from 'react'
import { lazy, Suspense, useEffect, useMemo, useState } from 'react'

import AdolescentManagement from '@/features/adolescentes/AdolescentManagement'
import FrequencyManagement from '@/features/frequencia/FrequencyManagement'
import { organizationApi } from '@/features/organizacao/api'
import OrganizationManagement from '@/features/organizacao/OrganizationManagement'
import { userManagementClient } from '@/features/users/api'
import UserManagement from '@/features/users/UserManagement'
import type { Discipulado, Perfil, Usuario } from '@/shared/api/types'
import { BOTTOM_NAV_OFFSET, EmptyState, LoadingState, PageHeader } from '@/shared/ui'

const ExecutiveDashboard = lazy(() => import('@/features/dashboards/ExecutiveDashboard'))
const AdminDashboard = lazy(() => import('@/features/dashboards/AdminDashboard'))
const ManagerDashboard = lazy(() => import('@/features/dashboards/ManagerDashboard'))
const LeaderDashboard = lazy(() => import('@/features/dashboards/LeaderDashboard'))
const FrequencyReport = lazy(() => import('@/features/relatorios/FrequencyReport'))

type Section =
  | 'visao-executiva'
  | 'painel'
  | 'minha-gerencia'
  | 'meu-discipulado'
  | 'estrutura'
  | 'usuarios'
  | 'adolescentes'
  | 'frequencia'
  | 'relatorios'
type NavGroup = 'Dashboards & BI' | 'Cadastros' | 'Operações' | 'Relatórios'
type NavItem = { value: Section; label: string; shortLabel?: string; group: NavGroup; icon: ReactNode }
const drawerWidth = 264
const roleLabel: Record<Perfil, string> = {
  ADMIN: 'Administrador',
  GERENTE: 'Gerente',
  DISCIPULADOR: 'Discipulador',
  CO_LIDER: 'Co-líder',
}

function primaryBottomItems(items: NavItem[]): NavItem[] {
  const overview = items.find((item) => item.group === 'Dashboards & BI')
  const adolescentes = items.find((item) => item.value === 'adolescentes')
  const frequencia = items.find((item) => item.value === 'frequencia')
  const relatorios = items.find((item) => item.value === 'relatorios')
  return [overview, adolescentes, frequencia, relatorios].filter((item): item is NavItem => Boolean(item))
}

export default function AuthenticatedApp({ currentUser, onLogout }: { currentUser: Usuario; onLogout: () => void }) {
  const sections = useMemo(() => {
    const values: NavItem[] = []
    if (currentUser.perfis.includes('ADMIN'))
      values.push({
        value: 'visao-executiva',
        label: 'Visão executiva',
        shortLabel: 'Executiva',
        group: 'Dashboards & BI',
        icon: <InsightsRounded />,
      })
    if (currentUser.perfis.includes('ADMIN'))
      values.push({
        value: 'painel',
        label: 'Painel',
        shortLabel: 'Painel',
        group: 'Dashboards & BI',
        icon: <DashboardRounded />,
      })
    if (currentUser.perfis.includes('GERENTE'))
      values.push({
        value: 'minha-gerencia',
        label: 'Minha gerência',
        shortLabel: 'Gerência',
        group: 'Dashboards & BI',
        icon: <DashboardRounded />,
      })
    if (currentUser.perfis.some((role) => role === 'DISCIPULADOR' || role === 'CO_LIDER'))
      values.push({
        value: 'meu-discipulado',
        label: 'Meu discipulado',
        shortLabel: 'Discipulado',
        group: 'Dashboards & BI',
        icon: <DashboardRounded />,
      })
    if (currentUser.perfis.includes('ADMIN'))
      values.push(
        { value: 'estrutura', label: 'Estrutura', group: 'Cadastros', icon: <AccountTreeRounded /> },
        { value: 'usuarios', label: 'Usuários', group: 'Cadastros', icon: <PeopleAltRounded /> },
      )
    values.push({
      value: 'adolescentes',
      label: 'Adolescentes',
      shortLabel: 'Adolescentes',
      group: 'Cadastros',
      icon: <GroupsRounded />,
    })
    if (currentUser.perfis.some((role) => role === 'ADMIN' || role === 'DISCIPULADOR' || role === 'CO_LIDER'))
      values.push({
        value: 'frequencia',
        label: currentUser.perfis.includes('ADMIN') ? 'Encontros e frequência' : 'Registrar frequência',
        shortLabel: 'Frequência',
        group: 'Operações',
        icon: <FactCheckRounded />,
      })
    values.push({
      value: 'relatorios',
      label: 'Relatórios',
      shortLabel: 'Relatórios',
      group: 'Relatórios',
      icon: <AssessmentRounded />,
    })
    return values
  }, [currentUser.perfis])
  const [section, setSection] = useState<Section>(sections[0].value)
  const [moreOpen, setMoreOpen] = useState(false)
  const currentSection = sections.find((item) => item.value === section) ?? sections[0]
  const bottomPrimary = useMemo(() => primaryBottomItems(sections), [sections])
  const bottomPrimaryIds = useMemo(() => new Set(bottomPrimary.map((item) => item.value)), [bottomPrimary])
  const moreItems = useMemo(
    () => sections.filter((item) => !bottomPrimaryIds.has(item.value)),
    [sections, bottomPrimaryIds],
  )
  const bottomValue = bottomPrimaryIds.has(section) ? section : 'mais'

  function navigate(value: Section) {
    setSection(value)
    setMoreOpen(false)
  }

  const navigation = (
    <Navigation
      items={sections}
      current={section}
      currentUser={currentUser}
      onNavigate={navigate}
      onLogout={onLogout}
    />
  )
  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar
        position="fixed"
        color="inherit"
        elevation={0}
        sx={{
          width: { md: `calc(100% - ${drawerWidth}px)` },
          ml: { md: `${drawerWidth}px` },
          borderBottom: '1px solid',
          borderColor: 'divider',
          bgcolor: 'rgba(255,255,255,.94)',
          backdropFilter: 'blur(12px)',
          zIndex: (theme) => theme.zIndex.drawer - 1,
        }}
      >
        <Toolbar sx={{ minHeight: { xs: 64, md: 68 }, px: { xs: 2, md: 3 } }}>
          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="body2" color="text.secondary">
              {currentSection.group} / {currentSection.label}
            </Typography>
            <Typography variant="h6">{currentSection.label}</Typography>
          </Box>
        </Toolbar>
      </AppBar>
      <Box component="nav" aria-label="Navegação principal" sx={{ display: { xs: 'none', md: 'block' } }}>
        <Drawer
          variant="permanent"
          sx={{ '& .MuiDrawer-paper': { width: drawerWidth, boxSizing: 'border-box', borderRightColor: 'divider' } }}
          open
        >
          {navigation}
        </Drawer>
      </Box>
      <Box
        component="main"
        sx={{
          ml: { md: `${drawerWidth}px` },
          pt: { xs: '64px', md: '68px' },
          pb: { xs: BOTTOM_NAV_OFFSET, md: 0 },
          minHeight: '100vh',
        }}
      >
        <Box sx={{ width: '100%', maxWidth: 1600, mx: 'auto', p: { xs: 2, sm: 3, lg: 4 } }}>
          <Suspense fallback={<LoadingState label="Carregando módulo..." />}>
            {section === 'visao-executiva' && <ExecutiveDashboard />}
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
      <Box
        component="nav"
        aria-label="Navegação móvel"
        sx={{
          display: { xs: 'block', md: 'none' },
          position: 'fixed',
          left: 0,
          right: 0,
          bottom: 0,
          zIndex: (theme) => theme.zIndex.appBar,
          borderTop: '1px solid',
          borderColor: 'divider',
          bgcolor: 'background.paper',
          pb: 'env(safe-area-inset-bottom, 0px)',
        }}
      >
        <BottomNavigation
          showLabels
          value={bottomValue}
          onChange={(_, value: Section | 'mais') => {
            if (value === 'mais') setMoreOpen(true)
            else navigate(value)
          }}
          sx={{ height: 56, '& .MuiBottomNavigationAction-root': { minWidth: 0, px: 0.5 } }}
        >
          {bottomPrimary.map((item) => (
            <BottomNavigationAction
              key={item.value}
              value={item.value}
              label={item.shortLabel ?? item.label}
              icon={item.icon}
              aria-label={item.label}
            />
          ))}
          <BottomNavigationAction value="mais" label="Mais" icon={<MoreHorizRounded />} aria-label="Mais opções" />
        </BottomNavigation>
      </Box>
      <Drawer
        anchor="bottom"
        open={moreOpen}
        onClose={() => setMoreOpen(false)}
        ModalProps={{ keepMounted: false }}
        PaperProps={{
          sx: {
            borderTopLeftRadius: 16,
            borderTopRightRadius: 16,
            maxHeight: '85vh',
            pb: 'env(safe-area-inset-bottom, 0px)',
          },
        }}
        sx={{ display: { xs: 'block', md: 'none' } }}
      >
        <Stack sx={{ pt: 1.5, pb: 1 }}>
          <Typography variant="subtitle2" color="text.secondary" sx={{ px: 2.5, pb: 1 }}>
            Mais
          </Typography>
          <List disablePadding>
            {moreItems.map((item) => (
              <ListItemButton
                key={item.value}
                selected={section === item.value}
                onClick={() => navigate(item.value)}
                sx={{ minHeight: 48, px: 2.5 }}
              >
                <ListItemIcon sx={{ minWidth: 40, color: section === item.value ? 'primary.main' : 'text.secondary' }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText primary={item.label} />
              </ListItemButton>
            ))}
          </List>
          <Divider sx={{ my: 1 }} />
          <ListItemButton
            onClick={() => {
              setMoreOpen(false)
              onLogout()
            }}
            sx={{ minHeight: 48, px: 2.5 }}
          >
            <ListItemIcon sx={{ minWidth: 40, color: 'text.secondary' }}>
              <LogoutRounded />
            </ListItemIcon>
            <ListItemText primary="Sair" />
          </ListItemButton>
          <Stack direction="row" alignItems="center" spacing={1.25} sx={{ px: 2.5, py: 2 }}>
            <Avatar
              sx={{
                width: 38,
                height: 38,
                bgcolor: '#EEF1FF',
                color: 'primary.main',
                fontSize: '.9rem',
                fontWeight: 700,
              }}
            >
              {initials(currentUser.nome)}
            </Avatar>
            <Box minWidth={0}>
              <Typography variant="body2" fontWeight={650} noWrap>
                {currentUser.nome}
              </Typography>
              <Typography variant="caption" color="text.secondary" noWrap display="block">
                {currentUser.perfis.map((role) => roleLabel[role]).join(', ')}
              </Typography>
            </Box>
          </Stack>
        </Stack>
      </Drawer>
    </Box>
  )
}

function Navigation({
  items,
  current,
  currentUser,
  onNavigate,
  onLogout,
}: {
  items: NavItem[]
  current: Section
  currentUser: Usuario
  onNavigate: (value: Section) => void
  onLogout: () => void
}) {
  const groups: NavGroup[] = ['Dashboards & BI', 'Cadastros', 'Operações', 'Relatórios']
  return (
    <Stack sx={{ height: '100%', bgcolor: 'background.paper' }}>
      <Stack
        direction="row"
        alignItems="center"
        spacing={1.4}
        sx={{ height: 76, px: 2.5, borderBottom: '1px solid', borderColor: 'divider' }}
      >
        <Box
          sx={{
            width: 42,
            height: 42,
            borderRadius: 2.5,
            display: 'grid',
            placeItems: 'center',
            color: '#fff',
            bgcolor: 'primary.main',
          }}
        >
          <Diversity3Rounded />
        </Box>
        <Box>
          <Typography variant="h6" color="primary.dark">
            SGD
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Gestão de discipulados
          </Typography>
        </Box>
      </Stack>
      <List role="tablist" sx={{ flexGrow: 1, overflowY: 'auto', px: 1.5, py: 2 }}>
        {groups.map((group) => {
          const groupItems = items.filter((item) => item.group === group)
          return groupItems.length ? (
            <Box component="li" key={group} sx={{ listStyle: 'none', mb: 1.5 }}>
              <Typography
                variant="overline"
                color="text.secondary"
                sx={{ px: 1.5, fontSize: '.66rem', fontWeight: 700 }}
              >
                {group}
              </Typography>
              {groupItems.map((item) => (
                <ListItemButton
                  component="button"
                  role="tab"
                  aria-selected={current === item.value}
                  key={item.value}
                  selected={current === item.value}
                  onClick={() => onNavigate(item.value)}
                  sx={{
                    width: '100%',
                    borderRadius: 2,
                    mt: 0.4,
                    minHeight: 44,
                    '&.Mui-selected': { bgcolor: '#EEF1FF', color: 'primary.dark', '&:hover': { bgcolor: '#E5E9FF' } },
                  }}
                >
                  <ListItemIcon
                    sx={{
                      minWidth: 38,
                      color: current === item.value ? 'primary.main' : 'text.secondary',
                      '& svg': { fontSize: 21 },
                    }}
                  >
                    {item.icon}
                  </ListItemIcon>
                  <ListItemText
                    primary={item.label}
                    primaryTypographyProps={{ fontSize: '.875rem', fontWeight: current === item.value ? 650 : 500 }}
                  />
                </ListItemButton>
              ))}
            </Box>
          ) : null
        })}
      </List>
      <ListItemButton
        component="button"
        onClick={onLogout}
        sx={{ width: '100%', borderRadius: 2, mt: 0.5, minHeight: 44 }}
      >
        <ListItemIcon sx={{ minWidth: 38, color: 'text.secondary', '& svg': { fontSize: 21 } }}>
          <LogoutRounded />
        </ListItemIcon>
        <ListItemText primary="Sair" primaryTypographyProps={{ fontSize: '.875rem', fontWeight: 500 }} />
      </ListItemButton>
      <Box sx={{ p: 2, borderTop: '1px solid', borderColor: 'divider' }}>
        <Stack direction="row" alignItems="center" spacing={1.25}>
          <Avatar
            sx={{
              width: 38,
              height: 38,
              bgcolor: '#EEF1FF',
              color: 'primary.main',
              fontSize: '.9rem',
              fontWeight: 700,
            }}
          >
            {initials(currentUser.nome)}
          </Avatar>
          <Box minWidth={0}>
            <Typography variant="body2" fontWeight={650} noWrap>
              {currentUser.nome}
            </Typography>
            <Typography variant="caption" color="text.secondary" noWrap display="block">
              {currentUser.perfis.map((role) => roleLabel[role]).join(', ')}
            </Typography>
          </Box>
        </Stack>
      </Box>
    </Stack>
  )
}

function FrequencyPage({ currentUser }: { currentUser: Usuario }) {
  const [discipulados, setDiscipulados] = useState<Discipulado[]>([])
  const [discipuladoId, setDiscipuladoId] = useState<number>(0)
  useEffect(() => {
    const consulta = currentUser.perfis.includes('ADMIN')
      ? organizationApi.listarDiscipulados(true).then((page) => page.content)
      : organizationApi.listarDiscipuladosLiderados(true)
    consulta
      .then((items) => {
        setDiscipulados(items)
        setDiscipuladoId((current) => (items.some((item) => item.id === current) ? current : (items[0]?.id ?? 0)))
      })
      .catch(() => {
        setDiscipulados([])
        setDiscipuladoId(0)
      })
  }, [currentUser.perfis])
  const podeAdministrar = currentUser.perfis.includes('ADMIN')
  const podeRegistrarNaoRealizacao =
    podeAdministrar || currentUser.perfis.some((role) => role === 'DISCIPULADOR' || role === 'CO_LIDER')
  const mostrarSeletor = discipulados.length > 1
  return (
    <Stack spacing={3}>
      <PageHeader
        title="Registrar frequência"
        description="Escolha a data e informe se houve discipulado."
        action={
          mostrarSeletor ? (
            <FormControl sx={{ minWidth: { xs: 240, sm: 320 } }}>
              <InputLabel>Discipulado</InputLabel>
              <Select
                label="Discipulado"
                value={discipuladoId || ''}
                onChange={(event) => setDiscipuladoId(Number(event.target.value))}
              >
                {discipulados.map((item) => (
                  <MenuItem key={item.id} value={item.id}>
                    {item.nome}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          ) : undefined
        }
      />
      {discipuladoId ? (
        <FrequencyManagement
          discipuladoId={discipuladoId}
          podeAdministrar={podeAdministrar}
          podeRegistrarNaoRealizacao={podeRegistrarNaoRealizacao}
        />
      ) : (
        <EmptyState title="Nenhum discipulado disponível" description="Não há discipulados ativos no seu escopo." />
      )}
    </Stack>
  )
}

function initials(name: string) {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase()
}
