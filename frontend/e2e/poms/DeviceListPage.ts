import { Page, Locator, expect } from '@playwright/test'

export class DeviceListPage {
  readonly page: Page
  readonly createBtn: Locator
  readonly searchInput: Locator
  readonly deviceTable: Locator
  readonly pagination: Locator

  constructor(page: Page) {
    this.page = page
    this.createBtn = page.locator('[data-testid="create-device-btn"]')
    this.searchInput = page.locator('.el-input__inner').first()
    this.deviceTable = page.locator('.el-table')
    this.pagination = page.locator('.el-pagination')
  }

  async goto() {
    await this.page.goto('/devices')
    await this.page.waitForLoadState('networkidle')
  }

  async search(keyword: string) {
    await this.searchInput.fill(keyword)
    await this.searchInput.press('Enter')
    await this.page.waitForTimeout(500)
  }

  async clickCreate() {
    await this.createBtn.click()
  }

  async clickEdit(rowIndex: number) {
    await this.deviceTable.locator('tbody tr').nth(rowIndex).locator('[data-testid="edit-btn"]').click()
  }

  async clickDelete(rowIndex: number) {
    await this.deviceTable.locator('tbody tr').nth(rowIndex).locator('[data-testid="delete-btn"]').click()
  }

  async expectTableHasRows(minRows: number = 0) {
    const rows = this.deviceTable.locator('tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThanOrEqual(minRows)
  }
}
