import { Page, Locator, expect } from '@playwright/test'

export class WorkloadPage {
  readonly page: Page
  readonly statTable: Locator
  readonly summaryCards: Locator
  readonly pagination: Locator
  readonly exportBtn: Locator

  constructor(page: Page) {
    this.page = page
    this.statTable = page.locator('.el-table')
    this.summaryCards = page.locator('.summary-card')
    this.pagination = page.locator('.el-pagination')
    this.exportBtn = page.locator('.el-button').filter({ hasText: '导出Excel' })
  }

  async goto() {
    await this.page.goto('/workload')
    await this.page.waitForLoadState('networkidle')
    await this.page.waitForTimeout(800)
  }

  async filterByWorkType(type: string) {
    await this.page.locator('.el-select').filter({ hasText: '全部' }).first().click()
    await this.page.locator('.el-select-dropdown__item').filter({ hasText: type }).click()
    await this.page.waitForTimeout(300)
  }

  async expectTableVisible() {
    await expect(this.statTable).toBeVisible()
  }

  async expectSummaryVisible() {
    const count = await this.summaryCards.count()
    expect(count).toBeGreaterThan(0)
  }
}
