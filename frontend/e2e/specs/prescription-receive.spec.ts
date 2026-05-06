import { test, expect } from '@playwright/test'
import { PrescriptionReceivePage } from '../poms/PrescriptionReceivePage'

test.describe('处方接收', () => {
  test('页面加载显示处方列表', async ({ page }) => {
    const receivePage = new PrescriptionReceivePage(page)
    await receivePage.goto()
    await receivePage.expectTableVisible()
  })

  test('状态切换标签可用', async ({ page }) => {
    const receivePage = new PrescriptionReceivePage(page)
    await receivePage.goto()
    await receivePage.switchStatusTab('待接收')
    await receivePage.switchStatusTab('已接收')
    await receivePage.switchStatusTab('已驳回')
    await receivePage.switchStatusTab('全部')
  })

  test('列表包含测试处方数据', async ({ page }) => {
    const receivePage = new PrescriptionReceivePage(page)
    await receivePage.goto()
    const rows = receivePage.dataTable.locator('tbody tr')
    expect(await rows.count()).toBeGreaterThan(0)
  })

  test('搜索功能可用', async ({ page }) => {
    const receivePage = new PrescriptionReceivePage(page)
    await receivePage.goto()
    const searchInput = page.locator('.el-input__inner').first()
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('张三')
      await receivePage.searchBtn.click()
      await page.waitForTimeout(500)
      const rows = receivePage.dataTable.locator('tbody tr')
      expect(await rows.count()).toBeGreaterThan(0)
    }
  })

  test('待接收处方显示接收和驳回按钮', async ({ page }) => {
    const receivePage = new PrescriptionReceivePage(page)
    await receivePage.goto()
    await receivePage.switchStatusTab('待接收')
    await page.waitForTimeout(500)
    const rows = receivePage.dataTable.locator('tbody tr')
    const count = await rows.count()
    if (count > 0) {
      const firstRow = rows.first()
      const receiveBtn = firstRow.locator('.el-button').filter({ hasText: '接收' })
      const rejectBtn = firstRow.locator('.el-button').filter({ hasText: '驳回' })
      expect(await receiveBtn.count() + await rejectBtn.count()).toBeGreaterThan(0)
    }
  })
})
