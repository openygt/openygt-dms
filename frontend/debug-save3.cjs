const { chromium } = require('playwright')
const TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5IiwidXNlcm5hbWUiOiJhZG1pbiIsImlhdCI6MTc3NzcxNjI4NywiZXhwIjoxNzc3ODAyNjg3LCJyb2xlcyI6WyJST0xFX0FETUlOIl0sInBlcm1pc3Npb25zIjpbIlJPTEVfQURNSU4iXX0.K200xCd6ak5aqd_Cu5TgbfzHM5orvj4pNvvQTlZWUvY'
async function run() {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  page.on('response', async res => {
    if (res.url().includes('/prescriptions')) {
      const status = res.status()
      const url = res.url()
      if (status >= 400) {
        const text = await res.text().catch(() => '')
        console.log('ERROR RESPONSE', status, url, text.substring(0, 200))
      } else {
        console.log('OK RESPONSE', status, url.substring(url.length - 40))
      }
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
  await page.waitForTimeout(4000)
  await page.screenshot({ path: '/tmp/debug-save3.png', fullPage: false })
  const msg = await page.locator('.el-message').allTextContents()
  console.log('messages:', msg)
  console.log('row count:', await page.locator('.el-table__row').count())
  console.log('table text:', await page.locator('.el-table').first().textContent())
  await browser.close()
}
run().catch(e => { console.error(e); process.exit(1) })
