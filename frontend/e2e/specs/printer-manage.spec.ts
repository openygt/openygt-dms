import { test, expect } from '@playwright/test'
import { PrinterManagePage } from '../poms/PrinterManagePage'

test.describe('打印机管理', () => {
  test('页面加载显示打印机列表', async ({ page }) => {
    const printerPage = new PrinterManagePage(page)
    await printerPage.goto()
    await printerPage.expectTableVisible()
  })

  test('打印机列表包含测试数据', async ({ page }) => {
    const printerPage = new PrinterManagePage(page)
    await printerPage.goto()
    const rows = printerPage.dataTable.locator('tbody tr')
    expect(await rows.count()).toBeGreaterThan(0)
  })

  test('搜索功能可用', async ({ page }) => {
    const printerPage = new PrinterManagePage(page)
    await printerPage.goto()
    const searchInput = page.locator('.el-input__inner').first()
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('标签')
      await printerPage.searchBtn.click()
      await page.waitForTimeout(500)
      const rows = printerPage.dataTable.locator('tbody tr')
      expect(await rows.count()).toBeGreaterThan(0)
    }
  })
})
