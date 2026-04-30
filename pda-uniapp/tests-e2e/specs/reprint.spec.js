/**
 * PDA E2E 测试 - 重打印模块
 *
 * 覆盖：重打印标签、未完成处方不能打印交接单
 */

const { test, expect } = require('@playwright/test')
const { testUsers, testTasks, resetMockServer, loginAs } = require('../fixtures/testData')

test.describe('PDA 重打印', () => {
  test.beforeEach(async ({ page }) => {
    await resetMockServer()
    const token = await loginAs('admin')
    await page.addInitScript((t) => {
      localStorage.setItem('pda_mock_env', '1')
      localStorage.setItem('pda_token', t)
    }, token)
    await page.goto('/#$1')
    await page.waitForLoadState('networkidle')
    await page.waitForFunction(() => typeof uni !== 'undefined')
  })

  test('TC-PDA-070: 重打印处方标签成功', async ({ page }) => {
    await page.goto('/#$1')

    await page.fill('[data-testid="reprint-barcode"]', testTasks.decocting.barcode)
    await page.click('[data-testid="btn-reprint-query"]')

    await expect(page.locator('text=' + testTasks.decocting.patientName)).toBeVisible()

    // 默认打印类型为 LABEL
    await page.click('[data-testid="btn-reprint-print"]')
    await expect(page.locator('text=打印任务已下发')).toBeVisible()
  })

  test('TC-PDA-071: 未完成处方不能打印交接单', async ({ page }) => {
    await page.goto('/#$1')

    await page.fill('[data-testid="reprint-barcode"]', testTasks.decocting.barcode)
    await page.click('[data-testid="btn-reprint-query"]')

    // 切换到交接单
    await page.click('text=交接单')
    await page.click('[data-testid="btn-reprint-print"]')

    await expect(page.locator('text=该处方尚未完成，无法打印交接单')).toBeVisible()
  })

  test('TC-PDA-072: 已完成处方可打印交接单', async ({ page }) => {
    await page.goto('/#$1')

    await page.fill('[data-testid="reprint-barcode"]', testTasks.completed.barcode)
    await page.click('[data-testid="btn-reprint-query"]')

    await page.click('text=交接单')
    await page.click('[data-testid="btn-reprint-print"]')

    await expect(page.locator('text=打印任务已下发')).toBeVisible()
  })
})
