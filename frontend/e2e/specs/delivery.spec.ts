import { test, expect } from '@playwright/test'
import { DeliveryManagePage } from '../poms/DeliveryManagePage'

test.describe('交付管理', () => {
  test('页面加载显示交付记录列表', async ({ page }) => {
    const deliveryPage = new DeliveryManagePage(page)
    await deliveryPage.goto()
    await deliveryPage.expectTableVisible()
  })

  test('交付列表包含测试数据', async ({ page }) => {
    const deliveryPage = new DeliveryManagePage(page)
    await deliveryPage.goto()
    const rows = deliveryPage.dataTable.locator('tbody tr')
    expect(await rows.count()).toBeGreaterThan(0)
  })

  test('搜索功能可用', async ({ page }) => {
    const deliveryPage = new DeliveryManagePage(page)
    await deliveryPage.goto()
    const searchInput = page.locator('.el-input__inner').first()
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('张三')
      await deliveryPage.searchBtn.click()
      await page.waitForTimeout(500)
      const rows = deliveryPage.dataTable.locator('tbody tr')
      expect(await rows.count()).toBeGreaterThan(0)
    }
  })

  test('新增按钮可用', async ({ page }) => {
    const deliveryPage = new DeliveryManagePage(page)
    await deliveryPage.goto()
    await expect(deliveryPage.createBtn).toBeVisible()
  })
})
