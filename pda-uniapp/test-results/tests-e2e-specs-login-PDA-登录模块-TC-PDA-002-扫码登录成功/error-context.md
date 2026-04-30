# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: tests-e2e/specs/login.spec.js >> PDA 登录模块 >> TC-PDA-002: 扫码登录成功
- Location: tests-e2e/specs/login.spec.js:31:3

# Error details

```
Error: page.goto: Protocol error (Page.navigate): Cannot navigate to invalid URL
Call log:
  - navigating to "/pages/login/index", waiting until "load"

```

# Test source

```ts
  1  | /**
  2  |  * PDA E2E 测试 - 登录模块
  3  |  *
  4  |  * 覆盖：账号登录、扫码登录、异常校验
  5  |  */
  6  | 
  7  | const { test, expect } = require('@playwright/test')
  8  | const { testUsers, resetMockServer } = require('../fixtures/testData')
  9  | 
  10 | test.describe('PDA 登录模块', () => {
  11 |   test.beforeEach(async ({ page }) => {
  12 |     await resetMockServer()
> 13 |     await page.goto('/pages/login/index')
     |                ^ Error: page.goto: Protocol error (Page.navigate): Cannot navigate to invalid URL
  14 |     await page.waitForLoadState('networkidle')
  15 |     await page.waitForFunction(() => typeof uni !== 'undefined')
  16 |     await page.evaluate(() => { uni.setStorageSync('pda_mock_env', '1') })
  17 |   })
  18 | 
  19 |   test('TC-PDA-001: 账号密码登录成功', async ({ page }) => {
  20 |     await page.click('[data-testid="tab-account"]')
  21 |     await page.fill('[data-testid="input-username"] input', testUsers.admin.userCode)
  22 |     await page.fill('[data-testid="input-password"] input', testUsers.admin.password)
  23 |     await page.fill('[data-testid="input-device-code"] input', 'JYJ-001')
  24 |     await page.click('[data-testid="btn-login"]')
  25 | 
  26 |     await expect(page).toHaveURL(/pages\/index\/index/)
  27 |     await expect(page.locator('[data-testid="user-name"]')).toContainText(testUsers.admin.name)
  28 |     await expect(page.locator('[data-testid="device-code"]')).toContainText('JYJ-001')
  29 |   })
  30 | 
  31 |   test('TC-PDA-002: 扫码登录成功', async ({ page }) => {
  32 |     await page.click('[data-testid="tab-scan"]')
  33 |     // 模拟扫码输入（直接设置 input 值）
  34 |     await page.evaluate((barcode) => {
  35 |       const el = document.querySelector('[data-testid="input-scan-code"] input')
  36 |       if (el) el.textContent = barcode
  37 |     }, testUsers.admin.barcode)
  38 |     await page.fill('[data-testid="input-scan-device-code"] input', 'JYJ-001')
  39 |     await page.click('[data-testid="btn-scan-login"]')
  40 | 
  41 |     await expect(page).toHaveURL(/pages\/index\/index/)
  42 |     await expect(page.locator('[data-testid="user-name"]')).toContainText(testUsers.admin.name)
  43 |   })
  44 | 
  45 |   test('TC-PDA-003: 密码错误提示', async ({ page }) => {
  46 |     await page.click('[data-testid="tab-account"]')
  47 |     await page.fill('[data-testid="input-username"] input', testUsers.admin.userCode)
  48 |     await page.fill('[data-testid="input-password"] input', 'wrongpassword')
  49 |     await page.fill('[data-testid="input-device-code"] input', 'JYJ-001')
  50 |     await page.click('[data-testid="btn-login"]')
  51 | 
  52 |     await expect(page.locator('text=用户名或密码错误')).toBeVisible()
  53 |     await expect(page).toHaveURL(/pages\/login\/index/)
  54 |   })
  55 | 
  56 |   test('TC-PDA-004: 空设备码校验', async ({ page }) => {
  57 |     await page.click('[data-testid="tab-account"]')
  58 |     await page.fill('[data-testid="input-username"] input', testUsers.admin.userCode)
  59 |     await page.fill('[data-testid="input-password"] input', testUsers.admin.password)
  60 |     // 不填设备码
  61 |     await page.click('[data-testid="btn-login"]')
  62 | 
  63 |     await expect(page.locator('text=请输入设备编码')).toBeVisible()
  64 |   })
  65 | 
  66 |   test('TC-PDA-005: 扫码登录员工码不存在', async ({ page }) => {
  67 |     await page.click('[data-testid="tab-scan"]')
  68 |     await page.fill('[data-testid="input-scan-device-code"] input', 'JYJ-001')
  69 |     await page.click('[data-testid="btn-scan-login"]')
  70 | 
  71 |     await expect(page.locator('text=员工码不存在')).toBeVisible()
  72 |   })
  73 | })
  74 | 
```