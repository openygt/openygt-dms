import { test, expect } from '@playwright/test'
import { DoctorManagePage } from '../poms/DoctorManagePage'

test.describe('医师管理', () => {
  test('页面加载显示医师列表', async ({ page }) => {
    const doctorPage = new DoctorManagePage(page)
    await doctorPage.goto()
    await doctorPage.expectTableVisible()
  })

  test('医师列表包含测试数据', async ({ page }) => {
    const doctorPage = new DoctorManagePage(page)
    await doctorPage.goto()
    const rows = doctorPage.dataTable.locator('tbody tr')
    expect(await rows.count()).toBeGreaterThan(0)
  })

  test('搜索功能可用', async ({ page }) => {
    const doctorPage = new DoctorManagePage(page)
    await doctorPage.goto()
    const searchInput = page.locator('.el-input__inner').first()
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('张仲景')
      await doctorPage.searchBtn.click()
      await page.waitForTimeout(500)
      const rows = doctorPage.dataTable.locator('tbody tr')
      expect(await rows.count()).toBeGreaterThan(0)
    }
  })

  test('新增按钮可用', async ({ page }) => {
    const doctorPage = new DoctorManagePage(page)
    await doctorPage.goto()
    await expect(doctorPage.createBtn).toBeVisible()
  })
})
