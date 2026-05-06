import { test, expect } from '@playwright/test'

test.describe('接口中心', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/interface-center')
    await page.waitForLoadState('networkidle')
  })

  test('页面加载显示接口配置表格', async ({ page }) => {
    await expect(page.locator('.el-card').first()).toBeVisible()
    await expect(page.locator('.el-table').first()).toBeVisible()
  })

  test('接口日志区域显示', async ({ page }) => {
    await expect(page.locator('text=接口调用日志')).toBeVisible()
  })
})
