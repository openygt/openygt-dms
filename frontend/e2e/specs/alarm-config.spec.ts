import { test, expect } from '@playwright/test'
import { AlarmConfigPage } from '../poms/AlarmConfigPage'

test.describe('告警配置', () => {
  test('告警配置页加载显示表格', async ({ page }) => {
    const config = new AlarmConfigPage(page)
    await config.goto()
    await config.expectTableVisible()
  })

  test('告警配置表格包含级别标签', async ({ page }) => {
    const config = new AlarmConfigPage(page)
    await config.goto()
    const tags = config.configTable.locator('.el-tag')
    const count = await tags.count()
    expect(count).toBeGreaterThan(0)
  })

  test('新增配置按钮可用', async ({ page }) => {
    const config = new AlarmConfigPage(page)
    await config.goto()
    await expect(config.addBtn).toBeVisible()
  })
})
