import { Page, Locator, expect } from '@playwright/test'

export class DeviceUtilizationPage {
  readonly page: Page
  readonly statTable: Locator
  readonly chartContainer: Locator

  constructor(page: Page) {
    this.page = page
    this.statTable = page.locator('.el-table')
    this.chartContainer = page.locator('.chart-container')
  }

  async goto() {
    await this.page.goto('/device-utilization')
    await this.page.waitForLoadState('networkidle')
    await this.page.waitForTimeout(1000)
  }

  async expectTableVisible() {
    await expect(this.statTable).toBeVisible()
  }

  async expectChartVisible() {
    await expect(this.chartContainer).toBeVisible()
  }
}
