export const PASSWORD_MIN_LENGTH = 6

export const PASSWORD_REQUIREMENTS = [
  {
    key: 'minLength',
    label: 'Pelo menos 6 caracteres',
    test: (value: string) => value.length >= PASSWORD_MIN_LENGTH,
  },
  { key: 'uppercase', label: '1 letra maiúscula', test: (value: string) => /[A-Z]/.test(value) },
  { key: 'lowercase', label: '1 letra minúscula', test: (value: string) => /[a-z]/.test(value) },
  { key: 'number', label: '1 número', test: (value: string) => /\d/.test(value) },
  { key: 'special', label: '1 caractere especial', test: (value: string) => /[^A-Za-z0-9]/.test(value) },
] as const

export type PasswordRequirementKey = (typeof PASSWORD_REQUIREMENTS)[number]['key']

export function passwordMeetsPolicy(password: string): boolean {
  return PASSWORD_REQUIREMENTS.every((requirement) => requirement.test(password))
}
