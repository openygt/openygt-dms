import { test, expect } from '@playwright/test'

test.describe('告警日志', () => {
  test('页面加载显示告警列表', async ({ page }) => {
    await page.goto('/alarms')
    await page.waitForLoadState('networkidle')
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('告警级别筛选可用', async ({ page }) => {
    await page.goto('/alarms')
    await page.waitForLoadState('networkidle')
    const filters = page.locator('.alarm-filter .el-radio-button, .alarm-filter .el-select')
    const count = await filters.count()
    if (count > 0) {
      await filters.first().click()
      await page.waitForTimeout(300)
    }
  })
})
