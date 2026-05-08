import { test, expect } from '@playwright/test'
import { login, navigate, waitTable, clickDialog, fillForm, submitForm } from './helpers'

test.describe('Flow 5: 基础数据管理', () => {

  test('医院管理列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/hospitals')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('科室管理列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/base/department')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('医师管理列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/base/doctor')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('药材目录列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/base/medicine')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('煎煮方案列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/schemes')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('包装规格列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/formula/package-spec')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('加水量公式列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/water-formulas')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('处方默认设置', async ({ page }) => {
    await login(page)
    await navigate(page, '/prescription-defaults')

    await page.waitForTimeout(2000)
    const content = page.locator('#app')
    await expect(content).toBeVisible({ timeout: 5000 })
  })

  test('异常: 创建弹窗表单', async ({ page }) => {
    await login(page)
    await navigate(page, '/hospitals')

    await waitTable(page)

    // Try to click create button
    const createBtn = page.locator('.el-button--primary').filter({ hasText: /新建|新增|创建/ })
    if (await createBtn.isVisible().catch(() => false)) {
      await createBtn.click()
      await page.waitForTimeout(1000)

      // Dialog should be visible
      const dialog = page.locator('.el-dialog')
      await expect(dialog).toBeVisible({ timeout: 5000 })

      // Close dialog
      const closeBtn = dialog.locator('.el-dialog__headerbtn')
      if (await closeBtn.isVisible().catch(() => false)) {
        await closeBtn.click()
        await page.waitForTimeout(500)
      }
    }
  })
})
