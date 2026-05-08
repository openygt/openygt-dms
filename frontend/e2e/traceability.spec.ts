import { test, expect } from '@playwright/test'
import { login, navigate, waitTable } from './helpers'

test.describe('Flow 4: 追溯查询', () => {

  test('处方追溯列表渲染', async ({ page }) => {
    await login(page)
    await navigate(page, '/traces')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })

    // Verify trace status tags
    const tags = page.locator('.el-table .el-tag')
    await expect(tags.first()).toBeVisible({ timeout: 5000 })
  })

  test('批次追溯搜索', async ({ page }) => {
    await login(page)
    await navigate(page, '/trace/batch')

    await page.waitForTimeout(2000)
    // Check for search input or table
    const input = page.locator('input[placeholder*="批次"]')
    const table = page.locator('.el-table')
    const anyVisible = (await input.isVisible().catch(() => false)) || (await table.isVisible().catch(() => false))
    expect(anyVisible).toBeTruthy()
  })

  test('异常追溯列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/trace/exception')

    await page.waitForTimeout(2000)
    const table = page.locator('.el-table')
    const empty = page.locator('.el-empty')
    const anyVisible = (await table.isVisible().catch(() => false)) || (await empty.isVisible().catch(() => false))
    expect(anyVisible).toBeTruthy()
  })

  test('异常追溯统计卡片', async ({ page }) => {
    await login(page)
    await navigate(page, '/trace/exception')

    await page.waitForTimeout(2000)
    // Check for stat/dashboard cards
    const cards = page.locator('.el-card')
    const count = await cards.count()
    expect(count).toBeGreaterThanOrEqual(1)
  })
})
