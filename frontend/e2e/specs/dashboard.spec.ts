import { test, expect } from '@playwright/test'

test.describe('实时看板', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
  })

  test('页面加载显示KPI卡片', async ({ page }) => {
    await page.waitForLoadState('networkidle')
    await expect(page.locator('.kpi-card')).toHaveCount(4)
  })

  test('异常预警卡片显示', async ({ page }) => {
    await page.waitForLoadState('networkidle')
    await expect(page.locator('text=异常预警')).toBeVisible()
  })

  test('快捷入口可点击', async ({ page }) => {
    await page.waitForLoadState('networkidle')
    await expect(page.locator('.quick-actions')).toBeVisible()
  })
})
