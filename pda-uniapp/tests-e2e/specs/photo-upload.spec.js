/**
 * PDA E2E 测试 - 拍照留档模块
 *
 * 覆盖：上传成功、强制模式跳过确认、照片类型切换
 */

const { test, expect } = require('@playwright/test')
const { testUsers, testTasks, resetMockServer, loginAs } = require('../fixtures/testData')

test.describe('PDA 拍照留档', () => {
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

  test('TC-PDA-060: 拍照上传成功', async ({ page }) => {
    await page.goto(`/pages/photo/upload?taskId=${testTasks.pending.taskId}&barcode=${testTasks.pending.barcode}`)

    // 模拟添加照片（直接操作 photos 数组）
    await page.evaluate(() => {
      const vm = document.querySelector('.container').__vue__
      if (vm) vm.photos.push({ path: 'mock://photo1.jpg', size: 1024 })
    })

    await page.click('[data-testid="btn-upload-photo"]')
    await expect(page.locator('text=上传成功')).toBeVisible()
  })

  test('TC-PDA-061: 强制模式跳过确认', async ({ page }) => {
    await page.goto(`/pages/photo/upload?taskId=${testTasks.pending.taskId}&barcode=${testTasks.pending.barcode}&mode=force`)

    await expect(page.locator('[data-testid="photo-force-mode"]')).toBeVisible()
    await page.click('[data-testid="btn-skip-photo"]')

    // 应弹出跳过确认弹窗
    await expect(page.locator('text=跳过确认')).toBeVisible()
    await page.click('text=确认')
  })

  test('TC-PDA-062: 照片类型切换', async ({ page }) => {
    await page.goto(`/pages/photo/upload?taskId=${testTasks.pending.taskId}&barcode=${testTasks.pending.barcode}`)

    await page.click('[data-testid="photo-type-WEIGHING"]')
    await expect(page.locator('[data-testid="photo-type-WEIGHING"]')).toHaveClass(/active/)

    await page.click('[data-testid="photo-type-EXCEPTION"]')
    await expect(page.locator('[data-testid="photo-type-EXCEPTION"]')).toHaveClass(/active/)
  })
})
