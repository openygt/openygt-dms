const { chromium } = require('playwright')

async function run() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await context.newPage()
  
  // 1. 打开登录页
  await page.goto('http://localhost:5182/login')
  await page.waitForTimeout(1000)
  await page.screenshot({ path: '/tmp/test-01-login-page.png' })
  console.log('1. 登录页')
  
  // 2. 输入账号密码
  await page.locator('input[placeholder="用户名"]').fill('admin')
  await page.locator('input[placeholder="密码"]').fill('admin123')
  await page.screenshot({ path: '/tmp/test-02-filled.png' })
  console.log('2. 填写账号密码')
  
  // 3. 点击登录
  await page.locator('button:has-text("登录")').click()
  await page.waitForTimeout(3000)
  await page.screenshot({ path: '/tmp/test-03-dashboard.png' })
  console.log('3. 登录后 -> 生产看板')
  
  // 4. 进入处方管理
  await page.goto('http://localhost:5182/prescriptions')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)
  await page.screenshot({ path: '/tmp/test-04-prescriptions.png' })
  console.log('4. 处方管理页面')
  
  // 5. 点击新增处方
  await page.getByRole('button', { name: '新增处方' }).click()
  await page.waitForTimeout(1000)
  await page.screenshot({ path: '/tmp/test-05-create-dialog.png' })
  console.log('5. 新增处方弹窗')
  
  // 6. 填写患者信息
  const dialog = page.locator('.el-dialog')
  await dialog.locator('input[placeholder*="必填"]').fill('测试患者')
  await page.waitForTimeout(500)
  
  // 选择医院
  const hospitalSelect = dialog.locator('.el-select').filter({ has: dialog.locator('input[placeholder="请选择"]') }).first()
  if (await hospitalSelect.isVisible().catch(() => false)) {
    await hospitalSelect.click()
    await page.waitForTimeout(400)
    const option = page.locator('.el-select-dropdown__item').first()
    if (await option.isVisible().catch(() => false)) await option.click()
    await page.waitForTimeout(400)
  }
  
  // 填写医师
  const doctorInput = dialog.locator('.el-form-item').filter({ hasText: '医师' }).locator('input')
  if (await doctorInput.isVisible().catch(() => false)) {
    await doctorInput.fill('王医师')
  }
  await page.waitForTimeout(500)
  await page.screenshot({ path: '/tmp/test-06-patient-info.png' })
  console.log('6. 填写患者信息')
  
  // 7. 添加药材
  await dialog.getByRole('button', { name: '+ 添加药材' }).click()
  await page.waitForTimeout(600)
  const rows = dialog.locator('.el-table__body .el-table__row')
  await rows.first().locator('input').first().fill('当归')
  await page.waitForTimeout(300)
  await rows.first().locator('.el-input-number input').fill('15')
  await page.waitForTimeout(300)
  
  await dialog.getByRole('button', { name: '+ 添加药材' }).click()
  await page.waitForTimeout(600)
  await rows.nth(1).locator('input').first().fill('川芎')
  await page.waitForTimeout(300)
  await rows.nth(1).locator('.el-input-number input').fill('10')
  await page.waitForTimeout(300)
  await page.screenshot({ path: '/tmp/test-07-herbs.png' })
  console.log('7. 添加药材')
  
  // 8. 保存
  await dialog.locator('.el-dialog__footer').getByRole('button', { name: '保存' }).click()
  await page.waitForTimeout(3000)
  await page.screenshot({ path: '/tmp/test-08-saved.png' })
  console.log('8. 保存后')
  
  // 9. 刷新页面确认
  await page.reload()
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)
  await page.screenshot({ path: '/tmp/test-09-reloaded.png' })
  console.log('9. 刷新后列表')
  
  // 10. 进入处方接收
  await page.goto('http://localhost:5182/prod/receive')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)
  await page.screenshot({ path: '/tmp/test-10-receive.png' })
  console.log('10. 处方接收页面')
  
  // 11. 查找并接收
  const receiveBtn = page.locator('.el-table__row').filter({ hasText: '测试患者' }).locator('button').filter({ hasText: '接收' }).first()
  if (await receiveBtn.isVisible().catch(() => false)) {
    await receiveBtn.click()
    await page.waitForTimeout(500)
    const okBtn = page.locator('.el-message-box__btns').getByRole('button', { name: '确定' }).first()
    if (await okBtn.isVisible().catch(() => false)) await okBtn.click()
    await page.waitForTimeout(2000)
  }
  await page.screenshot({ path: '/tmp/test-11-received.png' })
  console.log('11. 接收后')
  
  await browser.close()
  console.log('\n全部完成，截图保存在 /tmp/test-*.png')
}

run().catch(e => { console.error(e); process.exit(1) })
