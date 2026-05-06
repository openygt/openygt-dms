import { Page, Locator, expect } from '@playwright/test'

export class TracePage {
  readonly page: Page
  readonly searchInput: Locator
  readonly statusFilter: Locator
  readonly traceTable: Locator
  readonly pagination: Locator
  readonly dateRangePicker: Locator

  constructor(page: Page) {
    this.page = page
    this.searchInput = page.locator('.el-input__inner').first()
    this.statusFilter = page.locator('.el-select').first()
    this.traceTable = page.locator('.el-table')
    this.pagination = page.locator('.el-pagination')
    this.dateRangePicker = page.locator('.el-date-editor--daterange')
  }

  async goto() {
    await this.page.goto('/traces')
    await this.page.waitForLoadState('networkidle')
    await this.page.waitForTimeout(800)
  }

  async searchPrescriptionNo(no: string) {
    await this.searchInput.fill(no)
    await this.searchInput.press('Enter')
    await this.page.waitForTimeout(500)
  }

  async filterByStatus(status: '进行中' | '已完成' | '异常') {
    // 使用placeholder定位状态选择器
    const statusSelect = this.page.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select')
    await statusSelect.click()
    await this.page.waitForTimeout(300)
    // Element Plus下拉菜单可能不在视口内，使用page.getByRole
    await this.page.getByRole('option', { name: status }).click()
    await this.page.waitForTimeout(300)
  }

  async clickDetail(rowIndex: number = 0) {
    const detailBtn = this.traceTable.locator('tbody tr').nth(rowIndex).locator('.el-button').filter({ hasText: '详情' })
    await detailBtn.click()
    await this.page.waitForURL(/\/traces\//, { timeout: 5000 })
  }

  async clickTemperatureCurve(rowIndex: number = 0) {
    const btn = this.traceTable.locator('tbody tr').nth(rowIndex).locator('.el-button').filter({ hasText: '温度曲线' })
    await btn.click()
    await this.page.waitForURL(/\/traces\//, { timeout: 5000 })
  }

  async expectTableVisible() {
    await expect(this.traceTable).toBeVisible()
  }
}
