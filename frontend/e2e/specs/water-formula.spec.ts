import { test, expect } from '@playwright/test'
import { WaterFormulaPage } from '../poms/WaterFormulaPage'

test.describe('加水量公式管理', () => {
  test('公式管理页加载显示表格', async ({ page }) => {
    const formula = new WaterFormulaPage(page)
    await formula.goto()
    await formula.expectTableVisible()
  })

  test('公式表格包含测试按钮', async ({ page }) => {
    const formula = new WaterFormulaPage(page)
    await formula.goto()
    const testBtns = formula.formulaTable.locator('.el-button').filter({ hasText: '测试' })
    const count = await testBtns.count()
    expect(count).toBeGreaterThan(0)
  })

  test('新增公式按钮可用', async ({ page }) => {
    const formula = new WaterFormulaPage(page)
    await formula.goto()
    await expect(formula.addBtn).toBeVisible()
  })
})
