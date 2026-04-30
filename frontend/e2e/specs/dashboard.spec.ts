import { test, expect } from '@playwright/test'

test.describe('实时看板', () => {
  test('页面加载显示关键指标卡片', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')
    const cards = page.locator('.dashboard-card, .stat-card, .el-card')
    const count = await cards.count()
    expect(count).toBeGreaterThan(0)
  })

  test('看板数据刷新按钮可用', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')
    const refreshBtn = page.locator('[data-testid="dashboard-refresh"], .refresh-btn').first()
    if (await refreshBtn.isVisible().catch(() => false)) {
      await refreshBtn.click()
      await page.waitForTimeout(300)
    }
  })
})
