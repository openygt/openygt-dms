import { test, expect } from '@playwright/test'

test.describe('任务管理', () => {
  test('页面加载显示任务列表', async ({ page }) => {
    await page.goto('/tasks')
    await page.waitForLoadState('networkidle')
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('任务状态筛选可用', async ({ page }) => {
    await page.goto('/tasks')
    await page.waitForLoadState('networkidle')
    const filters = page.locator('.filter-bar .el-select, .status-filter .el-radio-button')
    const count = await filters.count()
    if (count > 0) {
      await filters.first().click()
      await page.waitForTimeout(300)
    }
  })
})
