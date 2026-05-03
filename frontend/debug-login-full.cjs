const { chromium } = require('playwright')

async function run() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await context.newPage()
  
  const apiErrors = []
  page.on('response', async resp => {
    const url = resp.url()
    if (url.includes('/api/') && resp.status() >= 400) {
      try {
        const body = await resp.text()
        apiErrors.push({ status: resp.status(), url: url.split('?')[0], body: body.substring(0, 200) })
      } catch(e) {
        apiErrors.push({ status: resp.status(), url: url.split('?')[0], body: 'N/A' })
      }
    }
  })
  page.on('console', msg => {
    if (msg.type() === 'error') console.log('CONSOLE ERROR:', msg.text())
  })
  page.on('pageerror', err => console.log('PAGE ERROR:', err.message))
  
  // 1. 打开登录页
  await page.goto('http://localhost:5182/login')
  await page.waitForTimeout(1000)
  await page.screenshot({ path: '/tmp/debug-step1-login-page.png' })
  console.log('Step 1: login page loaded, URL:', page.url())
  
  // 2. 输入账号密码点击登录
  await page.locator('input[placeholder="用户名"]').fill('admin')
  await page.locator('input[placeholder="密码"]').fill('admin123')
  await page.locator('button:has-text("登录")').click()
  
  // 等待页面跳转或错误提示
  await page.waitForTimeout(3000)
  await page.screenshot({ path: '/tmp/debug-step2-after-login.png' })
  console.log('Step 2: after login click, URL:', page.url())
  
  // 3. 等待dashboard加载完成
  await page.waitForTimeout(3000)
  await page.screenshot({ path: '/tmp/debug-step3-dashboard.png' })
  console.log('Step 3: dashboard loaded, URL:', page.url())
  
  // 4. 进入处方管理
  await page.goto('http://localhost:5182/prescriptions')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)
  await page.screenshot({ path: '/tmp/debug-step4-prescriptions.png' })
  console.log('Step 4: prescriptions page, URL:', page.url())
  
  console.log('\n=== API Errors ===')
  apiErrors.forEach(e => console.log(`${e.status} ${e.url}`))
  if (apiErrors.length === 0) console.log('No API errors')
  
  await browser.close()
}

run().catch(e => { console.error(e); process.exit(1) })
