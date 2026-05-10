import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:5171'

test.describe('system 模块冒烟测试', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)
    await page.fill('input[name="username"]', 'admin')
    await page.fill('input[name="password"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL((u) => !u.pathname.endsWith('/login'), { timeout: 30000 })
  })

  test('用户管理页面可打开', async ({ page }) => {
    await page.goto(`${BASE_URL}/sys/users`)
    await expect(page.locator('text=用户管理').first()).toBeVisible({ timeout: 10000 })
  })

  test('角色权限页面可打开', async ({ page }) => {
    await page.goto(`${BASE_URL}/roles`)
    await expect(page.locator('text=角色').first()).toBeVisible({ timeout: 10000 })
  })

  test('参数配置页面可打开', async ({ page }) => {
    await page.goto(`${BASE_URL}/configs`)
    await expect(page.locator('text=参数').first()).toBeVisible({ timeout: 10000 })
  })

  test('日志管理页面可打开', async ({ page }) => {
    await page.goto(`${BASE_URL}/sys/logs`)
    await expect(page.locator('text=日志').first()).toBeVisible({ timeout: 10000 })
  })
})
