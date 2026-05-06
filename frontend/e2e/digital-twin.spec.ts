import { test, expect } from '@playwright/test'

test('数字孪生 - 设备监控与操控演示', async ({ page }) => {
  // 1. 登录
  await page.goto('/login')
  await page.fill('input[placeholder="用户名"]', 'jgy001')
  await page.fill('input[placeholder="密码"]', 'admin123')
  await page.click('button:has-text("登录")')
  await page.waitForURL('**/dashboard', { timeout: 10000 })

  // 2. 导航到数字孪生页面
  await page.goto('/digital-twin')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1500)

  // 3. 等待页面加载完成
  await expect(page.locator('text=设备数字孪生')).toBeVisible()

  // 4. 截图：全部设备视图
  await page.waitForTimeout(1000)

  // 5. 筛选煎药机
  await page.click('text=煎药机')
  await page.waitForTimeout(1500)

  // 6. 筛选包装机
  await page.click('text=包装机')
  await page.waitForTimeout(1500)

  // 7. 筛选标签打印机
  await page.click('text=标签打印机')
  await page.waitForTimeout(1500)

  // 8. 筛选激光打印机
  await page.click('text=激光打印机')
  await page.waitForTimeout(1500)

  // 9. 回到全部设备
  await page.click('text=全部设备')
  await page.waitForTimeout(1500)

  // 10. 向下滚动查看所有设备
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight / 2))
  await page.waitForTimeout(1000)
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
  await page.waitForTimeout(1000)
  await page.evaluate(() => window.scrollTo(0, 0))
  await page.waitForTimeout(1000)

  // 11. 点击一个在线煎药机的操作按钮（开始浸泡）
  const onlineDecoctCard = page.locator('.dt-device.dt-type-1').filter({ hasNot: page.locator('.dt-status-offline') }).first()
  if (await onlineDecoctCard.isVisible().catch(() => false)) {
    const startBtn = onlineDecoctCard.locator('button:has-text("开始浸泡")')
    if (await startBtn.isVisible().catch(() => false)) {
      await startBtn.click()
      await page.waitForTimeout(2000)
    }
  }

  // 12. 点击一个在线包装机的操作按钮
  const onlinePackCard = page.locator('.dt-device.dt-type-2').filter({ hasNot: page.locator('.dt-status-offline') }).first()
  if (await onlinePackCard.isVisible().catch(() => false)) {
    const startPackBtn = onlinePackCard.locator('button:has-text("开始包装")')
    if (await startPackBtn.isVisible().catch(() => false)) {
      await startPackBtn.click()
      await page.waitForTimeout(2000)
    }
  }

  // 13. 点击一个在线打印机的操作按钮
  const onlinePrintCard = page.locator('.dt-device.dt-type-3').filter({ hasNot: page.locator('.dt-status-offline') }).first()
  if (await onlinePrintCard.isVisible().catch(() => false)) {
    const reprintBtn = onlinePrintCard.locator('button:has-text("补打标签")')
    if (await reprintBtn.isVisible().catch(() => false)) {
      await reprintBtn.click()
      await page.waitForTimeout(2000)
    }
  }

  // 14. 展示急停操作
  const runningCard = page.locator('.dt-device').filter({ has: page.locator('button:has-text("急停")') }).first()
  if (await runningCard.isVisible().catch(() => false)) {
    const emergencyBtn = runningCard.locator('button:has-text("急停")')
    if (await emergencyBtn.isVisible().catch(() => false)) {
      await emergencyBtn.click()
      await page.waitForTimeout(1500)
      // 取消急停
      await page.click('button:has-text("取消")')
      await page.waitForTimeout(1500)
    }
  }

  // 15. 最后停留展示完整页面
  await page.goto('/digital-twin')
  await page.waitForTimeout(3000)
})
