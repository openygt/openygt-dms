import { test, expect } from '@playwright/test'
import { DeviceUtilizationPage } from '../poms/DeviceUtilizationPage'

test.describe('设备利用率分析', () => {
  test('利用率页面加载显示图表', async ({ page }) => {
    const util = new DeviceUtilizationPage(page)
    await util.goto()
    await util.expectChartVisible()
  })

  test('利用率明细表格可见', async ({ page }) => {
    const util = new DeviceUtilizationPage(page)
    await util.goto()
    await util.expectTableVisible()
  })

  test('时间范围筛选按钮存在', async ({ page }) => {
    const util = new DeviceUtilizationPage(page)
    await util.goto()
    const radioGroup = page.locator('.el-radio-group')
    await expect(radioGroup).toBeVisible()
  })
})
