import {
  AccountTreeRounded,
  AssessmentRounded,
  Brightness4Rounded,
  Brightness7Rounded,
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
  Autocomplete,
  Avatar,
  BottomNavigation,
  BottomNavigationAction,
  Box,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Stack,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import { motion, useReducedMotion } from 'framer-motion'
import type { ReactNode } from 'react'
import { lazy, Suspense, useEffect, useMemo, useState } from 'react'

import { type AppSection, navigateToSection, resolveInitialSection } from '@/app/appNavigation'
import { useColorMode } from '@/app/useColorMode'
import AdolescentManagement from '@/features/adolescentes/AdolescentManagement'
import FrequencyManagement from '@/features/frequencia/FrequencyManagement'
import { organizationApi } from '@/features/organizacao/api'
import OrganizationManagement from '@/features/organizacao/OrganizationManagement'
import { userManagementClient } from '@/features/users/api'
import UserManagement from '@/features/users/UserManagement'
import { type Discipulado, labelDiscipulado, type Perfil, type Usuario } from '@/shared/api/types'
import { BOTTOM_NAV_OFFSET, EmptyState, LoadingState, PageHeader } from '@/shared/ui'

const ExecutiveDashboard = lazy(() => import('@/features/dashboards/ExecutiveDashboard'))
const AdminDashboard = lazy(() => import('@/features/dashboards/AdminDashboard'))
const ManagerDashboard = lazy(() => import('@/features/dashboards/ManagerDashboard'))
const LeaderDashboard = lazy(() => import('@/features/dashboards/LeaderDashboard'))
const FrequencyReport = lazy(() => import('@/features/relatorios/FrequencyReport'))

type Section = AppSection
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
  const { mode, toggleMode } = useColorMode()
  const reducedMotion = useReducedMotion()
  const isAdmin = currentUser.perfis.includes('ADMIN')
  const isGerente = currentUser.perfis.includes('GERENTE')
  const sections = useMemo(() => {
    const values: NavItem[] = []
    if (isAdmin || isGerente)
      values.push({
        value: 'visao-executiva',
        label: 'Visão executiva',
        shortLabel: 'Executiva',
        group: 'Dashboards & BI',
        icon: <InsightsRounded />,
      })
    if (isAdmin)
      values.push({
        value: 'painel',
        label: 'Painel',
        shortLabel: 'Painel',
        group: 'Dashboards & BI',
        icon: <DashboardRounded />,
      })
    if (isGerente)
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
    if (isAdmin)
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
        label: isAdmin ? 'Encontros e frequência' : 'Registrar frequência',
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
  }, [currentUser.perfis, isAdmin, isGerente])
  const available = useMemo(() => sections.map((item) => item.value), [sections])
  const [section, setSection] = useState<Section>(() => resolveInitialSection(available))
  const [moreOpen, setMoreOpen] = useState(false)
  const currentSection = sections.find((item) => item.value === section) ?? sections[0]
  const bottomPrimary = useMemo(() => primaryBottomItems(sections), [sections])
  const bottomPrimaryIds = useMemo(() => new Set(bottomPrimary.map((item) => item.value)), [bottomPrimary])
  const moreItems = useMemo(
    () => sections.filter((item) => !bottomPrimaryIds.has(item.value)),
    [sections, bottomPrimaryIds],
  )
  const bottomValue = bottomPrimaryIds.has(section) ? section : 'mais'
  const escopoExecutivo = isAdmin ? 'admin' : 'gerencia'
  const detalheExecutivo: Section = isAdmin ? 'painel' : 'minha-gerencia'

  useEffect(() => {
    if (!available.includes(section)) {
      const next = resolveInitialSection(available)
      setSection(next)
      navigateToSection(next, true)
      return
    }
    navigateToSection(section, true)
  }, [available, section])

  useEffect(() => {
    const onPopState = () => setSection(resolveInitialSection(available))
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [available])

  function navigate(value: Section) {
    setSection(value)
    navigateToSection(value)
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
          borderColor: (theme) => theme.app.border.subtle,
          bgcolor: (theme) => theme.app.surface.glass,
          backdropFilter: 'blur(20px)',
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
          <Tooltip title={mode === 'light' ? 'Ativar modo escuro' : 'Ativar modo claro'}>
            <IconButton onClick={toggleMode} color="inherit" aria-label="Alternar tema">
              {mode === 'light' ? <Brightness4Rounded /> : <Brightness7Rounded />}
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>
      <Box component="nav" aria-label="Navegação principal" sx={{ display: { xs: 'none', md: 'block' } }}>
        <Drawer
          variant="permanent"
          sx={{
            '& .MuiDrawer-paper': {
              width: drawerWidth,
              boxSizing: 'border-box',
              borderRightColor: (theme) => theme.app.border.subtle,
              boxShadow: (theme) => theme.app.shadow.drawer,
            },
          }}
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
            <motion.div
              key={section}
              initial={reducedMotion ? { opacity: 1, y: 0 } : { opacity: 0, y: -12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: reducedMotion ? 0 : 0.15, ease: 'easeOut' }}
            >
              {section === 'visao-executiva' && (
                <ExecutiveDashboard escopo={escopoExecutivo} onAbrirDetalhe={() => navigate(detalheExecutivo)} />
              )}
              {section === 'painel' && <AdminDashboard />}
              {section === 'minha-gerencia' && <ManagerDashboard />}
              {section === 'meu-discipulado' && <LeaderDashboard />}
              {section === 'estrutura' && <OrganizationManagement />}
              {section === 'usuarios' && <UserManagement client={userManagementClient} />}
              {section === 'adolescentes' && (
                <AdolescentManagement
                  podeAnonimizar={isAdmin}
                  podeEditar={currentUser.perfis.some(
                    (perfil) => perfil === 'ADMIN' || perfil === 'DISCIPULADOR' || perfil === 'CO_LIDER',
                  )}
                />
              )}
              {section === 'frequencia' && <FrequencyPage currentUser={currentUser} />}
              {section === 'relatorios' && <FrequencyReport currentUser={currentUser} />}
            </motion.div>
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
          borderColor: (theme) => theme.app.border.subtle,
          bgcolor: (theme) => theme.app.surface.elevated,
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
            borderTopLeftRadius: '16px',
            borderTopRightRadius: '16px',
            maxHeight: '85vh',
            pb: 'env(safe-area-inset-bottom, 0px)',
            bgcolor: (theme) => theme.app.surface.elevated,
            borderTop: '1px solid',
            borderColor: (theme) => theme.app.border.subtle,
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
                sx={{
                  minHeight: 48,
                  px: 2.5,
                  '&.Mui-selected': {
                    bgcolor: (theme) => alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.22 : 0.08),
                    color: (theme) =>
                      theme.palette.mode === 'dark' ? theme.palette.common.white : theme.palette.primary.dark,
                    '&:hover': {
                      bgcolor: (theme) => alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.3 : 0.12),
                    },
                  },
                }}
              >
                <ListItemIcon
                  sx={{
                    minWidth: 40,
                    color: (theme) =>
                      section === item.value
                        ? theme.palette.mode === 'dark'
                          ? theme.palette.common.white
                          : theme.palette.primary.main
                        : theme.palette.text.secondary,
                  }}
                >
                  {item.icon}
                </ListItemIcon>
                <ListItemText
                  primary={item.label}
                  primaryTypographyProps={{
                    sx: {
                      color: (theme) =>
                        section === item.value
                          ? theme.palette.mode === 'dark'
                            ? theme.palette.common.white
                            : theme.palette.primary.dark
                          : 'inherit',
                    },
                  }}
                />
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
                bgcolor: 'primary.main',
                color: 'primary.contrastText',
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
    <Stack
      sx={{
        height: '100%',
        bgcolor: (theme) => theme.app.surface.elevated,
        backgroundImage: (theme) => theme.app.gradient.surface,
      }}
    >
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
            color: 'primary.contrastText',
            bgcolor: 'primary.main',
          }}
        >
          <Diversity3Rounded />
        </Box>
        <Box>
          <Typography
            variant="h6"
            sx={{
              color: (theme) =>
                theme.palette.mode === 'dark' ? theme.palette.common.white : theme.palette.primary.dark,
            }}
          >
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
                    '&.Mui-selected': {
                      bgcolor: (theme) =>
                        alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.22 : 0.08),
                      color: (theme) =>
                        theme.palette.mode === 'dark' ? theme.palette.common.white : theme.palette.primary.dark,
                      '&:hover': {
                        bgcolor: (theme) =>
                          alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.3 : 0.12),
                      },
                      '& .MuiListItemIcon-root': {
                        color: (theme) =>
                          theme.palette.mode === 'dark' ? theme.palette.common.white : theme.palette.primary.main,
                      },
                    },
                  }}
                >
                  <ListItemIcon
                    sx={{
                      minWidth: 38,
                      color: (theme) =>
                        current === item.value
                          ? theme.palette.mode === 'dark'
                            ? theme.palette.common.white
                            : theme.palette.primary.main
                          : theme.palette.text.secondary,
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
              bgcolor: 'primary.main',
              color: 'primary.contrastText',
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
            <Autocomplete
              sx={{ minWidth: { xs: '100%', sm: 320 }, width: { xs: '100%', sm: 'auto' } }}
              options={discipulados}
              value={discipulados.find((item) => item.id === discipuladoId) ?? null}
              onChange={(_, value) => setDiscipuladoId(value?.id ?? 0)}
              getOptionLabel={labelDiscipulado}
              isOptionEqualToValue={(option, value) => option.id === value.id}
              noOptionsText="Nenhum discipulado encontrado"
              renderInput={(params) => (
                <TextField {...params} label="Discipulado" placeholder="Pesquisar discipulado ou discipulador" />
              )}
            />
          ) : undefined
        }
      />
      {discipuladoId ? (
        <FrequencyManagement
          discipuladoId={discipuladoId}
          discipulado={discipulados.find((item) => item.id === discipuladoId)}
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
