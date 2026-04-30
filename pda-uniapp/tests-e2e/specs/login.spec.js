/**
 * PDA E2E 测试 - 登录模块
 *
 * 前置条件：Mock Server 已启动（由 global-setup.js 自动处理）
 */

const { test, expect } = require('@playwright/test')
const { testUsers } = require('../fixtures/testData')

test.describe('PDA 登录模块', () => {
  test.beforeEach(async ({ page }) => {
    // 重置 Mock Server 数据
    await page.request.post('http://localhost:3456/__control/reset')
  })

  test('TC-PDA-001: 账号密码登录成功', async ({ page }) => {
    await page.goto('/pages/login/index')

    await page.click('[data-testid="tab-account"]')
    await page.fill('[data-testid="input-username"]', testUsers.admin.userCode)
    await page.fill('[data-testid="input-password"]', testUsers.admin.password)
    await page.fill('[data-testid="input-device-code"]', 'JYJ-001')
    await page.click('[data-testid="btn-login"]')

    await expect(page).toHaveURL(/pages\/index\/index/)
    await expect(page.locator('[data-testid="user-name"]')).toHaveText(testUsers.admin.name)
  })

  test('TC-PDA-002: 扫码登录成功', async ({ page }) => {
    await page.goto('/pages/login/index')
    await page.click('[data-testid="tab-scan"]')
    await page.fill('[data-testid="input-scan-code"]', testUsers.admin.barcode)
    await page.click('[data-testid="btn-scan-login"]')

    await expect(page).toHaveURL(/pages\/index\/index/)
    await expect(page.locator('[data-testid="user-name"]')).toHaveText(testUsers.admin.name)
  })

  test('TC-PDA-004: 密码错误提示', async ({ page }) => {
    await page.goto('/pages/login/index')
    await page.click('[data-testid="tab-account"]')
    await page.fill('[data-testid="input-username"]', testUsers.admin.userCode)
    await page.fill('[data-testid="input-password"]', 'wrongpassword')
    await page.click('[data-testid="btn-login"]')

    await expect(page.locator('[data-testid="toast-message"]')).toContainText('用户名或密码错误')
    await expect(page).toHaveURL(/pages\/login\/index/)
  })
})
