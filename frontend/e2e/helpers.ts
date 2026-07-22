import { expect, type Page } from '@playwright/test'

export function adminCredentials() {
  const email = process.env.E2E_ADMIN_EMAIL ?? process.env.ADMIN_INITIAL_EMAIL
  const password = process.env.E2E_ADMIN_PASSWORD ?? process.env.ADMIN_INITIAL_PASSWORD
  if (!email || !password) {
    throw new Error('Defina E2E_ADMIN_EMAIL/E2E_ADMIN_PASSWORD ou ADMIN_INITIAL_EMAIL/ADMIN_INITIAL_PASSWORD')
  }
  return { email, password }
}

export async function loginAsAdmin(page: Page) {
  const { email, password } = adminCredentials()
  await page.goto('/')
  await page.getByLabel('E-mail').fill(email)
  await page.getByLabel('Senha').fill(password)
  await page.getByRole('button', { name: 'Entrar no SGD' }).click()
  await expect(page.getByRole('tab', { name: 'Visão executiva' })).toBeVisible()
  await expect(page).toHaveURL(/\/app\/visao-executiva/)
}
