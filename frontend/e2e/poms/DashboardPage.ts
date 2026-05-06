import { Page, Locator, expect } from '@playwright/test'

export class DashboardPage {
  readonly page: Page
  readonly metricCards: Locator
  readonly stageChart: Locator
  readonly hourlyChart: Locator
  readonly workerTable: Locator
  readonly dateRangeTabs: Locator

  constructor(page: Page) {
    this.page = page
    this.metricCards = page.locator('.metric-card')
    this.stageChart = page.locator('.chart-container').first()
    this.hourlyChart = page.locator('.chart-container').nth(1)
    this.workerTable = page.locator('.el-table').first()
    this.dateRangeTabs = page.locator('.el-radio-group')
  }

  async goto() {
    await this.page.goto('/eq-dashboard')
    await this.page.waitForLoadState('networkidle')
    await this.page.waitForTimeout(1500)
  }

  async switchRange(range: '今日' | '本周' | '本月') {
    await this.page.locator('.el-radio-button__inner').filter({ hasText: range }).click()
    await this.page.waitForTimeout(500)
  }

  async expectMetricsVisible() {
    const count = await this.metricCards.count()
    expect(count).toBeGreaterThan(0)
  }

  async expectChartsVisible() {
    await expect(this.stageChart).toBeVisible({ timeout: 5000 })
  }
}
