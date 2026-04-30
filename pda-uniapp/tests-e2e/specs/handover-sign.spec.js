/**
 * PDA E2E 测试 - 交接签字模块
 *
 * 覆盖：签字成功、未签字提交校验
 */

const { test, expect } = require('@playwright/test')
const { testUsers, testTasks, resetMockServer, loginAs, setTaskStatus } = require('../fixtures/testData')

test.describe('PDA 交接签字', () => {
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

  test('TC-PDA-050: 交接签字成功', async ({ page }) => {
    // 设置任务为待交接状态（INSPECTING）
    await setTaskStatus(testTasks.pending.barcode, 'INSPECTING')
    await page.goto(`/pages/handover/sign?taskId=${testTasks.pending.taskId}&barcode=${testTasks.pending.barcode}`)

    await expect(page.locator('[data-testid="sign-task-barcode"]')).toContainText(testTasks.pending.barcode)

    // 模拟签字（在 canvas 上触发 touch 事件较复杂，直接模拟 hasSigned=true）
    await page.evaluate(() => {
      const canvas = document.querySelector('[data-testid="sign-canvas"]')
      if (canvas) {
        // 触发一个简单的 touch 事件序列
        const rect = canvas.getBoundingClientRect()
        const start = new TouchEvent('touchstart', { touches: [new Touch({ identifier: 1, target: canvas, clientX: rect.left + 10, clientY: rect.top + 10 })] })
        const move = new TouchEvent('touchmove', { touches: [new Touch({ identifier: 1, target: canvas, clientX: rect.left + 50, clientY: rect.top + 50 })] })
        const end = new TouchEvent('touchend', { touches: [] })
        canvas.dispatchEvent(start)
        canvas.dispatchEvent(move)
        canvas.dispatchEvent(end)
      }
    })

    await page.click('[data-testid="btn-sign-confirm"]')
    await expect(page.locator('text=签字成功')).toBeVisible()
  })

  test('TC-PDA-051: 未签字不能提交', async ({ page }) => {
    await setTaskStatus(testTasks.pending.barcode, 'INSPECTING')
    await page.goto(`/pages/handover/sign?taskId=${testTasks.pending.taskId}&barcode=${testTasks.pending.barcode}`)

    await page.click('[data-testid="btn-sign-confirm"]')
    await expect(page.locator('text=请先签字')).toBeVisible()
  })
})
