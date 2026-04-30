import { test, expect } from '@playwright/test'
import { BatchTracePage } from '../poms/BatchTracePage'

test.describe('批次追溯', () => {
  test('页面加载显示追溯列表', async ({ page }) => {
    const batchPage = new BatchTracePage(page)
    await batchPage.goto()
    await batchPage.expectTableVisible()
  })

  test('追溯列表包含测试数据', async ({ page }) => {
    const batchPage = new BatchTracePage(page)
    await batchPage.goto()
    const rows = batchPage.dataTable.locator('tbody tr')
    expect(await rows.count()).toBeGreaterThan(0)
  })

  test('搜索功能可用', async ({ page }) => {
    const batchPage = new BatchTracePage(page)
    await batchPage.goto()
    const searchInput = page.locator('.el-input__inner').first()
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('B2026')
      await batchPage.searchBtn.click()
      await page.waitForTimeout(500)
      const rows = batchPage.dataTable.locator('tbody tr')
      expect(await rows.count()).toBeGreaterThan(0)
    }
  })
})
