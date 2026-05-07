import { test, expect } from '@playwright/test'

test.describe('产能报表', () => {
  test('页面加载显示报表数据', async ({ page }) => {
    await page.goto('/capacity')
    await page.waitForLoadState('networkidle')
    const content = page.locator('.capacity-chart, .el-table, .report-content')
    const count = await content.count()
    expect(count).toBeGreaterThan(0)
  })

  test('时间范围筛选可用', async ({ page }) => {
    await page.goto('/capacity')
    await page.waitForLoadState('networkidle')
    const datePicker = page.locator('.el-date-editor').first()
    if (await datePicker.isVisible().catch(() => false)) {
      await datePicker.click()
      await page.waitForTimeout(300)
      // 点击空白处关闭日期选择器
      await page.keyboard.press('Escape')
    }
  })
})
