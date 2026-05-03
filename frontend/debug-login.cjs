const { chromium } = require('playwright')

async function run() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await context.newPage()
  
  const errors = []
  page.on('response', resp => {
    if (resp.url().includes('/login') && resp.status() >= 400) {
      errors.push(`${resp.status()} ${resp.url()}`)
    }
  })
  page.on('console', msg => {
    if (msg.type() === 'error') errors.push('CONSOLE: ' + msg.text())
  })
  
  await page.goto('http://localhost:5182/login')
  await page.waitForTimeout(1000)
  
  await page.locator('input[placeholder="用户名"]').fill('admin')
  await page.locator('input[placeholder="密码"]').fill('admin123')
  await page.locator('button:has-text("登录")').click()
  await page.waitForTimeout(3000)
  
  await page.screenshot({ path: '/tmp/debug-login-result.png' })
  console.log('Errors:', errors)
  console.log('URL:', page.url())
  
  await browser.close()
}

run().catch(e => { console.error(e); process.exit(1) })
