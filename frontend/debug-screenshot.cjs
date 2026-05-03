const { chromium } = require('playwright')

async function run() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await context.newPage()
  
  // 注入token
  await page.goto('http://localhost:5182/login')
  await page.evaluate(() => {
    localStorage.setItem('token', 'eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJ1c2VySWQiOjEsInN1YiI6ImFkbWluIiwidXNlcm5hbWUiOiJhZG1pbiIsInJvbGVzIjpbIlJPTEVfQURNSU4iXSwicGVybWlzc2lvbnMiOlsiUk9MRV9BRE1JTiJdLCJpYXQiOjE3NzYwMTg5M30.')
  })
  
  // 访问处方管理
  await page.goto('http://localhost:5182/prescriptions')
  await page.waitForTimeout(5000)
  
  // 截图
  await page.screenshot({ path: '/tmp/debug-prescriptions.png', fullPage: true })
  
  // 获取控制台日志
  const logs = await page.evaluate(() => {
    return window.__logs || []
  })
  
  console.log('Screenshot saved to /tmp/debug-prescriptions.png')
  
  // 获取页面标题
  const title = await page.title()
  console.log('Page title:', title)
  
  // 获取页面HTML片段
  const html = await page.content()
  console.log('HTML length:', html.length)
  
  await browser.close()
}

run().catch(e => { console.error(e); process.exit(1) })
