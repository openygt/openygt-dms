# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: specs/device-detail.spec.ts >> 设备详情页 >> 页面加载显示仪表盘和温度曲线
- Location: e2e/specs/device-detail.spec.ts:5:3

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: locator('[data-testid="temp-gauge"], .gauge-chart')
Expected: visible
Timeout: 10000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 10000ms
  - waiting for locator('[data-testid="temp-gauge"], .gauge-chart')

```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test'
  2  | import { DeviceDetailPage } from '../poms/DeviceDetailPage'
  3  | 
  4  | test.describe('设备详情页', () => {
  5  |   test('页面加载显示仪表盘和温度曲线', async ({ page }) => {
  6  |     const detail = new DeviceDetailPage(page)
  7  |     await detail.goto('DECOCT_001')
  8  |     // 图表容器存在即可（ECharts 初始化可能需要时间）
  9  |     const gauge = page.locator('[data-testid="temp-gauge"], .gauge-chart')
  10 |     const tempChart = page.locator('[data-testid="temp-chart"], .temp-chart')
> 11 |     await expect(gauge).toBeVisible({ timeout: 10000 })
     |                         ^ Error: expect(locator).toBeVisible() failed
  12 |     await expect(tempChart).toBeVisible({ timeout: 10000 })
  13 |   })
  14 | 
  15 |   test('温度曲线时间范围切换', async ({ page }) => {
  16 |     const detail = new DeviceDetailPage(page)
  17 |     await detail.goto('DECOCT_001')
  18 |     // 切换 1小时 / 6小时 / 24小时
  19 |     const tabs = page.locator('.time-range-tabs .el-radio-button')
  20 |     const count = await tabs.count()
  21 |     if (count > 0) {
  22 |       for (let i = 0; i < Math.min(count, 3); i++) {
  23 |         await tabs.nth(i).click()
  24 |         await page.waitForTimeout(300)
  25 |       }
  26 |     }
  27 |   })
  28 | 
  29 |   test('指令面板按钮可点击', async ({ page }) => {
  30 |     const detail = new DeviceDetailPage(page)
  31 |     await detail.goto('DECOCT_001')
  32 |     // 使用更通用的选择器
  33 |     const buttons = page.locator('.command-grid .el-button, [data-testid^="command-btn-"]')
  34 |     const count = await buttons.count()
  35 |     if (count === 0) {
  36 |       test.skip('指令按钮未渲染，跳过')
  37 |       return
  38 |     }
  39 |     expect(count).toBeGreaterThan(0)
  40 |     // 点击暂停按钮（相对安全）
  41 |     const pauseBtn = buttons.filter({ hasText: '暂停' })
  42 |     if (await pauseBtn.count() > 0) {
  43 |       await pauseBtn.click()
  44 |     }
  45 |   })
  46 | 
  47 |   test('返回按钮回到监控面板', async ({ page }) => {
  48 |     const detail = new DeviceDetailPage(page)
  49 |     await detail.goto('DECOCT_001')
  50 |     await detail.page.goto('/device-monitor')
  51 |     await expect(page).toHaveURL('/device-monitor')
  52 |   })
  53 | })
  54 | 
```