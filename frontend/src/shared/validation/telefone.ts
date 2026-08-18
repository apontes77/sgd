export function telefoneValido(valor?: string): boolean {
  if (!valor?.trim()) return true
  const digits = valor.replace(/\D/g, '')
  const local = digits.startsWith('55') && (digits.length === 12 || digits.length === 13) ? digits.slice(2) : digits
  return local.length === 10 || local.length === 11
}

export function mensagemTelefoneInvalido(rotulo: string): string {
  return `O ${rotulo} deve ser um telefone válido com DDD (10 ou 11 dígitos).`
}

export function validarTelefoneAdolescente(
  categoria: string,
  dados: {
    telefone?: string
    naoPossuiTelefone?: boolean
  },
): string | null {
  if (categoria === 'DISCIPULO_GOE') {
    if (dados.naoPossuiTelefone) return null
    if (!dados.telefone?.trim()) return 'Informe o telefone do adolescente ou marque que não possui telefone.'
  }
  if (!telefoneValido(dados.telefone)) return mensagemTelefoneInvalido('telefone do adolescente')
  return null
}
