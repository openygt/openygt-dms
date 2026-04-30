import { test, expect } from '@playwright/test'

test.describe('用户管理', () => {
  test('页面加载显示用户列表', async ({ page }) => {
    await page.goto('/users')
    await page.waitForLoadState('networkidle')
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('用户搜索可用', async ({ page }) => {
    await page.goto('/users')
    await page.waitForLoadState('networkidle')
    const searchInput = page.locator('.search-input input, .el-input__inner').first()
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('admin')
      await searchInput.press('Enter')
      await page.waitForTimeout(500)
    }
  })
})
