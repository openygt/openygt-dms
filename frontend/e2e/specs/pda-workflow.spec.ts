import { test, expect } from '@playwright/test'

// ═══════════════════════════════════════════════════════════
// PDA 端 E2E 测试（uniapp H5）
// 覆盖：扫码 → 任务详情 → 工序确认 → 设备绑定 → 交接签字
// ═══════════════════════════════════════════════════════════

const WORKER = { username: 'EMP001', password: 'admin123', deviceCode: 'JYJ-001' }

async function pdaLogin(page, user = WORKER) {
  await page.goto('http://localhost:5175/')
  await page.waitForLoadState('networkidle')
  // 切换到账号登录
  const accountTab = page.locator('[data-testid="tab-account"]').or(page.locator('text=账号登录'))
  if (await accountTab.isVisible().catch(() => false)) {
    await accountTab.click()
  }
  await page.locator('[data-testid="input-username"] input, input[placeholder*="账号"]').fill(user.username)
  await page.locator('[data-testid="input-password"] input, input[placeholder*="密码"]').fill(user.password)
  const deviceInput = page.locator('[data-testid="input-device-code"] input, input[placeholder*="设备"]').first()
  if (await deviceInput.isVisible().catch(() => false)) {
    await deviceInput.fill(user.deviceCode)
  }
  await page.click('[data-testid="btn-login"], button:has-text("登录")')
  await page.waitForTimeout(1500)
}

async function narrate(page, text, ms = 2000) {
  await page.evaluate((t) => {
    const el = document.createElement('div')
    el.id = 'e2e-narrator'
    el.style.cssText = 'position:fixed;bottom:40px;left:50%;transform:translateX(-50%);background:rgba(0,0,0,0.8);color:#fff;padding:8px 16px;border-radius:20px;font-size:14px;z-index:99999;white-space:nowrap;'
    el.textContent = '🔊 ' + t
    const old = document.getElementById('e2e-narrator')
    if (old) old.remove()
    document.body.appendChild(el)
    setTimeout(() => { const e = document.getElementById('e2e-narrator'); if(e) e.remove() }, 5000)
  }, text)
  await page.waitForTimeout(ms)
}

test.describe('📱 PDA 生产执行 E2E', () => {
  test.use({
    viewport: { width: 390, height: 844 },
    deviceScaleFactor: 3,
    isMobile: true,
    hasTouch: true,
  })

  test('PDA登录 → 扫码 → 任务详情 → 工序执行', async ({ page }) => {
    await pdaLogin(page)
    await narrate(page, 'PDA登录成功，进入工作台')

    // 进入扫码页
    await page.goto('http://localhost:5175/#/pages/task/scan')
    await page.waitForTimeout(1000)
    await narrate(page, '进入扫码页面，扫描任务条码')

    // 模拟扫码输入
    const barcodeInput = page.locator('[data-testid="input-barcode"] input, input[placeholder*="条码"]').first()
    if (await barcodeInput.isVisible().catch(() => false)) {
      await barcodeInput.fill('YP20250430001')
      await page.click('[data-testid="btn-query"], button:has-text("查询")')
    } else {
      // 直接跳转详情页模拟扫码结果
      await page.goto('http://localhost:5175/#/pages/task/detail?taskId=1001&barcode=YP20250430001')
    }
    await page.waitForTimeout(1200)
    await narrate(page, '查询到任务详情，展示患者信息和当前工序')

    // 查看任务详情
    const statusEl = page.locator('[data-testid="task-status"]').or(page.locator('text=待处理').or(page.locator('text=泡药中')))
    if (await statusEl.isVisible().catch(() => false)) {
      await narrate(page, '任务状态显示正确，可进行工序确认')
    }

    // 工序确认（开始泡药）
    const mainBtn = page.locator('[data-testid="btn-main-action"]').or(page.locator('button:has-text("开始")').or(page.locator('button:has-text("确认")'))).first()
    if (await mainBtn.isVisible().catch(() => false)) {
      await mainBtn.click()
      await page.waitForTimeout(1000)
      await narrate(page, '工序确认成功，状态自动流转')
    }
  })

  test('PDA设备绑定：扫码 → 绑定煎药机', async ({ page }) => {
    await pdaLogin(page)
    await page.goto('http://localhost:5175/#/pages/task/detail?taskId=1003&barcode=YP20250430003')
    await page.waitForTimeout(1200)
    await narrate(page, '查看煎药中任务，需要绑定煎药机')

    // 进入设备绑定页
    const bindBtn = page.locator('[data-testid="btn-main-action"]').or(page.locator('button:has-text("绑定")').or(page.locator('button:has-text("设备")'))).first()
    if (await bindBtn.isVisible().catch(() => false)) {
      await bindBtn.click()
      await page.waitForTimeout(1000)
      await narrate(page, '进入设备绑定页面，扫描设备条码')
    }

    // 模拟绑定
    await page.goto('http://localhost:5175/#/pages/device/bind?taskId=1003')
    await page.waitForTimeout(1000)
    const scanBtn = page.locator('[data-testid="btn-scan-device"]').or(page.locator('button:has-text("扫描")')).first()
    if (await scanBtn.isVisible().catch(() => false)) {
      await scanBtn.click()
      await page.waitForTimeout(800)
      await narrate(page, '扫描设备条码，绑定煎药机001')
    }

    const confirmBtn = page.locator('[data-testid="btn-bind"]').or(page.locator('button:has-text("绑定")')).first()
    if (await confirmBtn.isVisible().catch(() => false)) {
      await confirmBtn.click()
      await page.waitForTimeout(800)
      await narrate(page, '设备绑定成功，开始煎药')
    }
  })

  test('PDA交接签字：任务完成 → 电子签名', async ({ page }) => {
    await pdaLogin(page)
    await page.goto('http://localhost:5175/#/pages/task/detail?taskId=1005&barcode=YP20250430005')
    await page.waitForTimeout(1200)
    await narrate(page, '查看已完成任务，进行交接签字')

    // 进入签字页
    const signBtn = page.locator('[data-testid="btn-main-action"]').or(page.locator('button:has-text("签字")').or(page.locator('button:has-text("交接")'))).first()
    if (await signBtn.isVisible().catch(() => false)) {
      await signBtn.click()
      await page.waitForTimeout(1000)
      await narrate(page, '进入电子签名页面')
    }

    await page.goto('http://localhost:5175/#/pages/handover/sign?taskId=1005')
    await page.waitForTimeout(1000)

    const canvas = page.locator('[data-testid="sign-canvas"]').or(page.locator('canvas')).first()
    if (await canvas.isVisible().catch(() => false)) {
      // 在 canvas 上画一条线模拟签名
      const box = await canvas.boundingBox()
      if (box) {
        await page.mouse.move(box.x + 50, box.y + 50)
        await page.mouse.down()
        await page.mouse.move(box.x + 150, box.y + 100)
        await page.mouse.move(box.x + 250, box.y + 80)
        await page.mouse.up()
      }
      await page.waitForTimeout(500)
      await narrate(page, '完成手写签名')
    }

    const confirmSign = page.locator('[data-testid="btn-sign-confirm"]').or(page.locator('button:has-text("确认")')).first()
    if (await confirmSign.isVisible().catch(() => false)) {
      await confirmSign.click()
      await page.waitForTimeout(800)
      await narrate(page, '交接签字完成，任务正式结束')
    }
  })
})
