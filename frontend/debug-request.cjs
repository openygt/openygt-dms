const { chromium } = require('playwright')
const TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5IiwidXNlcm5hbWUiOiJhZG1pbiIsImlhdCI6MTc3NzcxNjI4NywiZXhwIjoxNzc3ODAyNjg3LCJyb2xlcyI6WyJST0xFX0FETUlOIl0sInBlcm1pc3Npb25zIjpbIlJPTEVfQURNSU4iXX0.K200xCd6ak5aqd_Cu5TgbfzHM5orvj4pNvvQTlZWUvY'
async function run() {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  page.on('request', req => {
    if (req.url().includes('prescriptions/structured')) {
      console.log('REQUEST URL:', req.url())
      console.log('REQUEST BODY:', req.postData())
    }
  })
  page.on('response', async res => {
    if (res.url().includes('prescriptions/structured')) {
      const t = await res.text()
      console.log('RESPONSE:', t.substring(0, 500))
    }
  })
  await page.goto('http://localhost:5182/login')
  await page.evaluate((t) => { localStorage.setItem('token', t) }, TOKEN)
  await page.goto('http://localhost:5182/prescriptions')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)
  await page.getByRole('button', { name: '新增处方' }).click()
  await page.waitForTimeout(1000)
  const dialog = page.locator('.el-dialog')
  await dialog.locator('input[placeholder*="必填"]').fill('演示患者')
  await page.waitForTimeout(500)
  await dialog.getByRole('button', { name: '+ 添加药材' }).click()
  await page.waitForTimeout(500)
  const rows = dialog.locator('.el-table__body .el-table__row')
  await rows.first().locator('input').first().fill('当归')
  await rows.first().locator('.el-input-number input').fill('15')
  await page.waitForTimeout(500)
  await dialog.locator('.el-dialog__footer').getByRole('button', { name: '保存' }).click()
  await page.waitForTimeout(3000)
  await browser.close()
}
run().catch(e => { console.error(e); process.exit(1) })
