import { Page, Locator, expect } from '@playwright/test'

export class WaterFormulaPage {
  readonly page: Page
  readonly formulaTable: Locator
  readonly addBtn: Locator

  constructor(page: Page) {
    this.page = page
    this.formulaTable = page.locator('.el-table')
    this.addBtn = page.locator('.el-button').filter({ hasText: '新增公式' })
  }

  async goto() {
    await this.page.goto('/water-formulas')
    await this.page.waitForLoadState('networkidle')
    await this.page.waitForTimeout(800)
  }

  async expectTableVisible() {
    await expect(this.formulaTable).toBeVisible()
  }

  async clickTest(rowIndex: number = 0) {
    const testBtn = this.formulaTable.locator('tbody tr').nth(rowIndex).locator('.el-button').filter({ hasText: '测试' })
    await testBtn.click()
    await this.page.waitForTimeout(500)
  }
}
