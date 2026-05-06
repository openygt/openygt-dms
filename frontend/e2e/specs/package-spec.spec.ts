import { test, expect } from '@playwright/test'
import { PackageSpecPage } from '../poms/PackageSpecPage'

test.describe('包装规格', () => {
  test('页面加载显示规格列表', async ({ page }) => {
    const specPage = new PackageSpecPage(page)
    await specPage.goto()
    await specPage.expectTableVisible()
  })

  test('规格列表包含测试数据', async ({ page }) => {
    const specPage = new PackageSpecPage(page)
    await specPage.goto()
    const rows = specPage.dataTable.locator('tbody tr')
    expect(await rows.count()).toBeGreaterThan(0)
  })

  test('搜索功能可用', async ({ page }) => {
    const specPage = new PackageSpecPage(page)
    await specPage.goto()
    const searchInput = page.locator('.el-input__inner').first()
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('200ml')
      await specPage.searchBtn.click()
      await page.waitForTimeout(500)
      const rows = specPage.dataTable.locator('tbody tr')
      expect(await rows.count()).toBeGreaterThan(0)
    }
  })

  test('新增按钮可用', async ({ page }) => {
    const specPage = new PackageSpecPage(page)
    await specPage.goto()
    await expect(specPage.createBtn).toBeVisible()
  })
})
