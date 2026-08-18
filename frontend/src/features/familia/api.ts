import { request } from '@/shared/api/httpClient'
import type { Pagina } from '@/shared/api/types'

export const NAO_CONSTA = 'Não consta'

export type SituacaoIgrejaFamilia =
  'FDV_ATUANTES' | 'FDV_NAO_ATUANTES' | 'OUTRA_IGREJA' | 'NAO_CRISTA' | 'AFASTADA' | 'NAO_CONSTA'

export type SituacaoPaisFamilia =
  'CASADOS' | 'SEPARADOS' | 'FALECIDOS' | 'ADOTIVOS' | 'HOMOAFETIVOS' | 'NULOS' | 'NAO_CONSTA'

export type SituacaoFichaFamilia = 'PREENCHIDA' | 'NAO_CONSTA'

export interface ResponsavelFamiliaInput {
  nome: string
  parentesco: string
  dataNascimento?: string
  estadoCivil: string
  profissao: string
  telefone: string
  email: string
  interessePessoal: string
}

export interface FamiliaInput {
  cep: string
  rua: string
  numero: string
  complemento: string
  bairro: string
  cidade: string
  situacaoIgreja: SituacaoIgrejaFamilia
  atuaOnde: string
  situacaoPais: SituacaoPaisFamilia
  descricao: string
  desafioFinanceiro: boolean
  desafioEmocional: boolean
  desafioEspiritual: boolean
  desafiosDescricao: string
  atividadesJuntas: string
  rotinaSemana: string
  irmaoDokmos: string
  pedidoOracao: string
  intervencao: string
  observacaoDiscipulador: string
  observacaoGerente: string
  responsavel1: ResponsavelFamiliaInput
  responsavel2: ResponsavelFamiliaInput
}

export interface Familia extends FamiliaInput {
  id: number
  adolescenteId: number
  situacaoFicha: SituacaoFichaFamilia
}

export interface FamiliaResumo {
  id: number
  adolescenteId: number
  adolescenteNome: string
  discipuladoId: number
  discipuladoNome: string
  situacaoFicha: SituacaoFichaFamilia
  situacaoIgreja: SituacaoIgrejaFamilia
  situacaoPais: SituacaoPaisFamilia
}

export type FamiliaListagemFiltros = {
  busca?: string
  situacaoIgreja?: SituacaoIgrejaFamilia | ''
  situacaoPais?: SituacaoPaisFamilia | ''
}

export const SITUACAO_IGREJA_LABEL: Record<SituacaoIgrejaFamilia, string> = {
  FDV_ATUANTES: 'Pertencem à Igreja Fonte da Vida e são atuantes',
  FDV_NAO_ATUANTES: 'Pertencem à Igreja Fonte da Vida e não são atuantes',
  OUTRA_IGREJA: 'Pertencem a outra igreja',
  NAO_CRISTA: 'Família não cristã',
  AFASTADA: 'Já pertenceu à igreja e se afastou',
  NAO_CONSTA: NAO_CONSTA,
}

export const SITUACAO_PAIS_LABEL: Record<SituacaoPaisFamilia, string> = {
  CASADOS: 'Casados',
  SEPARADOS: 'Separados',
  FALECIDOS: 'Falecidos',
  ADOTIVOS: 'Adotivos',
  HOMOAFETIVOS: 'Homoafetivos',
  NULOS: 'Nulos (criado por parente)',
  NAO_CONSTA: NAO_CONSTA,
}

export function responsavelNaoConsta(): ResponsavelFamiliaInput {
  return {
    nome: NAO_CONSTA,
    parentesco: NAO_CONSTA,
    dataNascimento: '',
    estadoCivil: NAO_CONSTA,
    profissao: NAO_CONSTA,
    telefone: NAO_CONSTA,
    email: NAO_CONSTA,
    interessePessoal: NAO_CONSTA,
  }
}

export function familiaNaoConsta(): FamiliaInput {
  return {
    cep: NAO_CONSTA,
    rua: NAO_CONSTA,
    numero: NAO_CONSTA,
    complemento: NAO_CONSTA,
    bairro: NAO_CONSTA,
    cidade: NAO_CONSTA,
    situacaoIgreja: 'NAO_CONSTA',
    atuaOnde: NAO_CONSTA,
    situacaoPais: 'NAO_CONSTA',
    descricao: NAO_CONSTA,
    desafioFinanceiro: false,
    desafioEmocional: false,
    desafioEspiritual: false,
    desafiosDescricao: NAO_CONSTA,
    atividadesJuntas: NAO_CONSTA,
    rotinaSemana: NAO_CONSTA,
    irmaoDokmos: NAO_CONSTA,
    pedidoOracao: NAO_CONSTA,
    intervencao: NAO_CONSTA,
    observacaoDiscipulador: NAO_CONSTA,
    observacaoGerente: NAO_CONSTA,
    responsavel1: responsavelNaoConsta(),
    responsavel2: responsavelNaoConsta(),
  }
}

export function toFamiliaPayload(input: FamiliaInput): FamiliaInput {
  const limpar = (valor: string) => valor.trim() || NAO_CONSTA
  const limparResponsavel = (r: ResponsavelFamiliaInput): ResponsavelFamiliaInput => ({
    nome: limpar(r.nome),
    parentesco: limpar(r.parentesco),
    dataNascimento: r.dataNascimento?.trim() || undefined,
    estadoCivil: limpar(r.estadoCivil),
    profissao: limpar(r.profissao),
    telefone: limpar(r.telefone),
    email: limpar(r.email),
    interessePessoal: limpar(r.interessePessoal),
  })
  return {
    ...input,
    cep: limpar(input.cep),
    rua: limpar(input.rua),
    numero: limpar(input.numero),
    complemento: limpar(input.complemento),
    bairro: limpar(input.bairro),
    cidade: limpar(input.cidade),
    atuaOnde: input.situacaoIgreja === 'FDV_ATUANTES' ? limpar(input.atuaOnde) : NAO_CONSTA,
    descricao: limpar(input.descricao),
    desafiosDescricao: limpar(input.desafiosDescricao),
    atividadesJuntas: limpar(input.atividadesJuntas),
    rotinaSemana: limpar(input.rotinaSemana),
    irmaoDokmos: limpar(input.irmaoDokmos),
    pedidoOracao: limpar(input.pedidoOracao),
    intervencao: limpar(input.intervencao),
    observacaoDiscipulador: limpar(input.observacaoDiscipulador),
    observacaoGerente: limpar(input.observacaoGerente),
    responsavel1: limparResponsavel(input.responsavel1),
    responsavel2: limparResponsavel(input.responsavel2),
  }
}

/** Garante campos obrigatórios preenchidos (texto ou “Não consta”) antes do POST. */
export function validarFamiliaObrigatoria(input: FamiliaInput): string | undefined {
  const ficha = toFamiliaPayload(input)
  const obrigatorio = (valor: string, rotulo: string) => {
    if (!valor.trim()) return `Informe ${rotulo} ou “Não consta”.`
    return undefined
  }
  const campos: Array<[string, string]> = [
    [ficha.cep, 'o CEP'],
    [ficha.rua, 'a rua'],
    [ficha.numero, 'o número'],
    [ficha.bairro, 'o bairro'],
    [ficha.cidade, 'a cidade'],
    [ficha.responsavel1.nome, 'o nome do responsável 1'],
    [ficha.responsavel1.parentesco, 'o parentesco do responsável 1'],
    [ficha.responsavel1.telefone, 'o telefone do responsável 1'],
    [ficha.responsavel2.nome, 'o nome do responsável 2'],
  ]
  for (const [valor, rotulo] of campos) {
    const erro = obrigatorio(valor, rotulo)
    if (erro) return erro
  }
  if (ficha.situacaoIgreja === 'FDV_ATUANTES' && ficha.atuaOnde === NAO_CONSTA) {
    return 'Informe onde a família atua quando pertence à Igreja Fonte da Vida e é atuante.'
  }
  return undefined
}

export const familiaApi = {
  listar: (page = 0, size = 20, filtros: FamiliaListagemFiltros = {}) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    const busca = filtros.busca?.trim()
    if (busca) params.set('busca', busca)
    if (filtros.situacaoIgreja) params.set('situacaoIgreja', filtros.situacaoIgreja)
    if (filtros.situacaoPais) params.set('situacaoPais', filtros.situacaoPais)
    return request<Pagina<FamiliaResumo>>(`/familias?${params}`)
  },
  obter: (adolescenteId: number) => request<Familia>(`/adolescentes/${adolescenteId}/familia`),
  salvar: (adolescenteId: number, body: FamiliaInput) =>
    request<Familia>(`/adolescentes/${adolescenteId}/familia`, {
      method: 'PUT',
      body: JSON.stringify(toFamiliaPayload(body)),
    }),
}
