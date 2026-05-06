import { test, expect } from '@playwright/test'

test.describe('质检记录', () => {
  test('页面加载显示质检列表', async ({ page }) => {
    await page.goto('/quality')
    await page.waitForLoadState('networkidle')
    const table = page.locator('.el-table').first()
    await expect(table).toBeVisible()
  })

  test('质检结果筛选可用', async ({ page }) => {
    await page.goto('/quality')
    await page.waitForLoadState('networkidle')
    const filters = page.locator('.filter-bar .el-select, .el-radio-group')
    const count = await filters.count()
    if (count > 0) {
      await filters.first().click()
      await page.waitForTimeout(300)
    }
  })
})
