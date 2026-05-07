import { test, expect } from '@playwright/test'
import { TracePage } from '../poms/TracePage'

test.describe('煎药过程追溯', () => {
  test('追溯查询页加载显示表格', async ({ page }) => {
    const trace = new TracePage(page)
    await trace.goto()
    await trace.expectTableVisible()
    // 验证表格有数据行
    const rows = trace.traceTable.locator('tbody tr')
    await expect(rows.first()).toBeVisible()
  })

  test('追溯查询支持处方号搜索', async ({ page }) => {
    const trace = new TracePage(page)
    await trace.goto()
    await trace.searchPrescriptionNo('PRE2026001')
    await page.waitForTimeout(500)
    await trace.expectTableVisible()
    // 搜索结果应包含PRE2026001
    const cell = trace.traceTable.locator('tbody tr td').filter({ hasText: 'PRE2026001' })
    await expect(cell).toBeVisible()
  })

  test('追溯查询支持状态过滤', async ({ page }) => {
    const trace = new TracePage(page)
    await trace.goto()
    await trace.filterByStatus('已完成')
    await page.waitForTimeout(500)
    await trace.expectTableVisible()
  })

  test('追溯查询支持患者搜索', async ({ page }) => {
    const trace = new TracePage(page)
    await trace.goto()
    const patientInput = page.locator('.el-input__inner').nth(1)
    await patientInput.fill('张三')
    await patientInput.press('Enter')
    await page.waitForTimeout(500)
    await trace.expectTableVisible()
  })

  test('点击详情进入追溯详情页', async ({ page }) => {
    const trace = new TracePage(page)
    await trace.goto()
    const rows = trace.traceTable.locator('tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThan(0)
    await trace.clickDetail(0)
    await expect(page).toHaveURL(/\/traces\/PRE/)
    // 详情页应显示信息卡片
    const infoCard = page.locator('.info-card')
    await expect(infoCard).toBeVisible({ timeout: 5000 })
  })

  test('追溯详情页显示基本信息', async ({ page }) => {
    const trace = new TracePage(page)
    await trace.goto()
    await trace.clickDetail(0)
    // 验证描述列表存在
    const descriptions = page.locator('.el-descriptions')
    await expect(descriptions).toBeVisible({ timeout: 5000 })
    // 验证处方号可见
    const prescriptionCell = page.locator('.el-descriptions__cell').filter({ hasText: /PRE\d+/ })
    await expect(prescriptionCell).toBeVisible()
  })

  test('追溯详情页工序时间轴Tab显示事件', async ({ page }) => {
    const trace = new TracePage(page)
    await trace.goto()
    await trace.clickDetail(0)
    await page.waitForTimeout(500)
    // 时间轴Tab默认激活，应显示时间线或空状态
    const timeline = page.locator('.el-timeline')
    const empty = page.locator('.el-empty')
    const hasTimeline = await timeline.isVisible().catch(() => false)
    const hasEmpty = await empty.isVisible().catch(() => false)
    expect(hasTimeline || hasEmpty).toBeTruthy()
  })

  test('追溯详情页温度曲线Tab可切换', async ({ page }) => {
    const trace = new TracePage(page)
    await trace.goto()
    await trace.clickDetail(0)
    await page.waitForTimeout(500)
    const tempTab = page.locator('.el-tabs__item').filter({ hasText: '温度曲线' })
    await expect(tempTab).toBeVisible()
    await tempTab.click()
    await page.waitForTimeout(800)
    // 温度曲线区域应可见
    const chart = page.locator('.temp-chart')
    const empty = page.locator('.el-empty')
    const hasChart = await chart.isVisible().catch(() => false)
    const hasEmpty = await empty.isVisible().catch(() => false)
    expect(hasChart || hasEmpty).toBeTruthy()
  })

  test('追溯详情页操作记录Tab可切换', async ({ page }) => {
    const trace = new TracePage(page)
    await trace.goto()
    await trace.clickDetail(0)
    await page.waitForTimeout(500)
    const eventTab = page.locator('.el-tabs__item').filter({ hasText: '操作记录' })
    await expect(eventTab).toBeVisible()
    await eventTab.click()
    await page.waitForTimeout(500)
    // 操作记录表格或空状态应可见
    const table = page.locator('.el-table')
    const empty = page.locator('.el-empty')
    const hasTable = await table.isVisible().catch(() => false)
    const hasEmpty = await empty.isVisible().catch(() => false)
    expect(hasTable || hasEmpty).toBeTruthy()
  })

  test('追溯详情页返回按钮可用', async ({ page }) => {
    const trace = new TracePage(page)
    await trace.goto()
    await trace.clickDetail(0)
    await page.waitForTimeout(500)
    const backBtn = page.locator('.page-header .el-button').filter({ hasText: '返回' })
    await backBtn.click()
    await page.waitForURL('/traces', { timeout: 5000 })
  })

  test('温度曲线快捷按钮跳转', async ({ page }) => {
    const trace = new TracePage(page)
    await trace.goto()
    const rows = trace.traceTable.locator('tbody tr')
    if (await rows.count() === 0) {
      test.skip('无追溯数据')
      return
    }
    await trace.clickTemperatureCurve(0)
    await expect(page).toHaveURL(/\/traces\/PRE/)
    // URL应包含tab=temperature参数
    const url = page.url()
    expect(url).toContain('tab=temperature')
  })
})
