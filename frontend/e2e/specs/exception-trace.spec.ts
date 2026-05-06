import { test, expect } from '@playwright/test'
import { ExceptionTracePage } from '../poms/ExceptionTracePage'

test.describe('异常追溯', () => {
  test('页面加载显示异常列表', async ({ page }) => {
    const excPage = new ExceptionTracePage(page)
    await excPage.goto()
    await excPage.expectTableVisible()
  })

  test('异常列表包含测试数据', async ({ page }) => {
    const excPage = new ExceptionTracePage(page)
    await excPage.goto()
    const rows = excPage.dataTable.locator('tbody tr')
    expect(await rows.count()).toBeGreaterThan(0)
  })

  test('搜索功能可用', async ({ page }) => {
    const excPage = new ExceptionTracePage(page)
    await excPage.goto()
    const searchInput = page.locator('.el-input__inner').first()
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('张三')
      await excPage.searchBtn.click()
      await page.waitForTimeout(500)
      const rows = excPage.dataTable.locator('tbody tr')
      expect(await rows.count()).toBeGreaterThan(0)
    }
  })
})
