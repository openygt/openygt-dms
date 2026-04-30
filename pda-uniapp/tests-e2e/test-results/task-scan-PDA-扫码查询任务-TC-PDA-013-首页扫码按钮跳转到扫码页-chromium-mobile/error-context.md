# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: task-scan.spec.js >> PDA 扫码查询任务 >> TC-PDA-013: 首页扫码按钮跳转到扫码页
- Location: tests-e2e/specs/task-scan.spec.js:59:3

# Error details

```
TimeoutError: page.click: Timeout 10000ms exceeded.
Call log:
  - waiting for locator('[data-testid="btn-scan"]')

```

# Test source

```ts
  1  | /**
  2  |  * PDA E2E 测试 - 扫码查询任务模块
  3  |  *
  4  |  * 覆盖：扫码跳转、手动输入、历史记录、空状态
  5  |  */
  6  | 
  7  | const { test, expect } = require('@playwright/test')
  8  | const { testUsers, testTasks, resetMockServer, loginAs } = require('../fixtures/testData')
  9  | 
  10 | test.describe('PDA 扫码查询任务', () => {
  11 |   test.beforeEach(async ({ page }) => {
  12 |     await resetMockServer()
  13 |     const token = await loginAs('admin')
  14 |     await page.addInitScript((t) => {
  15 |       localStorage.setItem('pda_mock_env', '1')
  16 |       localStorage.setItem('pda_token', t)
  17 |     }, token)
  18 |     await page.goto('/#$1')
  19 |     await page.waitForLoadState('networkidle')
  20 |     await page.waitForFunction(() => typeof uni !== 'undefined')
  21 |   })
  22 | 
  23 |   test('TC-PDA-010: 手动输入条码查询任务成功', async ({ page }) => {
  24 |     await page.fill('[data-testid="input-barcode"]', testTasks.pending.barcode)
  25 |     await page.click('[data-testid="btn-query"]')
  26 | 
  27 |     await expect(page).toHaveURL(/pages\/task\/detail/)
  28 |     await expect(page.locator('[data-testid="task-barcode"]')).toHaveText(testTasks.pending.barcode)
  29 |     await expect(page.locator('[data-testid="task-status"]')).toContainText('待处理')
  30 |   })
  31 | 
  32 |   test('TC-PDA-011: 查询不存在条码提示错误', async ({ page }) => {
  33 |     await page.fill('[data-testid="input-barcode"]', 'NOTEXIST001')
  34 |     await page.click('[data-testid="btn-query"]')
  35 | 
  36 |     await expect(page.locator('text=查询失败')).toBeVisible()
  37 |     await expect(page).toHaveURL(/pages\/task\/scan/)
  38 |   })
  39 | 
  40 |   test('TC-PDA-012: 扫码历史记录可点击重查', async ({ page }) => {
  41 |     // 先查询一次
  42 |     await page.fill('[data-testid="input-barcode"]', testTasks.pending.barcode)
  43 |     await page.click('[data-testid="btn-query"]')
  44 |     await expect(page).toHaveURL(/pages\/task\/detail/)
  45 | 
  46 |     // 返回扫码页
  47 |     await page.goBack()
  48 |     await expect(page).toHaveURL(/pages\/task\/scan/)
  49 | 
  50 |     // 历史记录中应包含刚才的条码
  51 |     const historyItem = page.locator('[data-testid="history-item"]').first()
  52 |     await expect(historyItem).toContainText(testTasks.pending.barcode)
  53 | 
  54 |     // 点击历史记录
  55 |     await historyItem.click()
  56 |     await expect(page).toHaveURL(/pages\/task\/detail/)
  57 |   })
  58 | 
  59 |   test('TC-PDA-013: 首页扫码按钮跳转到扫码页', async ({ page }) => {
  60 |     await page.goto('/#$1')
> 61 |     await page.click('[data-testid="btn-scan"]')
     |                ^ TimeoutError: page.click: Timeout 10000ms exceeded.
  62 |     await expect(page).toHaveURL(/pages\/task\/scan/)
  63 |   })
  64 | })
  65 | 
```