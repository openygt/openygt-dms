const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')

async function fetchRealToken() {
  const res = await fetch('http://localhost:18080/api/v1/rbac/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' }),
  })
  const data = await res.json()
  if (data.code !== 200) {
    throw new Error(`Login failed: ${data.message}`)
  }
  console.log('[record] Real token obtained for user:', data.data.username)
  return data.data.token
}

async function run() {
  const videoDir = path.join(__dirname, 'demo-video-raw')
  if (!fs.existsSync(videoDir)) fs.mkdirSync(videoDir, { recursive: true })

  const token = await fetchRealToken()

  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    recordVideo: { dir: videoDir, size: { width: 1440, height: 900 } }
  })
  const page = await context.newPage()

  // 设置登录态
  await page.goto('http://localhost:5182/login')
  await page.evaluate((t) => { localStorage.setItem('token', t) }, token)

  // 1. 处方管理页面
  await page.goto('http://localhost:5182/prescriptions')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)

  // 2. 点击新增处方
  await page.getByRole('button', { name: '新增处方' }).click()
  await page.waitForTimeout(1000)

  // 3. 填写患者信息
  const dialog = page.locator('.el-dialog')
  await dialog.locator('input[placeholder*="必填"]').fill('演示患者')
  await page.waitForTimeout(600)

  // 选择医院（弹窗内placeholder="请选择"的医院下拉框）
  const hospitalSelect = dialog.locator('.el-select').filter({ has: dialog.locator('input[placeholder="请选择"]') }).first()
  if (await hospitalSelect.isVisible().catch(() => false)) {
    await hospitalSelect.click()
    await page.waitForTimeout(400)
    const option = page.locator('.el-select-dropdown__item').first()
    if (await option.isVisible().catch(() => false)) await option.click()
    await page.waitForTimeout(400)
  }

  // 医师
  const doctorInput = dialog.locator('.el-form-item').filter({ hasText: '医师' }).locator('input')
  if (await doctorInput.isVisible().catch(() => false)) {
    await doctorInput.fill('王医师')
  }
  await page.waitForTimeout(600)

  // 4. 添加药材
  await dialog.getByRole('button', { name: '+ 添加药材' }).click()
  await page.waitForTimeout(600)

  const rows = dialog.locator('.el-table__body .el-table__row')
  const row0 = rows.first()
  await row0.locator('input').first().fill('当归')
  await page.waitForTimeout(400)
  await row0.locator('.el-input-number input').fill('15')
  await page.waitForTimeout(400)

  await dialog.getByRole('button', { name: '+ 添加药材' }).click()
  await page.waitForTimeout(600)
  const row1 = rows.nth(1)
  await row1.locator('input').first().fill('川芎')
  await page.waitForTimeout(400)
  await row1.locator('.el-input-number input').fill('10')
  await page.waitForTimeout(400)

  // 5. 保存
  await dialog.locator('.el-dialog__footer').getByRole('button', { name: '保存' }).click()
  await page.waitForTimeout(2500)

  // 6. 列表确认
  await page.waitForSelector('.el-table__row:has-text("演示患者")', { timeout: 10000 })
  await page.waitForTimeout(1000)

  // 7. 处方接收页面
  await page.goto('http://localhost:5182/prod/receive')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)

  // 8. 接收处方
  const receiveBtn = page.locator('.el-table__row').filter({ hasText: '演示患者' }).locator('button').filter({ hasText: '接收' }).first()
  if (await receiveBtn.isVisible().catch(() => false)) {
    await receiveBtn.click()
    await page.waitForTimeout(500)
    const okBtn = page.locator('.el-message-box__btns').getByRole('button', { name: '确定' }).first()
    if (await okBtn.isVisible().catch(() => false)) await okBtn.click()
    await page.waitForTimeout(2000)
  }

  // 9. 查看详情
  const detailBtn = page.locator('.el-table__row').filter({ hasText: '演示患者' }).locator('button').filter({ hasText: '详情' }).first()
  if (await detailBtn.isVisible().catch(() => false)) {
    await detailBtn.click()
    await page.waitForTimeout(3000)
  }

  await context.close()
  await browser.close()

  // 获取视频文件
  const files = fs.readdirSync(videoDir).filter(f => f.endsWith('.webm'))
  if (files.length === 0) {
    console.error('No video file generated')
    process.exit(1)
  }
  // 按修改时间取最新的
  const videoPath = files.map(f => ({ name: f, mtime: fs.statSync(path.join(videoDir, f)).mtime }))
    .sort((a, b) => b.mtime - a.mtime)[0].name
  const fullPath = path.join(videoDir, videoPath)
  console.log('VIDEO_PATH=' + fullPath)
}

run().catch(e => { console.error(e); process.exit(1) })
