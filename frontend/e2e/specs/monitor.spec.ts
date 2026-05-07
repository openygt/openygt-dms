import { test, expect } from '@playwright/test'
import { DeviceMonitorPage } from '../poms/DeviceMonitorPage'

test.describe('设备监控面板', () => {
  test('页面加载显示设备卡片', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    const hasCards = await monitor.deviceCards.count() > 0
    const emptyState = page.locator('.ygt-empty')
    expect(hasCards || (await emptyState.isVisible().catch(() => false))).toBeTruthy()
  })

  test('设备类型过滤按钮完整（含PDA）', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    await monitor.expectFilterVisible('煎药机')
    await monitor.expectFilterVisible('包装机')
    await monitor.expectFilterVisible('标签打印机')
    await monitor.expectFilterVisible('激光打印机')
    await monitor.expectFilterVisible('PDA')
    await monitor.expectFilterVisible('全部')
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
    await monitor.filterByType('pda')
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

  test('离线设备不显示操作按钮（仅详情）', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    const cards = monitor.deviceCards
    const count = await cards.count()
    if (count === 0) {
      test.skip('无设备数据，跳过')
      return
    }
    // 遍历所有卡片，找到离线的
    let foundOffline = false
    for (let i = 0; i < count; i++) {
      const card = cards.nth(i)
      const statusText = await card.locator('[data-testid="status-text"]').textContent() || ''
      if (statusText.includes('离线')) {
        foundOffline = true
        // 离线设备不应有急停/换班/开始等操作按钮
        await expect(card.locator('[data-testid="emergency-stop-btn"]')).toHaveCount(0)
        await expect(card.locator('[data-testid="shift-handover-btn"]')).toHaveCount(0)
        // 但应该有查看详情按钮
        const detailBtn = card.locator('.el-button').filter({ hasText: '查看详情' })
        await expect(detailBtn).toBeVisible()
        break
      }
    }
    if (!foundOffline) {
      test.skip('当前无离线设备，跳过')
    }
  })

  test('在线设备根据状态显示对应操作按钮', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    const cards = monitor.deviceCards
    const count = await cards.count()
    if (count === 0) {
      test.skip('无设备数据，跳过')
      return
    }
    // 找到第一个在线的煎药机
    let found = false
    for (let i = 0; i < count; i++) {
      const card = cards.nth(i)
      const statusText = await card.locator('[data-testid="status-text"]').textContent() || ''
      if (!statusText.includes('离线')) {
        found = true
        // 在线设备至少应有查看详情按钮
        const detailBtn = card.locator('.el-button').filter({ hasText: '查看详情' })
        await expect(detailBtn).toBeVisible()
        // 不应同时存在"暂停"和"继续"按钮
        const pauseBtns = card.locator('.el-button').filter({ hasText: '暂停' })
        const resumeBtns = card.locator('.el-button').filter({ hasText: '继续' })
        const hasPause = await pauseBtns.count() > 0
        const hasResume = await resumeBtns.count() > 0
        expect(hasPause && hasResume).toBeFalsy()
        break
      }
    }
    if (!found) {
      test.skip('当前无在线设备，跳过')
    }
  })

  test('急停按钮弹出确认对话框', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    const count = await monitor.deviceCards.count()
    if (count === 0) {
      test.skip('无设备数据，跳过')
      return
    }
    // 找一个在线且非故障的设备
    let found = false
    for (let i = 0; i < count; i++) {
      const card = monitor.deviceCards.nth(i)
      const statusText = await card.locator('[data-testid="status-text"]').textContent() || ''
      if (!statusText.includes('离线') && !statusText.includes('故障')) {
        const code = await card.getAttribute('data-device-code') || 'unknown'
        const stopBtn = card.locator('[data-testid="emergency-stop-btn"]')
        if (await stopBtn.count() > 0) {
          found = true
          await stopBtn.click()
          const dialog = page.locator('[data-testid="confirm-dialog"]')
          await expect(dialog).toBeVisible({ timeout: 5000 })
          await page.locator('[data-testid="cancel-emergency-stop"]').click()
          await expect(dialog).toBeHidden({ timeout: 5000 })
          break
        }
      }
    }
    if (!found) {
      test.skip('无可用急停按钮的在线设备，跳过')
    }
  })

  test('换班按钮弹出交接对话框', async ({ page }) => {
    const monitor = new DeviceMonitorPage(page)
    await monitor.goto()
    const count = await monitor.deviceCards.count()
    if (count === 0) {
      test.skip('无设备数据，跳过')
      return
    }
    // 找一个有换班按钮的在线设备
    let found = false
    for (let i = 0; i < count; i++) {
      const card = monitor.deviceCards.nth(i)
      const shiftBtn = card.locator('[data-testid="shift-handover-btn"]')
      if (await shiftBtn.count() > 0 && await shiftBtn.isVisible()) {
        found = true
        await shiftBtn.click()
        const dialog = page.locator('[data-testid="shift-handover-dialog"]')
        await expect(dialog).toBeVisible({ timeout: 5000 })
        await page.locator('.el-dialog__footer .el-button').first().click()
        await expect(dialog).toBeHidden({ timeout: 5000 })
        break
      }
    }
    if (!found) {
      test.skip('无可用换班按钮的设备，跳过')
    }
  })
})
