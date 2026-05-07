import { test as setup } from '@playwright/test'

const authFile = 'e2e/.auth/user.json'

setup('authenticate', async ({ page }) => {
  await page.goto('/login')
  // Element Plus el-input 没有 name 属性，使用 placeholder 定位
  await page.locator('.el-input__inner').first().fill('admin')
  await page.locator('.el-input__inner').nth(1).fill('admin123')
  await page.locator('button:has-text("登录"), button.el-button--primary').click()
  await page.waitForURL('/dashboard')
  await page.context().storageState({ path: authFile })
})
