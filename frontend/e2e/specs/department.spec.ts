import { test, expect } from '@playwright/test'
import { DepartmentManagePage } from '../poms/DepartmentManagePage'

test.describe('科室管理', () => {
  test('页面加载显示科室列表', async ({ page }) => {
    const deptPage = new DepartmentManagePage(page)
    await deptPage.goto()
    await deptPage.expectTableVisible()
  })

  test('科室列表包含测试数据', async ({ page }) => {
    const deptPage = new DepartmentManagePage(page)
    await deptPage.goto()
    const rows = deptPage.dataTable.locator('tbody tr')
    expect(await rows.count()).toBeGreaterThan(0)
  })

  test('搜索功能可用', async ({ page }) => {
    const deptPage = new DepartmentManagePage(page)
    await deptPage.goto()
    const searchInput = page.locator('.el-input__inner').first()
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('内科')
      await deptPage.searchBtn.click()
      await page.waitForTimeout(500)
      const rows = deptPage.dataTable.locator('tbody tr')
      expect(await rows.count()).toBeGreaterThan(0)
    }
  })

  test('新增按钮可用', async ({ page }) => {
    const deptPage = new DepartmentManagePage(page)
    await deptPage.goto()
    await expect(deptPage.createBtn).toBeVisible()
  })
})
