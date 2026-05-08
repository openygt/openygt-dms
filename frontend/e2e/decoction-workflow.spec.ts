import { test, expect } from '@playwright/test'
import { login, navigate, waitTable, assertCard, assertChart } from './helpers'

test.describe('Flow 1: 煎煮核心流程', () => {

  test('登录并验证 Dashboard KPI', async ({ page }) => {
    await login(page)

    // Should redirect to dashboard
    await page.waitForURL(/\/(dashboard|\/)/, { timeout: 10000 })

    // Verify KPI cards are rendered (main dashboard)
    const kpiCards = page.locator('.kpi-card')
    await expect(kpiCards.first()).toBeVisible({ timeout: 10000 })
    const kpiCount = await kpiCards.count()
    expect(kpiCount).toBeGreaterThanOrEqual(4)
  })

  test('处方接收页面列表渲染', async ({ page }) => {
    await login(page)
    await navigate(page, '/prod/receive')

    // Verify table renders — use data-testid for the main table
    await waitTable(page, '[data-testid="data-table"]')
    const rows = page.locator('[data-testid="data-table"] .el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible()
  })

  test('处方管理列表渲染', async ({ page }) => {
    await login(page)
    await navigate(page, '/prescriptions')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible()
  })

  test('任务看板状态标签', async ({ page }) => {
    await login(page)
    await navigate(page, '/tasks')

    await waitTable(page)
    // Verify status tags exist in the table
    const tags = page.locator('.el-table .el-tag')
    await expect(tags.first()).toBeVisible({ timeout: 5000 })
  })

  test('设备监控页面渲染', async ({ page }) => {
    await login(page)
    await navigate(page, '/device-monitor')

    // Wait for device cards to load
    await page.waitForTimeout(2000)
    const deviceCards = page.locator('.device-card, .el-card')
    const cardCount = await deviceCards.count()
    expect(cardCount).toBeGreaterThanOrEqual(1)
  })

  test('质检管理列表渲染', async ({ page }) => {
    await login(page)
    await navigate(page, '/quality')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible()
  })

  test('交付管理列表渲染', async ({ page }) => {
    await login(page)
    await navigate(page, '/prod/delivery')

    await waitTable(page)
    const rows = page.locator('.el-table__body-wrapper tbody tr')
    await expect(rows.first()).toBeVisible()
  })

  test('数据看板图表渲染', async ({ page }) => {
    await login(page)
    await navigate(page, '/eq-dashboard')

    // Check KPI metric cards
    await page.waitForTimeout(2000)
    const metricCards = page.locator('.metric-card')
    await expect(metricCards.first()).toBeVisible({ timeout: 10000 })

    // Check for ECharts canvas (stage distribution or hourly trend)
    const charts = page.locator('.chart-container canvas')
    const chartCount = await charts.count()
    expect(chartCount).toBeGreaterThanOrEqual(1)
  })

  test('异常: 错误密码登录', async ({ page }) => {
    await page.goto('/login')
    await page.waitForSelector('.login-card', { timeout: 10000 })

    // Fill wrong password
    await page.locator('.login-card input').first().fill('admin')
    await page.locator('.login-card input').nth(1).fill('wrongpassword')
    await page.locator('.login-card .el-button--primary').click()

    // Should show error message
    await page.waitForTimeout(1000)
    const errMsg = page.locator('.el-message--error')
    await expect(errMsg).toBeVisible({ timeout: 5000 })
  })

  test('异常: 空数据空状态', async ({ page }) => {
    await login(page)
    // Navigate to a page that may have no data
    await navigate(page, '/trace/exception')

    // Either the table renders or empty state shows
    await page.waitForTimeout(2000)
    const empty = page.locator('.el-empty')
    const table = page.locator('.el-table')
    const anyVisible = (await empty.isVisible().catch(() => false)) || (await table.isVisible().catch(() => false))
    expect(anyVisible).toBeTruthy()
  })
})
