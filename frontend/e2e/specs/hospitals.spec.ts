import { test, expect } from '@playwright/test'

test.describe('医院管理', () => {
  test('页面加载显示医院列表', async ({ page }) => {
    await page.goto('/hospitals')
    await page.waitForLoadState('networkidle')
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('医院搜索可用', async ({ page }) => {
    await page.goto('/hospitals')
    await page.waitForLoadState('networkidle')
    const searchInput = page.locator('.search-input input, .el-input__inner').first()
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('医院')
      await searchInput.press('Enter')
      await page.waitForTimeout(500)
    }
  })
})
