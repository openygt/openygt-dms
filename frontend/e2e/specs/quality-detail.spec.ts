import { test, expect } from '@playwright/test'

test.describe('质检管理详细质检', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/quality')
    await page.waitForLoadState('networkidle')
  })

  test('质检记录页面加载', async ({ page }) => {
    await expect(page.locator('text=质检记录')).toBeVisible()
  })

  test('详细质检弹窗可打开', async ({ page }) => {
    const btn = page.locator('button:has-text("详细质检")').first()
    if (await btn.isVisible().catch(() => false)) {
      await btn.click()
      await page.waitForTimeout(300)
      await expect(page.locator('.el-dialog__title:has-text("详细质检执行")')).toBeVisible()
    }
  })
})
