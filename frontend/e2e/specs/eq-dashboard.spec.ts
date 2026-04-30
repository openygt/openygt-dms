import { test, expect } from '@playwright/test'
import { DashboardPage } from '../poms/DashboardPage'

test.describe('数据看板', () => {
  test('看板页面加载显示指标卡片', async ({ page }) => {
    const dashboard = new DashboardPage(page)
    await dashboard.goto()
    await dashboard.expectMetricsVisible()
  })

  test('看板显示阶段分布图表', async ({ page }) => {
    const dashboard = new DashboardPage(page)
    await dashboard.goto()
    await dashboard.expectChartsVisible()
  })

  test('看板显示工人效率排行', async ({ page }) => {
    const dashboard = new DashboardPage(page)
    await dashboard.goto()
    const workerTable = page.locator('.el-table').filter({ hasText: '操作人' }).first()
    await expect(workerTable).toBeVisible({ timeout: 5000 })
  })

  test('时间范围切换可用', async ({ page }) => {
    const dashboard = new DashboardPage(page)
    await dashboard.goto()
    const tabs = page.locator('.el-radio-button__inner')
    const count = await tabs.count()
    if (count > 0) {
      await tabs.first().click()
      await page.waitForTimeout(300)
    }
  })

  test('24小时趋势图表可见', async ({ page }) => {
    const dashboard = new DashboardPage(page)
    await dashboard.goto()
    const chartRow = page.locator('.chart-row').nth(1)
    await expect(chartRow).toBeVisible()
  })
})
