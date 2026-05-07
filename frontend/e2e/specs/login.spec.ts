import { test, expect } from '@playwright/test'

test.describe('登录页', () => {
  test('登录表单可交互', async ({ page }) => {
    await page.goto('/login')
    // Element Plus el-input 内部是 .el-input__inner
    const inputs = page.locator('.el-input__inner')
    await expect(inputs.first()).toBeVisible()
    await expect(inputs.nth(1)).toBeVisible()
    await expect(page.locator('button.el-button--primary')).toBeEnabled()
  })

  test('空密码提交显示错误提示', async ({ page }) => {
    await page.goto('/login')
    const inputs = page.locator('.el-input__inner')
    await inputs.first().fill('admin')
    // 清空密码输入框
    await inputs.nth(1).fill('')
    await page.locator('button.el-button--primary').click()
    // Element Plus 使用 ElMessage 提示空密码
    await page.waitForTimeout(500)
    const message = page.locator('.el-message--warning, .el-message--error')
    const hasMessage = await message.count() > 0
    expect(hasMessage).toBeTruthy()
  })
})
