import { Page, Locator, expect } from '@playwright/test'

export class DeviceMonitorPage {
  readonly page: Page
  readonly deviceCards: Locator
  readonly refreshBtn: Locator
  readonly fullscreenBtn: Locator
  readonly alarmIndicator: Locator
  readonly typeFilter: Locator

  constructor(page: Page) {
    this.page = page
    this.deviceCards = page.locator('[data-testid^="device-card-"]')
    this.refreshBtn = page.locator('[data-testid="refresh-btn"]')
    this.fullscreenBtn = page.locator('[data-testid="fullscreen-btn"]')
    this.alarmIndicator = page.locator('[data-testid="alarm-indicator"]')
    this.typeFilter = page.locator('.el-radio-group')
  }

  async goto() {
    await this.page.goto('/device-monitor')
    await this.page.waitForLoadState('networkidle')
    // 给页面一点时间渲染
    await this.page.waitForTimeout(1000)
  }

  async filterByType(type: 'decoct' | 'pack' | 'all') {
    const label = { decoct: '煎药机', pack: '包装机', all: '全部' }[type]
    // 使用更精确的选择器：在 .el-radio-group 内部找包含文本的 label
    await this.page.locator('.el-radio-group .el-radio-button__inner').filter({ hasText: label }).click()
    await this.page.waitForTimeout(300)
  }

  async getDeviceCard(code: string) {
    return this.page.locator(`[data-testid="device-card-${code}"]`)
  }

  async clickDeviceCard(code: string) {
    await (await this.getDeviceCard(code)).click()
  }

  async clickEmergencyStop(code: string) {
    const card = await this.getDeviceCard(code)
    await card.locator('[data-testid="emergency-stop-btn"]').click()
  }

  async clickShiftHandover(code: string) {
    const card = await this.getDeviceCard(code)
    await card.locator('[data-testid="shift-handover-btn"]').click()
  }

  async expectDeviceVisible(code: string) {
    await expect(this.page.locator(`[data-testid="device-card-${code}"]`)).toBeVisible()
  }

  async expectDeviceStatus(code: string, statusText: string) {
    const card = await this.getDeviceCard(code)
    await expect(card.locator('[data-testid="status-text"]')).toContainText(statusText)
  }
}
