import { test, expect } from '@playwright/test'
import { login, navigate, waitTable } from './helpers'

test.describe('Flow 8: 打印管理', () => {

  test('打印中心队列列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/print-center')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })

    // Verify status tags
    const tags = page.locator('.el-table .el-tag')
    await expect(tags.first()).toBeVisible({ timeout: 5000 })
  })

  test('标签模板列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/print/template')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('打印机管理列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/print/printer')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('打印日志列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/print/log')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('异常: 打印重试操作', async ({ page }) => {
    await login(page)
    await navigate(page, '/print-center')

    await waitTable(page)

    // Look for an enabled retry button (disabled for non-FAILED rows)
    const retryBtn = page.locator('.el-table .el-button:not(.is-disabled)').filter({ hasText: /重试/ })
    if (await retryBtn.first().isVisible({ timeout: 3000 }).catch(() => false)) {
      await retryBtn.first().click()
      await page.waitForTimeout(1000)
    }
  })
})
