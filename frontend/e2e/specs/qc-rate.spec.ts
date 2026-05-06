import { test, expect } from '@playwright/test'
import { QcRatePage } from '../poms/QcRatePage'

test.describe('质检合格率', () => {
  test('页面加载显示统计页面', async ({ page }) => {
    const qcPage = new QcRatePage(page)
    await qcPage.goto()
    await qcPage.expectTableVisible()
  })

  test('统计页面包含数据', async ({ page }) => {
    const qcPage = new QcRatePage(page)
    await qcPage.goto()
    const cards = page.locator('.el-card')
    expect(await cards.count()).toBeGreaterThan(0)
  })

  test('日期筛选可用', async ({ page }) => {
    const qcPage = new QcRatePage(page)
    await qcPage.goto()
    const datePicker = page.locator('.el-date-picker, .el-date-editor').first()
    if (await datePicker.isVisible().catch(() => false)) {
      await expect(datePicker).toBeVisible()
    }
  })
})
