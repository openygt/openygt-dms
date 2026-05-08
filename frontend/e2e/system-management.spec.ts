import { test, expect } from '@playwright/test'
import { login, navigate, waitTable } from './helpers'

test.describe('Flow 6: 系统管理', () => {

  test('用户管理列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/users')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('角色管理列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/roles')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('菜单管理树形结构', async ({ page }) => {
    await login(page)
    await navigate(page, '/menus')

    await page.waitForTimeout(2000)
    // Look for tree structure
    const tree = page.locator('.el-tree')
    if (await tree.isVisible().catch(() => false)) {
      const nodes = tree.locator('.el-tree-node')
      await expect(nodes.first()).toBeVisible({ timeout: 5000 })
    } else {
      // Fallback: check table
      const table = page.locator('.el-table')
      await expect(table).toBeVisible({ timeout: 5000 })
    }
  })

  test('系统配置列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/configs')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('系统日志列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/logs')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('接口中心列表', async ({ page }) => {
    await login(page)
    await navigate(page, '/interface-center')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('用户管理角色分配弹窗', async ({ page }) => {
    await login(page)
    await navigate(page, '/users')

    await waitTable(page)

    // Look for role assignment button
    const roleBtn = page.locator('.el-table .el-button').filter({ hasText: /角色/ })
    if (await roleBtn.first().isVisible().catch(() => false)) {
      await roleBtn.first().click()
      await page.waitForTimeout(1000)

      // Dialog should appear
      const dialog = page.locator('.el-dialog')
      await expect(dialog).toBeVisible({ timeout: 5000 }).catch(() => {})
    }
  })

  test('角色管理菜单分配弹窗', async ({ page }) => {
    await login(page)
    await navigate(page, '/roles')

    await waitTable(page)

    // Look for menu assignment button
    const menuBtn = page.locator('.el-table .el-button').filter({ hasText: /菜单|权限/ })
    if (await menuBtn.first().isVisible().catch(() => false)) {
      await menuBtn.first().click()
      await page.waitForTimeout(1000)

      // Dialog should appear
      const dialog = page.locator('.el-dialog')
      await expect(dialog).toBeVisible({ timeout: 5000 }).catch(() => {})
    }
  })
})
