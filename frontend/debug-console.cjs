const { chromium } = require('playwright')

async function run() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await context.newPage()
  
  const errors = []
  const consoleLogs = []
  
  page.on('pageerror', err => errors.push('PAGE ERROR: ' + err.message))
  page.on('console', msg => consoleLogs.push(msg.type() + ': ' + msg.text()))
  
  await page.goto('http://localhost:5182/login')
  await page.evaluate(() => {
    localStorage.setItem('token', 'eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJ1c2VySWQiOjEsInN1YiI6ImFkbWluIiwidXNlcm5hbWUiOiJhZG1pbiIsInJvbGVzIjpbIlJPTEVfQURNSU4iXSwicGVybWlzc2lvbnMiOlsiUk9MRV9BRE1JTiJdLCJpYXQiOjE3NzYwMTg5M30.')
  })
  
  await page.goto('http://localhost:5182/prescriptions')
  await page.waitForTimeout(5000)
  
  console.log('=== PAGE ERRORS ===')
  errors.forEach(e => console.log(e))
  console.log('=== CONSOLE LOGS ===')
  consoleLogs.forEach(l => console.log(l))
  
  await browser.close()
}

run().catch(e => { console.error(e); process.exit(1) })
