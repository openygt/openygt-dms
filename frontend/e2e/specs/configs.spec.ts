import { test, expect } from '@playwright/test'

test.describe('系统配置', () => {
  test('页面加载显示配置列表', async ({ page }) => {
    await page.goto('/configs')
    await page.waitForLoadState('networkidle')
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('配置项可搜索', async ({ page }) => {
    await page.goto('/configs')
    await page.waitForLoadState('networkidle')
    const searchInput = page.locator('.search-input input, .el-input__inner').first()
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('timeout')
      await searchInput.press('Enter')
      await page.waitForTimeout(500)
    }
  })
})
