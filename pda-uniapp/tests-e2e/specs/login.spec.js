/**
 * PDA E2E 测试 - 登录模块
 *
 * 覆盖：账号登录、扫码登录、异常校验
 */

const { test, expect } = require('@playwright/test')
const { testUsers, resetMockServer } = require('../fixtures/testData')

test.describe('PDA 登录模块', () => {
  test.beforeEach(async ({ page }) => {
    await resetMockServer()
    await page.addInitScript(() => { localStorage.setItem('pda_mock_env', '1') })
    await page.goto('/#$1')
    await page.waitForLoadState('networkidle')
    await page.waitForFunction(() => typeof uni !== 'undefined')
    await page.evaluate(() => { uni.setStorageSync('pda_mock_env', '1') })
  })

  test('TC-PDA-001: 账号密码登录成功', async ({ page }) => {
    await page.click('[data-testid="tab-account"]')
    await page.fill('[data-testid="input-username"] input', testUsers.admin.userCode)
    await page.fill('[data-testid="input-password"] input', testUsers.admin.password)
    await page.fill('[data-testid="input-device-code"] input', 'JYJ-001')
    await page.click('[data-testid="btn-login"]')

    await expect(page).toHaveURL(/pages\/index\/index/)
    await expect(page.locator('[data-testid="user-name"]')).toContainText(testUsers.admin.name)
    await expect(page.locator('[data-testid="device-code"]')).toContainText('JYJ-001')
  })

  test('TC-PDA-002: 扫码登录成功', async ({ page }) => {
    await page.click('[data-testid="tab-scan"]')
    // 通过隐藏 input 设置扫码值（Playwright fill 需要可见元素，用 evaluate 直接设置）
    await page.evaluate((barcode) => {
      const input = document.querySelector('[data-testid="input-scan-code-hidden"] input')
      if (input) {
        input.value = barcode
        input.dispatchEvent(new Event('input', { bubbles: true }))
        input.dispatchEvent(new Event('change', { bubbles: true }))
      }
    }, testUsers.admin.barcode)
    await page.fill('[data-testid="input-scan-device-code"] input', 'JYJ-001')
    await page.click('[data-testid="btn-scan-login"]')

    await expect(page).toHaveURL(/pages\/index\/index/)
    await expect(page.locator('[data-testid="user-name"]')).toContainText(testUsers.admin.name)
  })

  test('TC-PDA-003: 密码错误提示', async ({ page }) => {
    await page.click('[data-testid="tab-account"]')
    await page.fill('[data-testid="input-username"] input', testUsers.admin.userCode)
    await page.fill('[data-testid="input-password"] input', 'wrongpassword')
    await page.fill('[data-testid="input-device-code"] input', 'JYJ-001')
    await page.click('[data-testid="btn-login"]')

    await expect(page.locator('text=用户名或密码错误')).toBeVisible()
    await expect(page).toHaveURL(/pages\/login\/index/)
  })

  test('TC-PDA-004: 空设备码校验', async ({ page }) => {
    await page.click('[data-testid="tab-account"]')
    await page.fill('[data-testid="input-username"] input', testUsers.admin.userCode)
    await page.fill('[data-testid="input-password"] input', testUsers.admin.password)
    // 不填设备码
    await page.click('[data-testid="btn-login"]')

    await expect(page.locator('text=请输入设备编码')).toBeVisible()
  })

  test('TC-PDA-005: 扫码登录员工码不存在', async ({ page }) => {
    await page.click('[data-testid="tab-scan"]')
    // 输入一个不存在的员工码
    await page.evaluate((barcode) => {
      const input = document.querySelector('[data-testid="input-scan-code-hidden"] input')
      if (input) {
        input.value = barcode
        input.dispatchEvent(new Event('input', { bubbles: true }))
        input.dispatchEvent(new Event('change', { bubbles: true }))
      }
    }, 'NOTEXIST001')
    await page.fill('[data-testid="input-scan-device-code"] input', 'JYJ-001')
    await page.click('[data-testid="btn-scan-login"]')

    await expect(page.locator('text=员工码不存在')).toBeVisible()
  })
})
