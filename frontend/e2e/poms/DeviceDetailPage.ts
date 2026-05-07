import { Page, Locator, expect } from '@playwright/test'

export class DeviceDetailPage {
  readonly page: Page
  readonly gaugeChart: Locator
  readonly tempChart: Locator
  readonly commandButtons: Locator
  readonly commandTimeline: Locator
  readonly backButton: Locator
  readonly timeRangeTabs: Locator

  constructor(page: Page) {
    this.page = page
    this.gaugeChart = page.locator('[data-testid="temp-gauge"]')
    this.tempChart = page.locator('[data-testid="temp-chart"]')
    this.commandButtons = page.locator('[data-testid^="command-btn-"]')
    this.commandTimeline = page.locator('.el-timeline')
    this.backButton = page.locator('.detail-header .el-button')
    this.timeRangeTabs = page.locator('.chart-header .el-radio-group .el-radio-button')
  }

  async goto(code: string) {
    await this.page.goto(`/device/${code}/detail`)
    await this.page.waitForLoadState('networkidle')
    // 给页面一点时间渲染
    await this.page.waitForTimeout(1500)
  }

  async expectChartsLoaded() {
    await expect(this.gaugeChart).toBeVisible()
    await expect(this.tempChart).toBeVisible()
  }

  async sendCommand(commandType: 'START' | 'PAUSE' | 'STOP' | 'EMERGENCY_STOP') {
    const btn = this.page.locator(`[data-testid="command-btn-${commandType}"]`)
    await btn.click()
    // 处理可能的二次确认弹窗
    const confirmBtn = this.page.locator('.el-message-box__btns .el-button--primary')
    if (await confirmBtn.isVisible().catch(() => false)) {
      await confirmBtn.click()
    }
  }

  async switchTimeRange(index: number) {
    await this.timeRangeTabs.nth(index).click()
    await this.page.waitForTimeout(300)
  }

  async goBack() {
    await this.backButton.first().click()
    await this.page.waitForURL('/device-monitor')
  }
}
