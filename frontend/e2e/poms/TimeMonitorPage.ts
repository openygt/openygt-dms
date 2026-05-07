import { Page, Locator, expect } from '@playwright/test'

export class TimeMonitorPage {
  readonly page: Page
  readonly statNormal: Locator
  readonly statWarning: Locator
  readonly statTimeout: Locator
  readonly statResolved: Locator
  readonly dataTable: Locator
  readonly ruleBtn: Locator
  readonly refreshBtn: Locator

  constructor(page: Page) {
    this.page = page
    this.statNormal = page.locator('.stat-normal .stat-value')
    this.statWarning = page.locator('.stat-warning .stat-value')
    this.statTimeout = page.locator('.stat-timeout .stat-value')
    this.statResolved = page.locator('.stat-resolved .stat-value')
    this.dataTable = page.locator('.el-table')
    this.ruleBtn = page.locator('button:has-text("配置规则")')
    this.refreshBtn = page.locator('button:has-text("刷新")')
  }

  async goto() {
    await this.page.goto('/monitor/time')
    await this.page.waitForLoadState('networkidle')
    await this.page.waitForTimeout(800)
  }

  async expectStatsVisible() {
    await expect(this.statNormal).toBeVisible()
    await expect(this.statWarning).toBeVisible()
    await expect(this.statTimeout).toBeVisible()
    await expect(this.statResolved).toBeVisible()
  }

  async expectTableVisible() {
    await expect(this.dataTable).toBeVisible()
  }
}
