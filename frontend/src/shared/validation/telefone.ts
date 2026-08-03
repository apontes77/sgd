/** Retorna os dígitos do telefone, removendo DDI 55 quando presente. */
export function digitosTelefone(valor: string | undefined | null): string {
  const digits = (valor ?? '').replace(/\D/g, '')
  if ((digits.length === 12 || digits.length === 13) && digits.startsWith('55')) {
    return digits.slice(2)
  }
  return digits
}

/** Telefone brasileiro com DDD: 10 (fixo) ou 11 (celular) dígitos. Vazio é válido. */
export function telefoneValido(valor: string | undefined | null): boolean {
  if (!valor?.trim()) return true
  const digits = digitosTelefone(valor)
  return digits.length === 10 || digits.length === 11
}

export function mensagemTelefoneInvalido(rotulo: string): string {
  return `O ${rotulo} deve ser um telefone válido com DDD (10 ou 11 dígitos).`
}

export function contatoMinimoValido(dados: {
  nomeMae?: string
  telefoneMae?: string
  nomePai?: string
  telefonePai?: string
  responsavelNome?: string
  responsavelTelefone?: string
}): boolean {
  const mae = Boolean(dados.nomeMae?.trim()) && Boolean(dados.telefoneMae?.trim()) && telefoneValido(dados.telefoneMae)
  const pai = Boolean(dados.nomePai?.trim()) && Boolean(dados.telefonePai?.trim()) && telefoneValido(dados.telefonePai)
  const responsavel =
    Boolean(dados.responsavelNome?.trim()) &&
    Boolean(dados.responsavelTelefone?.trim()) &&
    telefoneValido(dados.responsavelTelefone)
  return mae || pai || responsavel
}

/** Discípulo GOE exige telefone do adolescente; demais categorias exigem contato familiar. */
export function validarContatosPorCategoria(
  categoria: 'DISCIPULO' | 'VISITANTE' | 'DISCIPULO_GOE',
  dados: {
    telefone?: string
    nomeMae?: string
    telefoneMae?: string
    nomePai?: string
    telefonePai?: string
    responsavelNome?: string
    responsavelTelefone?: string
  },
): string | null {
  if (categoria === 'DISCIPULO_GOE') {
    if (!dados.telefone?.trim()) return 'O telefone do adolescente é obrigatório para discípulo GOE.'
    if (!telefoneValido(dados.telefone)) return mensagemTelefoneInvalido('telefone do adolescente')
    return null
  }
  if (!contatoMinimoValido(dados)) {
    return 'Informe nome e telefone da mãe, ou do pai, ou do responsável.'
  }
  return null
}
