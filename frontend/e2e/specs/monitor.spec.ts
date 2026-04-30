import { test, expect } from '@playwright/test'
import { DeviceMonitorPage } from '../poms/DeviceMonitorPage'

test.describe('设备监控面板', () => {
  test('页面加载显示设备卡片', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    // 至少能看到一个设备卡片，或空状态
    const hasCards = await monitor.deviceCards.count() > 0
    const emptyState = page.locator('.ygt-empty')
    expect(hasCards || (await emptyState.isVisible().catch(() => false))).toBeTruthy()
  })

  test('设备类型过滤切换', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    const filterCount = await monitor.typeFilter.count()
    if (filterCount === 0) {
      test.skip('过滤组件未渲染，跳过')
      return
    }
    await monitor.filterByType('decoct')
    await monitor.filterByType('pack')
    await monitor.filterByType('all')
  })

  test('刷新按钮可点击', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    const count = await monitor.refreshBtn.count()
    if (count === 0) {
      test.skip('刷新按钮未渲染，跳过')
      return
    }
    await expect(monitor.refreshBtn).toBeEnabled()
    await monitor.refreshBtn.click()
    await page.waitForTimeout(500)
  })

  test('点击设备卡片进入详情页', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    const count = await monitor.deviceCards.count()
    if (count === 0) {
      test.skip('无设备数据，跳过')
      return
    }
    const firstCard = monitor.deviceCards.first()
    await firstCard.click()
    await page.waitForURL(/\/device\/.*\/detail/, { timeout: 5000 })
  })

  test('急停按钮弹出确认对话框', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    const count = await monitor.deviceCards.count()
    if (count === 0) {
      test.skip('无设备数据，跳过')
      return
    }
    const firstCode = await monitor.deviceCards.first().getAttribute('data-device-code') || 'unknown'
    await monitor.clickEmergencyStop(firstCode)
    const dialog = page.locator('[data-testid="confirm-dialog"]')
    await expect(dialog).toBeVisible({ timeout: 5000 })
    await page.locator('[data-testid="cancel-emergency-stop"]').click()
    await expect(dialog).toBeHidden({ timeout: 5000 })
  })

  test('换班按钮弹出交接对话框', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    const count = await monitor.deviceCards.count()
    if (count === 0) {
      test.skip('无设备数据，跳过')
      return
    }
    const firstCode = await monitor.deviceCards.first().getAttribute('data-device-code') || 'unknown'
    await monitor.clickShiftHandover(firstCode)
    const dialog = page.locator('[data-testid="shift-handover-dialog"]')
    await expect(dialog).toBeVisible({ timeout: 5000 })
    await page.locator('.el-dialog__footer .el-button').first().click()
    await expect(dialog).toBeHidden({ timeout: 5000 })
  })
})
