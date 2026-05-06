import { Page, Locator, expect } from '@playwright/test'

export class ExceptionTracePage {
  readonly page: Page
  readonly dataTable: Locator
  readonly searchBtn: Locator

  constructor(page: Page) {
    this.page = page
    this.dataTable = page.locator('[data-testid="data-table"]')
    this.searchBtn = page.locator('[data-testid="search-btn"]')
  }

  async goto() {
    await this.page.goto('/trace/exception')
    await this.page.waitForLoadState('networkidle')
    await this.page.waitForTimeout(800)
  }

  async expectTableVisible() {
    await expect(this.dataTable).toBeVisible()
  }
}
