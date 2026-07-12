export interface Pagina<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
export interface Adolescente { id: number; nome: string; dataNascimento: string; telefone?: string; instagram?: string; discipuladoId: number; ativo: boolean }
export interface AdolescenteInput { nome: string; dataNascimento: string; telefone?: string; instagram?: string; discipuladoId: number; ativo?: boolean }
export interface Vinculo { id: number; adolescenteId: number; discipuladoId: number; dataInicio: string; dataFim?: string; ativo: boolean }
export interface DiscipuladoResumo { id: number; nome: string; ativo?: boolean }

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = sessionStorage.getItem('sgd.access-token')
  const response = await fetch(`/api/v1${path}`, { ...options, headers: { Accept: 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers } })
  if (!response.ok) {
    const problem = await response.json().catch(() => null) as { detail?: string; title?: string; message?: string } | null
    throw new Error(problem?.detail ?? problem?.message ?? problem?.title ?? 'Não foi possível concluir a operação.')
  }
  return (response.status === 204 ? undefined : response.json()) as Promise<T>
}

export const adolescentesApi = {
  listar: (discipuladoId?: number, ativo?: boolean) => {
    const params = new URLSearchParams({ page: '0', size: '100' })
    if (discipuladoId) params.set('discipuladoId', String(discipuladoId))
    if (ativo !== undefined) params.set('ativo', String(ativo))
    return request<Pagina<Adolescente>>(`/adolescentes?${params}`)
  },
  listarDiscipulados: () => request<Pagina<DiscipuladoResumo>>('/discipulados?ativo=true&page=0&size=100'),
  criar: (body: AdolescenteInput) => request<Adolescente>('/adolescentes', { method: 'POST', body: JSON.stringify(body) }),
  atualizar: (id: number, body: AdolescenteInput) => request<Adolescente>(`/adolescentes/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  transferir: (id: number, discipuladoId: number, dataInicio: string) => request<Vinculo>(`/adolescentes/${id}/vinculos`, { method: 'POST', body: JSON.stringify({ discipuladoId, dataInicio }) }),
}
