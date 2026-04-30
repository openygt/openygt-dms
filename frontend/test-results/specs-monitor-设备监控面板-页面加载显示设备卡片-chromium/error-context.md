# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: specs/monitor.spec.ts >> 设备监控面板 >> 页面加载显示设备卡片
- Location: e2e/specs/monitor.spec.ts:5:3

# Error details

```
Error: expect(received).toBeTruthy()

Received: false
```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test'
  2  | import { DeviceMonitorPage } from '../poms/DeviceMonitorPage'
  3  | 
  4  | test.describe('设备监控面板', () => {
  5  |   test('页面加载显示设备卡片', async ({ page }) => {
  6  |     const monitor = new DeviceMonitorPage(page)
  7  |     await monitor.goto()
  8  |     // 至少能看到一个设备卡片，或空状态
  9  |     const hasCards = await monitor.deviceCards.count() > 0
  10 |     const emptyState = page.locator('.ygt-empty')
> 11 |     expect(hasCards || (await emptyState.isVisible().catch(() => false))).toBeTruthy()
     |                                                                           ^ Error: expect(received).toBeTruthy()
  12 |   })
  13 | 
  14 |   test('设备类型过滤切换', async ({ page }) => {
  15 |     const monitor = new DeviceMonitorPage(page)
  16 |     await monitor.goto()
  17 |     const filterCount = await monitor.typeFilter.count()
  18 |     if (filterCount === 0) {
  19 |       test.skip('过滤组件未渲染，跳过')
  20 |       return
  21 |     }
  22 |     await monitor.filterByType('decoct')
  23 |     await monitor.filterByType('pack')
  24 |     await monitor.filterByType('all')
  25 |   })
  26 | 
  27 |   test('刷新按钮可点击', async ({ page }) => {
  28 |     const monitor = new DeviceMonitorPage(page)
  29 |     await monitor.goto()
  30 |     const count = await monitor.refreshBtn.count()
  31 |     if (count === 0) {
  32 |       test.skip('刷新按钮未渲染，跳过')
  33 |       return
  34 |     }
  35 |     await expect(monitor.refreshBtn).toBeEnabled()
  36 |     await monitor.refreshBtn.click()
  37 |     await page.waitForTimeout(500)
  38 |   })
  39 | 
  40 |   test('点击设备卡片进入详情页', async ({ page }) => {
  41 |     const monitor = new DeviceMonitorPage(page)
  42 |     await monitor.goto()
  43 |     const count = await monitor.deviceCards.count()
  44 |     if (count === 0) {
  45 |       test.skip('无设备数据，跳过')
  46 |       return
  47 |     }
  48 |     const firstCard = monitor.deviceCards.first()
  49 |     await firstCard.click()
  50 |     await page.waitForURL(/\/device\/.*\/detail/, { timeout: 5000 })
  51 |   })
  52 | 
  53 |   test('急停按钮弹出确认对话框', async ({ page }) => {
  54 |     const monitor = new DeviceMonitorPage(page)
  55 |     await monitor.goto()
  56 |     const count = await monitor.deviceCards.count()
  57 |     if (count === 0) {
  58 |       test.skip('无设备数据，跳过')
  59 |       return
  60 |     }
  61 |     const firstCode = await monitor.deviceCards.first().getAttribute('data-device-code') || 'unknown'
  62 |     await monitor.clickEmergencyStop(firstCode)
  63 |     const dialog = page.locator('[data-testid="confirm-dialog"]')
  64 |     await expect(dialog).toBeVisible({ timeout: 5000 })
  65 |     await page.locator('[data-testid="cancel-emergency-stop"]').click()
  66 |     await expect(dialog).toBeHidden({ timeout: 5000 })
  67 |   })
  68 | 
  69 |   test('换班按钮弹出交接对话框', async ({ page }) => {
  70 |     const monitor = new DeviceMonitorPage(page)
  71 |     await monitor.goto()
  72 |     const count = await monitor.deviceCards.count()
  73 |     if (count === 0) {
  74 |       test.skip('无设备数据，跳过')
  75 |       return
  76 |     }
  77 |     const firstCode = await monitor.deviceCards.first().getAttribute('data-device-code') || 'unknown'
  78 |     await monitor.clickShiftHandover(firstCode)
  79 |     const dialog = page.locator('[data-testid="shift-handover-dialog"]')
  80 |     await expect(dialog).toBeVisible({ timeout: 5000 })
  81 |     await page.locator('.el-dialog__footer .el-button').first().click()
  82 |     await expect(dialog).toBeHidden({ timeout: 5000 })
  83 |   })
  84 | })
  85 | 
```