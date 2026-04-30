/**
 * PDA E2E 测试 - 工序确认模块
 *
 * 覆盖：单步确认、完整状态流转、跳步校验、二次确认弹窗
 */

const { test, expect } = require('@playwright/test')
const { testUsers, testTasks, resetMockServer, loginAs, setTaskStatus } = require('../fixtures/testData')

test.describe('PDA 工序确认', () => {
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

  test('TC-PDA-030: 开始泡药确认成功', async ({ page }) => {
    await page.fill('[data-testid="input-barcode"]', testTasks.pending.barcode)
    await page.click('[data-testid="btn-query"]')

    await page.click('[data-testid="btn-main-action"]')
    await expect(page).toHaveURL(/pages\/task\/confirm/)

    // 选择开始泡药步骤并确认
    await page.click('[data-testid="step-START_SOAK"]')
    await page.click('[data-testid="btn-confirm-step"]')

    await expect(page.locator('text=已确认：开始泡药')).toBeVisible()
  })

  test('TC-PDA-031: 结束泡药确认成功', async ({ page }) => {
    // 预置任务为泡药中
    await setTaskStatus(testTasks.pending.barcode, 'SOAKING')

    await page.fill('[data-testid="input-barcode"]', testTasks.pending.barcode)
    await page.click('[data-testid="btn-query"]')

    await page.click('[data-testid="btn-main-action"]')
    await page.click('[data-testid="step-END_SOAK"]')
    await page.click('[data-testid="btn-confirm-step"]')

    await expect(page.locator('text=已确认：结束泡药')).toBeVisible()
  })

  test('TC-PDA-032: 跳步校验-不能确认非当前步骤', async ({ page }) => {
    await page.fill('[data-testid="input-barcode"]', testTasks.pending.barcode)
    await page.click('[data-testid="btn-query"]')

    await page.click('[data-testid="btn-main-action"]')
    // 尝试直接选择结束泡药（跳步）
    await page.click('[data-testid="step-END_SOAK"]')

    await expect(page.locator('text=请先完成前置工序')).toBeVisible()
  })

  test('TC-PDA-033: 二次确认弹窗-结束煎药', async ({ page }) => {
    await setTaskStatus(testTasks.pending.barcode, 'DECOCTING')
    await page.request.post(`http://localhost:3456/__control/tasks/${testTasks.pending.taskId}/status`, {
      data: { status: 'DECOCTING' }
    })

    await page.fill('[data-testid="input-barcode"]', testTasks.pending.barcode)
    await page.click('[data-testid="btn-query"]')

    await page.click('[data-testid="btn-main-action"]')
    await page.click('[data-testid="step-END_DECOCT"]')
    await page.click('[data-testid="btn-confirm-step"]')

    // 应弹出二次确认
    await expect(page.locator('text=确认后不可撤销')).toBeVisible()
    await page.click('text=确认')

    await expect(page.locator('text=已确认：结束煎药')).toBeVisible()
  })

  test('TC-PDA-034: 完整状态流转 PENDING → COMPLETED', async ({ page }) => {
    const barcode = testTasks.pending.barcode
    const steps = [
      { status: 'PENDING', step: 'START_SOAK', label: '开始泡药' },
      { status: 'SOAKING', step: 'END_SOAK', label: '结束泡药' },
      { status: 'SOAKED', step: 'START_DECOCT', label: '开始煎药', needDevice: true, deviceCode: 'JYJ-001' },
      { status: 'DECOCTING', step: 'END_DECOCT', label: '结束煎药', confirm: true },
      { status: 'DECOCTED', step: 'START_POUR', label: '开始出液' },
      { status: 'POURING', step: 'END_POUR', label: '结束出液' },
      { status: 'POURED', step: 'START_PACKAGE', label: '开始包装', needDevice: true, deviceCode: 'BZB-001' },
      { status: 'PACKAGING', step: 'END_PACKAGE', label: '结束包装' },
      { status: 'PACKAGED', step: 'LABEL_CONFIRM', label: '贴标确认' },
      { status: 'LABELING', step: 'INSPECT_PASS', label: '质检通过', needPhoto: true, confirm: true }
    ]

    for (const s of steps) {
      // 先拍照（如果需要）
      if (s.needPhoto) {
        await page.goto(`/pages/photo/upload?taskId=${testTasks.pending.taskId}&mode=force`)
        await page.evaluate((t) => { uni.setStorageSync('pda_token', t) }, await loginAs('admin'))
        // 模拟直接上传（mock 中不需要真实文件）
        await page.click('[data-testid="btn-upload-photo"]')
        await page.waitForTimeout(500)
      }

      // 如果需要设备，先绑定
      if (s.needDevice) {
        await page.goto(`/pages/device/bind?taskId=${testTasks.pending.taskId}&stepType=${s.step}&barcode=${barcode}`)
        await page.evaluate((t) => { uni.setStorageSync('pda_token', t) }, await loginAs('admin'))
        await page.evaluate((code) => {
          const el = document.querySelector('[data-testid="btn-scan-device"] .scan-text')
          if (el) el.textContent = code
        }, s.deviceCode)
        await page.click('[data-testid="btn-bind"]')
        await page.waitForTimeout(800)
      }

      // 确认步骤
      await page.goto(`/pages/task/scan`)
      await page.evaluate((t) => { uni.setStorageSync('pda_token', t) }, await loginAs('admin'))
      await page.fill('[data-testid="input-barcode"]', barcode)
      await page.click('[data-testid="btn-query"]')
      await page.click('[data-testid="btn-main-action"]')

      await page.click(`[data-testid="step-${s.step}"]`)
      await page.click('[data-testid="btn-confirm-step"]')

      if (s.confirm) {
        await page.click('text=确认')
      }

      await page.waitForTimeout(500)
    }

    // 最终状态应为已完成
    await page.goto(`/pages/task/scan`)
    await page.fill('[data-testid="input-barcode"]', barcode)
    await page.click('[data-testid="btn-query"]')
    await expect(page.locator('[data-testid="task-status"]')).toContainText('已完成')
  })
})
