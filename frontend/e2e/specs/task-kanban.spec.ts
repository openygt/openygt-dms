import { test, expect } from '@playwright/test'

test.describe('任务看板', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/tasks')
    await page.waitForLoadState('networkidle')
  })

  test('页面加载默认列表视图', async ({ page }) => {
    await expect(page.locator('.el-table')).toBeVisible()
  })

  test('切换到看板视图', async ({ page }) => {
    await page.locator('text=看板视图').click()
    await page.waitForTimeout(500)
    await expect(page.locator('.kanban-board')).toBeVisible()
  })

  test('看板视图显示状态列', async ({ page }) => {
    await page.locator('text=看板视图').click()
    await page.waitForTimeout(500)
    await expect(page.locator('.kanban-column')).toHaveCount(12)
  })
})
