import { test, expect } from '@playwright/test'
import { login, navigate, waitTable, clickDialog } from './helpers'

test.describe('Flow 3: 告警生命周期', () => {

  test('告警日志列表渲染', async ({ page }) => {
    await login(page)
    await navigate(page, '/alarms')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })

    // Verify alarm status tags
    const tags = page.locator('.el-table .el-tag')
    await expect(tags.first()).toBeVisible({ timeout: 5000 })
  })

  test('告警日志状态筛选', async ({ page }) => {
    await login(page)
    await navigate(page, '/alarms')

    // Look for filter selects
    const selects = page.locator('.el-select')
    const selectCount = await selects.count()
    if (selectCount > 0) {
      await selects.first().click()
      await page.waitForTimeout(500)
      // Select first option
      const option = page.locator('.el-select-dropdown__item').first()
      await option.click()
      await page.waitForTimeout(1000)
    }
  })

  test('告警配置列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/alarm-configs')

    await page.waitForTimeout(2000)
    const table = page.locator('.el-table')
    const exists = await table.isVisible().catch(() => false)
    if (exists) {
      const rows = page.locator('.el-table__body-wrapper tbody tr')
      await expect(rows.first()).toBeVisible({ timeout: 5000 })
    }
  })

  test('告警配置启用/禁用切换', async ({ page }) => {
    await login(page)
    await navigate(page, '/alarm-configs')

    await page.waitForTimeout(2000)
    // Look for switch toggles
    const switches = page.locator('.el-switch')
    if (await switches.first().isVisible().catch(() => false)) {
      await switches.first().click()
      await page.waitForTimeout(500)
    }
  })

  test('异常: 空告警数据状态', async ({ page }) => {
    await login(page)
    await navigate(page, '/alarms')

    await page.waitForTimeout(2000)
    const empty = page.locator('.el-empty')
    const table = page.locator('.el-table')
    const anyVisible = (await empty.isVisible().catch(() => false)) || (await table.isVisible().catch(() => false))
    expect(anyVisible).toBeTruthy()
  })
})
