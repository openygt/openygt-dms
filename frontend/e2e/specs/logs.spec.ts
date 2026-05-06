import { test, expect } from '@playwright/test'

test.describe('系统日志', () => {
  test('页面加载显示日志列表', async ({ page }) => {
    await page.goto('/logs')
    await page.waitForLoadState('networkidle')
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('日志级别筛选可用', async ({ page }) => {
    await page.goto('/logs')
    await page.waitForLoadState('networkidle')
    const filters = page.locator('.filter-bar .el-select, .el-radio-group')
    const count = await filters.count()
    if (count > 0) {
      await filters.first().click()
      await page.waitForTimeout(300)
    }
  })
})
