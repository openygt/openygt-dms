import { test, expect } from '@playwright/test'
import { DeviceMonitorPage } from '../poms/DeviceMonitorPage'

test.describe('设备监控快捷链接', () => {
  test('监控面板显示快捷链接按钮组', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    const quickLinks = page.locator('.quick-links')
    await expect(quickLinks).toBeVisible()
  })

  test('点击煎药追溯跳转', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    const btn = page.locator('.quick-links .el-button').filter({ hasText: '煎药追溯' })
    await btn.click()
    await page.waitForURL('/traces', { timeout: 5000 })
    await expect(page.locator('.el-table')).toBeVisible()
  })

  test('点击数据看板跳转', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    const btn = page.locator('.quick-links .el-button').filter({ hasText: '数据看板' })
    await btn.click()
    await page.waitForURL('/eq-dashboard', { timeout: 5000 })
    await expect(page.locator('.metric-card').first()).toBeVisible()
  })

  test('点击工作量跳转', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    const btn = page.locator('.quick-links .el-button').filter({ hasText: '工作量' })
    await btn.click()
    await page.waitForURL('/workload', { timeout: 5000 })
    await expect(page.locator('.summary-card').first()).toBeVisible()
  })

  test('点击利用率跳转', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    const btn = page.locator('.quick-links .el-button').filter({ hasText: '利用率' })
    await btn.click()
    await page.waitForURL('/device-utilization', { timeout: 5000 })
    await expect(page.locator('.chart-container').first()).toBeVisible()
  })
})
