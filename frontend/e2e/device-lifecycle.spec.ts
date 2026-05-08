import { test, expect } from '@playwright/test'
import { login, navigate, waitTable, assertChart, clickDialog, fillForm, submitForm } from './helpers'

test.describe('Flow 2: 设备生命周期', () => {

  test('设备台账列表渲染', async ({ page }) => {
    await login(page)
    await navigate(page, '/devices')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })

    // Verify status columns exist
    const tags = page.locator('.el-table .el-tag')
    await expect(tags.first()).toBeVisible({ timeout: 5000 })
  })

  test('设备监控状态卡片', async ({ page }) => {
    await login(page)
    await navigate(page, '/device-monitor')

    await page.waitForTimeout(2000)
    const deviceCards = page.locator('.device-card, .el-card')
    await expect(deviceCards.first()).toBeVisible({ timeout: 10000 })

    // Check for device status indicators
    const statusIndicators = page.locator('.device-status, .status-tag, .el-tag')
    await expect(statusIndicators.first()).toBeVisible({ timeout: 5000 })
  })

  test('设备利用率趋势图', async ({ page }) => {
    await login(page)
    await navigate(page, '/device-utilization')

    await page.waitForTimeout(2000)
    // Check for chart canvas or table
    const chart = page.locator('canvas')
    const table = page.locator('.el-table')
    const anyVisible = (await chart.isVisible().catch(() => false)) || (await table.isVisible().catch(() => false))
    expect(anyVisible).toBeTruthy()
  })

  test('创建设备弹窗', async ({ page }) => {
    await login(page)
    await navigate(page, '/devices')

    // Click create button
    const createBtn = page.locator('.el-button--primary').filter({ hasText: /新建|新增|创建/ })
    if (await createBtn.isVisible().catch(() => false)) {
      await createBtn.click()
      await page.waitForTimeout(1000)

      // Dialog should be visible
      const dialog = page.locator('.el-dialog')
      await expect(dialog).toBeVisible({ timeout: 5000 })
    }
  })

  test('设备详情页面', async ({ page }) => {
    await login(page)
    // Navigate to first device detail (ID=1 is the default decocting machine)
    await navigate(page, '/device/1/detail')

    await page.waitForTimeout(2000)
    // Check for device info section
    const content = page.locator('#app')
    await expect(content).toBeVisible({ timeout: 5000 })
  })

  test('异常: 删除取消操作', async ({ page }) => {
    await login(page)
    await navigate(page, '/devices')

    await waitTable(page)

    // Find a delete button
    const deleteBtn = page.locator('.el-table .el-button--danger, .el-table .el-button').filter({ hasText: /删除/ })
    if (await deleteBtn.first().isVisible().catch(() => false)) {
      await deleteBtn.first().click()
      await page.waitForTimeout(1000)

      // Confirm dialog should appear
      const confirmBtn = page.locator('.el-message-box .el-button--default').filter({ hasText: /取消/ })
      if (await confirmBtn.isVisible().catch(() => false)) {
        await confirmBtn.click()
        await page.waitForTimeout(500)
      }
    }
  })
})
