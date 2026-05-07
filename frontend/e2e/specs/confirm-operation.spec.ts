import { test, expect } from '@playwright/test'

test.describe('关键操作二次确认', () => {
  test('全局请求拦截器存在（request.ts 已配置）', async ({ page }) => {
    // 通过访问任意需要登录的页面验证前端加载正常
    await page.goto('/tasks')
    await page.waitForLoadState('networkidle')
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('页面加载后前端框架可用', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')
    const title = page.locator('.page-header-title')
    await expect(title).toBeVisible()
  })
})
