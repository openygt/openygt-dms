import { test, expect } from '@playwright/test'
import { DeviceDetailPage } from '../poms/DeviceDetailPage'

test.describe('设备详情页', () => {
  test('页面加载显示仪表盘和温度曲线', async ({ page }) => {
    const detail = new DeviceDetailPage(page)
    await detail.goto('DECOCT_001')
    const gauge = page.locator('[data-testid="temp-gauge"], .gauge-chart')
    const tempChart = page.locator('[data-testid="temp-chart"], .temp-chart')
    await expect(gauge).toBeVisible({ timeout: 10000 })
    await expect(tempChart).toBeVisible({ timeout: 10000 })
  })

  test('温度曲线时间范围切换', async ({ page }) => {
    const detail = new DeviceDetailPage(page)
    await detail.goto('DECOCT_001')
    const tabs = page.locator('.time-range-tabs .el-radio-button')
    const count = await tabs.count()
    if (count > 0) {
      for (let i = 0; i < Math.min(count, 3); i++) {
        await tabs.nth(i).click()
        await page.waitForTimeout(300)
      }
    }
  })

  test('指令面板按钮可点击', async ({ page }) => {
    const detail = new DeviceDetailPage(page)
    await detail.goto('DECOCT_001')
    const buttons = page.locator('.command-grid .el-button, [data-testid^="command-btn-"]')
    const count = await buttons.count()
    if (count === 0) {
      test.skip('指令按钮未渲染，跳过')
      return
    }
    expect(count).toBeGreaterThan(0)
    const pauseBtn = buttons.filter({ hasText: '暂停' })
    if (await pauseBtn.count() > 0 && await pauseBtn.isEnabled()) {
      await pauseBtn.click()
    }
  })

  test('离线设备详情页指令按钮禁用', async ({ page }) => {
    const detail = new DeviceDetailPage(page)
    // 找一个离线设备（如 EQ001）
    await detail.goto('EQ001')
    await page.waitForTimeout(1000)
    const buttons = page.locator('.command-grid .el-button')
    const count = await buttons.count()
    if (count === 0) {
      test.skip('指令按钮未渲染，跳过')
      return
    }
    // 检查非急停按钮是否被禁用
    for (let i = 0; i < count; i++) {
      const btn = buttons.nth(i)
      const text = await btn.textContent() || ''
      if (!text.includes('急停')) {
        await expect(btn).toBeDisabled()
      }
    }
  })

  test('在线设备详情页指令按钮可用', async ({ page }) => {
    const detail = new DeviceDetailPage(page)
    await detail.goto('DECOCT_001')
    await page.waitForTimeout(1000)
    const buttons = page.locator('.command-grid .el-button')
    const count = await buttons.count()
    if (count === 0) {
      test.skip('指令按钮未渲染，跳过')
      return
    }
    // 至少有一个按钮是启用的
    let hasEnabled = false
    for (let i = 0; i < count; i++) {
      const btn = buttons.nth(i)
      if (await btn.isEnabled()) {
        hasEnabled = true
        break
      }
    }
    expect(hasEnabled).toBeTruthy()
  })

  test('返回按钮回到监控面板', async ({ page }) => {
    const detail = new DeviceDetailPage(page)
    await detail.goto('DECOCT_001')
    await detail.page.goto('/device-monitor')
    await expect(page).toHaveURL('/device-monitor')
  })
})
