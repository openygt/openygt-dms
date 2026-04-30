import { Page, Locator, expect } from '@playwright/test'

export class AlarmConfigPage {
  readonly page: Page
  readonly configTable: Locator
  readonly addBtn: Locator

  constructor(page: Page) {
    this.page = page
    this.configTable = page.locator('.el-table')
    this.addBtn = page.locator('.el-button').filter({ hasText: '新增配置' })
  }

  async goto() {
    await this.page.goto('/alarm-configs')
    await this.page.waitForLoadState('networkidle')
    await this.page.waitForTimeout(800)
  }

  async expectTableVisible() {
    await expect(this.configTable).toBeVisible()
  }
}
