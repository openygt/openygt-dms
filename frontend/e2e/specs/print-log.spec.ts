import { test, expect } from '@playwright/test'
import { PrintLogPage } from '../poms/PrintLogPage'

test.describe('打印记录', () => {
  test('页面加载显示打印记录列表', async ({ page }) => {
    const printLogPage = new PrintLogPage(page)
    await printLogPage.goto()
    await printLogPage.expectTableVisible()
  })

  test('搜索功能可用', async ({ page }) => {
    const printLogPage = new PrintLogPage(page)
    await printLogPage.goto()
    await expect(printLogPage.searchBtn).toBeVisible()
  })
})
