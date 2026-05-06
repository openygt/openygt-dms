import { test, expect } from '@playwright/test'
import { TimeMonitorPage } from '../poms/TimeMonitorPage'

test.describe('时效监控', () => {
  test('页面加载显示统计卡片', async ({ page }) => {
    const tmPage = new TimeMonitorPage(page)
    await tmPage.goto()
    await tmPage.expectStatsVisible()
  })

  test('统计卡片包含数值', async ({ page }) => {
    const tmPage = new TimeMonitorPage(page)
    await tmPage.goto()
    const normalText = await tmPage.statNormal.textContent()
    expect(normalText).toMatch(/^\d+$/)
  })

  test('时效监控列表表格可见', async ({ page }) => {
    const tmPage = new TimeMonitorPage(page)
    await tmPage.goto()
    await tmPage.expectTableVisible()
  })

  test('配置规则按钮可用', async ({ page }) => {
    const tmPage = new TimeMonitorPage(page)
    await tmPage.goto()
    await expect(tmPage.ruleBtn).toBeVisible()
    await expect(tmPage.ruleBtn).toBeEnabled()
  })

  test('刷新按钮可用', async ({ page }) => {
    const tmPage = new TimeMonitorPage(page)
    await tmPage.goto()
    await expect(tmPage.refreshBtn).toBeVisible()
    await expect(tmPage.refreshBtn).toBeEnabled()
  })

  test('表格包含阶段和剩余时间列', async ({ page }) => {
    const tmPage = new TimeMonitorPage(page)
    await tmPage.goto()
    const headers = tmPage.dataTable.locator('thead th')
    const headerTexts = await headers.allTextContents()
    const joined = headerTexts.join(' ')
    expect(joined).toContain('阶段')
    expect(joined).toContain('剩余时间')
    expect(joined).toContain('状态')
  })
})
