import { Page, Locator, expect } from '@playwright/test'

export class MedicineCatalogPage {
  readonly page: Page
  readonly dataTable: Locator
  readonly createBtn: Locator
  readonly searchBtn: Locator

  constructor(page: Page) {
    this.page = page
    this.dataTable = page.locator('[data-testid="data-table"]')
    this.createBtn = page.locator('[data-testid="create-btn"]')
    this.searchBtn = page.locator('[data-testid="search-btn"]')
  }

  async goto() {
    await this.page.goto('/base/medicine')
    await this.page.waitForLoadState('networkidle')
    await this.page.waitForTimeout(800)
  }

  async expectTableVisible() {
    await expect(this.dataTable).toBeVisible()
  }
}
