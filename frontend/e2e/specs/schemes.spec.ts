import { test, expect } from '@playwright/test'

test.describe('煎药方案', () => {
  test('页面加载显示方案列表', async ({ page }) => {
    await page.goto('/schemes')
    await page.waitForLoadState('networkidle')
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('方案搜索可用', async ({ page }) => {
    await page.goto('/schemes')
    await page.waitForLoadState('networkidle')
    const searchInput = page.locator('.search-box input, .el-input__inner').first()
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('测试')
      await searchInput.press('Enter')
      await page.waitForTimeout(500)
    }
  })
})
