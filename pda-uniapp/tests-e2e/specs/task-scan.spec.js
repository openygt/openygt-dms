/**
 * PDA E2E 测试 - 扫码查询任务模块
 *
 * 覆盖：扫码跳转、手动输入、历史记录、空状态
 */

const { test, expect } = require('@playwright/test')
const { testUsers, testTasks, resetMockServer, loginAs } = require('../fixtures/testData')

test.describe('PDA 扫码查询任务', () => {
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

  test('TC-PDA-010: 手动输入条码查询任务成功', async ({ page }) => {
    await page.fill('[data-testid="input-barcode"]', testTasks.pending.barcode)
    await page.click('[data-testid="btn-query"]')

    await expect(page).toHaveURL(/pages\/task\/detail/)
    await expect(page.locator('[data-testid="task-barcode"]')).toHaveText(testTasks.pending.barcode)
    await expect(page.locator('[data-testid="task-status"]')).toContainText('待处理')
  })

  test('TC-PDA-011: 查询不存在条码提示错误', async ({ page }) => {
    await page.fill('[data-testid="input-barcode"]', 'NOTEXIST001')
    await page.click('[data-testid="btn-query"]')

    await expect(page.locator('text=查询失败')).toBeVisible()
    await expect(page).toHaveURL(/pages\/task\/scan/)
  })

  test('TC-PDA-012: 扫码历史记录可点击重查', async ({ page }) => {
    // 先查询一次
    await page.fill('[data-testid="input-barcode"]', testTasks.pending.barcode)
    await page.click('[data-testid="btn-query"]')
    await expect(page).toHaveURL(/pages\/task\/detail/)

    // 返回扫码页
    await page.goBack()
    await expect(page).toHaveURL(/pages\/task\/scan/)

    // 历史记录中应包含刚才的条码
    const historyItem = page.locator('[data-testid="history-item"]').first()
    await expect(historyItem).toContainText(testTasks.pending.barcode)

    // 点击历史记录
    await historyItem.click()
    await expect(page).toHaveURL(/pages\/task\/detail/)
  })

  test('TC-PDA-013: 首页扫码按钮跳转到扫码页', async ({ page }) => {
    await page.goto('/#$1')
    await page.click('[data-testid="btn-scan"]')
    await expect(page).toHaveURL(/pages\/task\/scan/)
  })
})
