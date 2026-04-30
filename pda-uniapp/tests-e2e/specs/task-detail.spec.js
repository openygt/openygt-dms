/**
 * PDA E2E 测试 - 任务详情模块
 *
 * 覆盖：详情展示、状态机驱动UI、设备绑定跳转、拍照跳转
 */

const { test, expect } = require('@playwright/test')
const { testUsers, testTasks, testDevices, resetMockServer, loginAs } = require('../fixtures/testData')

test.describe('PDA 任务详情页', () => {
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

  test('TC-PDA-020: 任务详情正确展示状态、药材、步骤时间线', async ({ page }) => {
    await page.fill('[data-testid="input-barcode"]', testTasks.decocting.barcode)
    await page.click('[data-testid="btn-query"]')

    await expect(page.locator('[data-testid="task-status"]')).toContainText('煎药中')
    await expect(page.locator('[data-testid="task-barcode"]')).toHaveText(testTasks.decocting.barcode)
    // 药材清单应可见
    await expect(page.locator('text=黄芪')).toBeVisible()
    await expect(page.locator('text=人参')).toBeVisible()
    // 步骤时间线
    await expect(page.locator('[data-testid="step-START_DECOCT"]')).toHaveClass(/step-current/)
  })

  test('TC-PDA-021: 待处理任务 nextAction 为开始泡药', async ({ page }) => {
    await page.fill('[data-testid="input-barcode"]', testTasks.pending.barcode)
    await page.click('[data-testid="btn-query"]')

    await expect(page.locator('[data-testid="btn-main-action"]')).toContainText('开始泡药')
  })

  test('TC-PDA-022: 需要设备的工序跳转设备绑定页', async ({ page }) => {
    // 设置任务状态为已泡药（下一步开始煎药需要设备）
    await page.request.post(`http://localhost:3456/__control/tasks/${testTasks.pending.taskId}/status`, {
      data: { status: 'SOAKED' }
    })

    await page.fill('[data-testid="input-barcode"]', testTasks.pending.barcode)
    await page.click('[data-testid="btn-query"]')

    await expect(page.locator('[data-testid="btn-main-action"]')).toContainText('开始煎药')
    await page.click('[data-testid="btn-main-action"]')

    // 应跳转到设备绑定页
    await expect(page).toHaveURL(/pages\/device\/bind/)
    await expect(page.locator('[data-testid="bind-task-barcode"]')).toContainText(testTasks.pending.barcode)
  })

  test('TC-PDA-023: 已完成任务主操作按钮禁用', async ({ page }) => {
    await page.fill('[data-testid="input-barcode"]', testTasks.completed.barcode)
    await page.click('[data-testid="btn-query"]')

    await expect(page.locator('[data-testid="task-status"]')).toContainText('已完成')
    await expect(page.locator('[data-testid="btn-main-action"]')).toBeDisabled()
  })

  test('TC-PDA-024: 拍照留档按钮跳转', async ({ page }) => {
    await page.fill('[data-testid="input-barcode"]', testTasks.decocting.barcode)
    await page.click('[data-testid="btn-query"]')

    await page.click('[data-testid="btn-go-photo"]')
    await expect(page).toHaveURL(/pages\/photo\/upload/)
  })
})
