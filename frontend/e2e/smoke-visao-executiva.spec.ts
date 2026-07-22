import { expect, test } from '@playwright/test'
import { loginAsAdmin } from './helpers'

test.describe('smoke — visão executiva', () => {
  test('login admin abre a Visão executiva com grade BI', async ({ page }) => {
    await loginAsAdmin(page)

    await expect(page.getByRole('heading', { name: 'Visão executiva' })).toBeVisible()
    await expect(page.getByText('Dashboards & BI / Visão executiva')).toBeVisible()
    await expect(page.getByTestId('grade-executiva')).toBeVisible()
    await expect(page.getByText('Presença geral')).toBeVisible()
    await expect(page.getByText('Volume mensal')).toBeVisible()
    await expect(page.getByText('Encontros por situação')).toBeVisible()
    await expect(page.getByText('Composição de presença')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Imprimir / PDF' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Abrir painel detalhado' })).toBeVisible()
  })

  test('deep-link e drill-down para o painel detalhado', async ({ page }) => {
    await loginAsAdmin(page)

    await page.goto('/app/adolescentes')
    await expect(page.getByRole('tab', { name: 'Adolescentes' })).toHaveAttribute('aria-selected', 'true')

    await page.goto('/app/visao-executiva')
    await expect(page.getByRole('heading', { name: 'Visão executiva' })).toBeVisible()
    await page.getByRole('button', { name: 'Abrir painel detalhado' }).click()
    await expect(page).toHaveURL(/\/app\/painel/)
    await expect(page.getByRole('heading', { name: 'Painel administrativo' })).toBeVisible()
  })
})
