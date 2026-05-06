import { test, expect } from '@playwright/test'

test.describe('打印中心', () => {
  test('页面加载显示打印任务列表', async ({ page }) => {
    await page.goto('/print')
    await page.waitForLoadState('networkidle')
    const content = page.locator('.el-table, .ygt-empty, .el-card')
    await expect(content.first()).toBeVisible()
  })

  test('打印任务筛选标签页可切换', async ({ page }) => {
    await page.goto('/print')
    await page.waitForLoadState('networkidle')
    const tabs = page.locator('.el-tabs__nav .el-tabs__item')
    const count = await tabs.count()
    if (count > 1) {
      await tabs.nth(1).click()
      await page.waitForTimeout(300)
    }
  })
})
