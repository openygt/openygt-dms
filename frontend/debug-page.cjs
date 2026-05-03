const { chromium } = require('playwright')
const TOKEN = 'eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJ1c2VySWQiOjEsInN1YiI6ImFkbWluIiwidXNlcm5hbWUiOiJhZG1pbiIsInJvbGVzIjpbIlJPTEVfQURNSU4iXSwicGVybWlzc2lvbnMiOlsiUk9MRV9BRE1JTiJdLCJpYXQiOjE3NzYwMTg5M30.'

async function run() {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  await page.goto('http://localhost:5182/login')
  await page.evaluate((t) => { localStorage.setItem('token', t) }, TOKEN)
  await page.goto('http://localhost:5182/prescriptions')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(3000)
  await page.screenshot({ path: '/tmp/debug-prescription.png', fullPage: true })
  console.log('screenshot saved to /tmp/debug-prescription.png')
  const html = await page.content()
  console.log(html.includes('新增处方') ? 'found 新增处方' : 'NOT found 新增处方')
  console.log(html.includes('处方管理') ? 'found 处方管理' : 'NOT found 处方管理')
  await browser.close()
}
run().catch(e => { console.error(e); process.exit(1) })
