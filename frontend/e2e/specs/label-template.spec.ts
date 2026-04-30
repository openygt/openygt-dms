import { test, expect } from '@playwright/test'
import { LabelTemplatePage } from '../poms/LabelTemplatePage'

test.describe('标签模板', () => {
  test('页面加载显示模板列表', async ({ page }) => {
    const tplPage = new LabelTemplatePage(page)
    await tplPage.goto()
    await tplPage.expectTableVisible()
  })

  test('模板列表包含测试数据', async ({ page }) => {
    const tplPage = new LabelTemplatePage(page)
    await tplPage.goto()
    const rows = tplPage.dataTable.locator('tbody tr')
    expect(await rows.count()).toBeGreaterThan(0)
  })

  test('搜索功能可用', async ({ page }) => {
    const tplPage = new LabelTemplatePage(page)
    await tplPage.goto()
    const searchInput = page.locator('.el-input__inner').first()
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('标签')
      await tplPage.searchBtn.click()
      await page.waitForTimeout(500)
      const rows = tplPage.dataTable.locator('tbody tr')
      expect(await rows.count()).toBeGreaterThan(0)
    }
  })

  test('新增按钮可用', async ({ page }) => {
    const tplPage = new LabelTemplatePage(page)
    await tplPage.goto()
    await expect(tplPage.createBtn).toBeVisible()
  })
})
