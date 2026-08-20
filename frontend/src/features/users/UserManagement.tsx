import { AddRounded, EditRounded, PersonAddRounded } from '@mui/icons-material'
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  LinearProgress,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useEffect, useState } from 'react'

import type { Pagina, Perfil, Usuario } from '@/shared/api/types'
import {
  DataTableCard,
  EmptyState,
  FilterToolbar,
  FormSheet,
  PageHeader,
  RowActionsMenu,
  StatusChip,
} from '@/shared/ui'

export interface UserManagementClient {
  list(page: number, size: number, active?: boolean): Promise<Pagina<Usuario>>
  create(body: { nome: string; email: string; senha: string; perfis: Perfil[] }): Promise<Usuario>
  update(id: number, body: { nome?: string; perfis?: Perfil[]; ativo?: boolean }): Promise<Usuario>
}

const roles: Perfil[] = ['ADMIN', 'GERENTE', 'DISCIPULADOR', 'CO_LIDER']
const roleLabel: Record<Perfil, string> = {
  ADMIN: 'Administrador',
  GERENTE: 'Gerente',
  DISCIPULADOR: 'Discipulador',
  CO_LIDER: 'Co-líder',
}
const emptyForm = { nome: '', email: '', senha: '', perfis: [] as Perfil[] }

export default function UserManagement({ client }: { client: UserManagementClient }) {
  const [result, setResult] = useState<Pagina<Usuario>>({
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
  })
  const [activeFilter, setActiveFilter] = useState('')
  const [form, setForm] = useState(emptyForm)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<Usuario>()
  const [pendingUser, setPendingUser] = useState<Usuario>()
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  async function load(page = result.page) {
    setLoading(true)
    setError('')
    try {
      setResult(await client.list(page, result.size, activeFilter === '' ? undefined : activeFilter === 'true'))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível carregar os usuários.')
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => {
    void load(0)
  }, [activeFilter]) // eslint-disable-line react-hooks/exhaustive-deps

  function openCreate() {
    setEditingUser(undefined)
    setForm(emptyForm)
    setDrawerOpen(true)
  }
  function openEdit(user: Usuario) {
    setEditingUser(user)
    setForm({ nome: user.nome, email: user.email, senha: '', perfis: [...user.perfis] })
    setDrawerOpen(true)
  }
  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setMessage('')
    setLoading(true)
    try {
      if (editingUser) {
        await client.update(editingUser.id, { nome: form.nome, perfis: form.perfis })
        setMessage('Usuário atualizado com sucesso.')
      } else {
        await client.create(form)
        setMessage('Usuário criado com sucesso.')
      }
      setForm(emptyForm)
      setEditingUser(undefined)
      setDrawerOpen(false)
      await load(editingUser ? result.page : 0)
    } catch (reason) {
      setError(
        reason instanceof Error
          ? reason.message
          : editingUser
            ? 'Não foi possível atualizar o usuário.'
            : 'Não foi possível criar o usuário.',
      )
    } finally {
      setLoading(false)
    }
  }
  async function toggleActive(user: Usuario) {
    setError('')
    setMessage('')
    try {
      await client.update(user.id, { ativo: !user.ativo })
      setMessage(user.ativo ? 'Usuário inativado e sessões revogadas.' : 'Usuário reativado.')
      setPendingUser(undefined)
      await load()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível atualizar o usuário.')
    }
  }
  function closeDrawer() {
    if (!loading) {
      setDrawerOpen(false)
      setEditingUser(undefined)
      setForm(emptyForm)
    }
  }

  return (
    <Stack spacing={3}>
      <PageHeader
        title="Gestão de usuários"
        description="Administre acessos, perfis e situação das contas."
        eyebrow="Gestão"
        action={
          <Button variant="contained" startIcon={<AddRounded />} onClick={openCreate}>
            Novo usuário
          </Button>
        }
      />
      {error && (
        <Alert severity="error" onClose={() => setError('')}>
          {error}
        </Alert>
      )}
      {message && (
        <Alert severity="success" onClose={() => setMessage('')}>
          {message}
        </Alert>
      )}
      <FilterToolbar>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          justifyContent="space-between"
          alignItems={{ sm: 'center' }}
          gap={2}
        >
          <TextField
            select
            label="Status"
            value={activeFilter}
            onChange={(e) => setActiveFilter(e.target.value)}
            sx={{ minWidth: { xs: '100%', sm: 220 }, width: { xs: '100%', sm: 'auto' } }}
          >
            <MenuItem value="">Todos</MenuItem>
            <MenuItem value="true">Ativos</MenuItem>
            <MenuItem value="false">Inativos</MenuItem>
          </TextField>
          <Typography variant="body2" color="text.secondary">
            {result.totalElements} usuário{result.totalElements === 1 ? '' : 's'}
          </Typography>
        </Stack>
      </FilterToolbar>
      <DataTableCard>
        {loading && <LinearProgress />}
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Nome</TableCell>
              <TableCell>E-mail</TableCell>
              <TableCell>Perfis</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Ações</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {result.content.map((user) => (
              <TableRow key={user.id} hover>
                <TableCell>
                  <Typography variant="body2" fontWeight={650}>
                    {user.nome}
                  </Typography>
                </TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>
                  {user.perfis.map((role) => (
                    <Chip
                      key={role}
                      label={roleLabel[role]}
                      size="small"
                      variant="outlined"
                      sx={{ mr: 0.5, my: 0.25 }}
                    />
                  ))}
                </TableCell>
                <TableCell>
                  <StatusChip active={Boolean(user.ativo)} />
                </TableCell>
                <TableCell align="right">
                  <RowActionsMenu
                    ariaLabel={`Ações de ${user.nome}`}
                    actions={[
                      { label: 'Editar', onClick: () => openEdit(user) },
                      {
                        label: user.ativo ? 'Inativar' : 'Reativar',
                        onClick: () => setPendingUser(user),
                        color: user.ativo ? 'warning' : 'primary',
                      },
                    ]}
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        {!loading && result.content.length === 0 && (
          <EmptyState title="Nenhum usuário encontrado" description="Ajuste o filtro ou cadastre um novo usuário." />
        )}
      </DataTableCard>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Button disabled={result.page === 0 || loading} onClick={() => void load(result.page - 1)}>
          Anterior
        </Button>
        <Typography variant="body2" color="text.secondary">
          Página {result.page + 1} de {Math.max(1, result.totalPages)}
        </Typography>
        <Button disabled={result.page + 1 >= result.totalPages || loading} onClick={() => void load(result.page + 1)}>
          Próxima
        </Button>
      </Stack>
      <FormSheet
        open={drawerOpen}
        onClose={closeDrawer}
        title={editingUser ? 'Editar usuário' : 'Novo usuário'}
        width={480}
        icon={editingUser ? <EditRounded color="primary" /> : <PersonAddRounded color="primary" />}
        component="form"
        onSubmit={submit}
        actions={
          <>
            <Button onClick={closeDrawer}>Cancelar</Button>
            <Button type="submit" variant="contained" disabled={loading || form.perfis.length === 0}>
              {editingUser ? 'Salvar alterações' : 'Cadastrar usuário'}
            </Button>
          </>
        }
      >
        {error && <Alert severity="error">{error}</Alert>}
        <TextField
          required
          autoFocus
          label="Nome"
          value={form.nome}
          onChange={(e) => setForm({ ...form, nome: e.target.value })}
        />
        <TextField
          required
          type="email"
          label="E-mail"
          value={form.email}
          disabled={Boolean(editingUser)}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
        />
        {!editingUser && (
          <TextField
            required
            type="password"
            inputProps={{ minLength: 12 }}
            label="Senha inicial"
            helperText="Mínimo de 12 caracteres"
            value={form.senha}
            onChange={(e) => setForm({ ...form, senha: e.target.value })}
          />
        )}
        <Box>
          <Typography variant="subtitle2" gutterBottom>
            Perfis de acesso
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            Um usuário pode acumular vários papéis. Marque Administrador para conceder gestão total a quem já é
            discipulador, gerente ou co-líder.
          </Typography>
          <Stack>
            {roles.map((role) => (
              <FormControlLabel
                key={role}
                control={
                  <Checkbox
                    checked={form.perfis.includes(role)}
                    onChange={(_, checked) =>
                      setForm({
                        ...form,
                        perfis: checked ? [...form.perfis, role] : form.perfis.filter((value) => value !== role),
                      })
                    }
                  />
                }
                label={roleLabel[role]}
              />
            ))}
          </Stack>
        </Box>
      </FormSheet>
      <Dialog open={Boolean(pendingUser)} onClose={() => setPendingUser(undefined)} maxWidth="xs" fullWidth>
        <DialogTitle>{pendingUser?.ativo ? 'Inativar usuário?' : 'Reativar usuário?'}</DialogTitle>
        <DialogContent>
          <Typography color="text.secondary">
            {pendingUser?.ativo
              ? `O acesso de ${pendingUser.nome} será bloqueado e as sessões ativas serão revogadas.`
              : `O acesso de ${pendingUser?.nome} será restaurado.`}
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPendingUser(undefined)}>Cancelar</Button>
          <Button
            variant="contained"
            color={pendingUser?.ativo ? 'warning' : 'primary'}
            onClick={() => pendingUser && void toggleActive(pendingUser)}
          >
            Confirmar
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
