import { Page, expect, Locator } from '@playwright/test'

/**
 * Login as admin and wait for dashboard to load
 */
export async function login(page: Page) {
  await page.goto('/login')
  await page.waitForSelector('.login-card', { timeout: 10000 })

  // Fill login form
  const usernameInput = page.locator('.login-card input').first()
  const passwordInput = page.locator('.login-card input').nth(1)
  await usernameInput.fill('admin')
  await passwordInput.fill('admin123')

  // Click login button
  await page.locator('.login-card .el-button--primary').click()

  // Wait for navigation to dashboard
  await page.waitForURL(/(\/dashboard|\/)/, { timeout: 10000 })
  await page.waitForTimeout(1000)
}

/**
 * Navigate to a route and wait for content to render
 */
export async function navigate(page: Page, route: string) {
  await page.goto(route)
  await page.waitForTimeout(1500) // Wait for async data to load
}

/**
 * Wait for table data to load (rows appear)
 */
export async function waitTable(page: Page, selector = '.el-table') {
  const table = page.locator(selector).first()
  await table.waitFor({ state: 'visible', timeout: 10000 })
  // Wait for at least one row
  const rows = table.locator('.el-table__body-wrapper tbody tr')
  await rows.first().waitFor({ state: 'attached', timeout: 10000 }).catch(() => {})
}

/**
 * Get cell text at a specific row and column in a table
 */
export async function getCell(page: Page, row: number, col: number, selector = '.el-table'): Promise<string> {
  const cell = page.locator(`${selector} .el-table__body-wrapper tbody tr`).nth(row).locator('td').nth(col)
  return (await cell.textContent()) || ''
}

/**
 * Assert Element Plus tag has the correct type class
 */
export async function assertTag(page: Page, selector: string, type: 'success' | 'warning' | 'info' | 'danger' | 'primary' | '') {
  const tag = page.locator(selector)
  await tag.waitFor({ state: 'visible', timeout: 5000 })
  if (type) {
    await expect(tag).toHaveClass(new RegExp(`el-tag--${type}`))
  }
}

/**
 * Assert KPI card value is a number
 */
export async function assertCard(page: Page, selector: string) {
  const el = page.locator(selector)
  await el.waitFor({ state: 'visible', timeout: 5000 })
  const text = (await el.textContent()) || ''
  expect(text.trim()).not.toBe('')
}

/**
 * Assert ECharts canvas is rendered inside a container
 */
export async function assertChart(page: Page, selector: string) {
  const container = page.locator(selector)
  await container.waitFor({ state: 'visible', timeout: 5000 })
  // ECharts renders a canvas inside the container
  const canvas = container.locator('canvas')
  await expect(canvas).toBeAttached({ timeout: 5000 })
}

/**
 * Assert empty state is visible
 */
export async function assertEmpty(page: Page, selector = '.el-empty') {
  const empty = page.locator(selector)
  await empty.waitFor({ state: 'visible', timeout: 5000 })
}

/**
 * Assert error/alert state is visible
 */
export async function assertError(page: Page, selector = '.el-alert--error') {
  const alert = page.locator(selector)
  await alert.waitFor({ state: 'visible', timeout: 5000 })
}

/**
 * Click a dialog/confirm button by text
 */
export async function clickDialog(page: Page, btnText: string) {
  const btn = page.locator('.el-dialog .el-button').filter({ hasText: btnText })
  await btn.click()
  // Wait for dialog to close
  await page.waitForTimeout(500)
}

/**
 * Fill a form field (label-based lookup for Element Plus)
 */
export async function fillFormField(page: Page, label: string, value: string) {
  // Find the form item by label text
  const formItem = page.locator('.el-form-item').filter({ hasText: label })
  const input = formItem.locator('input, textarea')
  await input.fill(value)
}

/**
 * Fill an entire form with a data map { label: value }
 */
export async function fillForm(page: Page, data: Record<string, string>) {
  for (const [label, value] of Object.entries(data)) {
    await fillFormField(page, label, value)
  }
}

/**
 * Submit a form by pressing the submit button
 */
export async function submitForm(page: Page, btnText = '保存') {
  const btn = page.locator('.el-dialog .el-button--primary').filter({ hasText: btnText })
  await btn.click()
  await page.waitForTimeout(800)
}

/**
 * Wait for loading state to disappear
 */
export async function waitLoadingDone(page: Page) {
  const loading = page.locator('.el-loading-mask')
  if (await loading.isVisible().catch(() => false)) {
    await loading.waitFor({ state: 'hidden', timeout: 15000 })
  }
}

/**
 * Select an option from an El-Select by label text
 */
export async function selectOption(page: Page, label: string, optionText: string) {
  const formItem = page.locator('.el-form-item').filter({ hasText: label })
  const select = formItem.locator('.el-select')
  await select.click()
  await page.waitForTimeout(500)
  const option = page.locator('.el-select-dropdown__item').filter({ hasText: optionText })
  await option.click()
  await page.waitForTimeout(300)
}
