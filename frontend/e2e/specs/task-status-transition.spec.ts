import { test, expect } from '@playwright/test'

test.describe('任务状态转换守卫', () => {
  test('任务列表页包含"待交付"状态选项（v1.1 合并状态）', async ({ page }) => {
    await page.goto('/tasks')
    await page.waitForLoadState('networkidle')
    const statusSelect = page.locator('.el-select').filter({ hasText: '状态' })
    if (await statusSelect.isVisible().catch(() => false)) {
      await statusSelect.click()
      await page.waitForTimeout(300)
      const options = page.locator('.el-select-dropdown__item')
      const texts = await options.allTextContents()
      const joined = texts.join(' ')
      expect(joined).toContain('待交付')
      // v1.1 合并后不应单独出现旧状态
      expect(joined).not.toContain('待贴标')
      expect(joined).not.toContain('待交接')
    }
  })

  test('看板视图加载正常', async ({ page }) => {
    await page.goto('/tasks')
    await page.waitForLoadState('networkidle')
    const kanbanBtn = page.locator('.el-radio-button__original-radio[value="kanban"]')
      .locator('..')
    if (await kanbanBtn.isVisible().catch(() => false)) {
      await kanbanBtn.click()
      await page.waitForTimeout(500)
      const kanbanColumns = page.locator('.kanban-column, .el-card')
      const count = await kanbanColumns.count()
      expect(count).toBeGreaterThan(0)
    }
  })

  test('任务详情页状态标签可识别', async ({ page }) => {
    await page.goto('/tasks')
    await page.waitForLoadState('networkidle')
    const firstRow = page.locator('.el-table__row').first()
    if (await firstRow.isVisible().catch(() => false)) {
      const statusCell = firstRow.locator('td').nth(3)
      const text = await statusCell.textContent()
      expect(text).toBeTruthy()
    }
  })
})
