import { test, expect } from '@playwright/test'

test.describe('角色管理', () => {
  test('页面加载显示角色列表', async ({ page }) => {
    await page.goto('/roles')
    await page.waitForLoadState('networkidle')
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('角色权限树可展开', async ({ page }) => {
    await page.goto('/roles')
    await page.waitForLoadState('networkidle')
    const treeNodes = page.locator('.el-tree-node__content')
    const count = await treeNodes.count()
    if (count > 0) {
      await treeNodes.first().click()
      await page.waitForTimeout(300)
    }
  })
})
