import { test, expect } from '@playwright/test'
import { WorkloadPage } from '../poms/WorkloadPage'

test.describe('工作量统计', () => {
  test('工作量统计页加载显示统计卡片', async ({ page }) => {
    const workload = new WorkloadPage(page)
    await workload.goto()
    await workload.expectSummaryVisible()
  })

  test('工作量统计表格可见', async ({ page }) => {
    const workload = new WorkloadPage(page)
    await workload.goto()
    await workload.expectTableVisible()
  })

  test('工作量统计支持工作类型过滤', async ({ page }) => {
    const workload = new WorkloadPage(page)
    await workload.goto()
    await workload.filterByWorkType('调剂')
    await page.waitForTimeout(500)
    await workload.expectTableVisible()
  })

  test('分页组件存在', async ({ page }) => {
    const workload = new WorkloadPage(page)
    await workload.goto()
    await expect(workload.pagination).toBeVisible()
  })
})
