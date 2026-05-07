import { Page, Locator, expect } from '@playwright/test'

export class PrescriptionReceivePage {
  readonly page: Page
  readonly dataTable: Locator
  readonly searchBtn: Locator

  constructor(page: Page) {
    this.page = page
    this.dataTable = page.locator('[data-testid="data-table"]')
    this.searchBtn = page.locator('[data-testid="search-btn"]')
  }

  async goto() {
    await this.page.goto('/prod/receive')
    await this.page.waitForLoadState('networkidle')
    await this.page.waitForTimeout(800)
  }

  async expectTableVisible() {
    await expect(this.dataTable).toBeVisible()
  }

  async switchStatusTab(status: '全部' | '待接收' | '已接收' | '已驳回') {
    const tab = this.page.locator('.el-radio-button__inner').filter({ hasText: status })
    await tab.click()
    await this.page.waitForTimeout(500)
  }
}
