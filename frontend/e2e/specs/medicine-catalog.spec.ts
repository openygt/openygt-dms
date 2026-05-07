import { test, expect } from '@playwright/test'
import { MedicineCatalogPage } from '../poms/MedicineCatalogPage'

test.describe('药材目录', () => {
  test('页面加载显示药材列表', async ({ page }) => {
    const medicinePage = new MedicineCatalogPage(page)
    await medicinePage.goto()
    await medicinePage.expectTableVisible()
  })

  test('药材列表包含测试数据', async ({ page }) => {
    const medicinePage = new MedicineCatalogPage(page)
    await medicinePage.goto()
    const rows = medicinePage.dataTable.locator('tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThan(0)
  })

  test('搜索功能可用', async ({ page }) => {
    const medicinePage = new MedicineCatalogPage(page)
    await medicinePage.goto()
    const searchInput = page.locator('.el-input__inner').first()
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('黄芪')
      await medicinePage.searchBtn.click()
      await page.waitForTimeout(500)
      const rows = medicinePage.dataTable.locator('tbody tr')
      expect(await rows.count()).toBeGreaterThan(0)
    }
  })

  test('新增按钮可用', async ({ page }) => {
    const medicinePage = new MedicineCatalogPage(page)
    await medicinePage.goto()
    await expect(medicinePage.createBtn).toBeVisible()
  })
})
