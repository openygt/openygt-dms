import { chromium } from './node_modules/playwright/index.mjs';

const BASE = 'http://localhost:5172';
const API = 'http://localhost:9092';

async function main() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  
  const loginRes = await context.request.post(`${API}/api/v1/rbac/auth/login`, {
    data: { username: 'admin', password: 'admin123' }
  });
  const loginData = await loginRes.json();
  const token = loginData.data.token;
  console.log(`[LOGIN] token: ${token ? 'OK' : 'FAIL'}`);
  
  const page = await context.newPage();
  await page.goto(BASE + '/#/login');
  await page.evaluate((t) => { localStorage.setItem('token', t); }, token);
  
  const pages = [
    { name: '标准药材主数据', url: '/#/medicines' },
    { name: '科室管理', url: '/#/base/department' },
    { name: '医师管理', url: '/#/base/doctor' },
    { name: '药材目录', url: '/#/base/medicine-catalog' },
  ];
  
  for (const p of pages) {
    console.log(`\n=== ${p.name} (${p.url}) ===`);
    try {
      await page.goto(BASE + p.url, { timeout: 15000, waitUntil: 'networkidle' });
      await page.waitForTimeout(2000);
      const bodyText = await page.textContent('body');
      const hasContent = bodyText && bodyText.length > 100;
      const hasTable = await page.$('table, .el-table');
      const hasRows = await page.$$eval('tr, .el-table__row', els => els.length);
      console.log(`  Content: ${hasContent ? 'YES' : 'NO'}`);
      console.log(`  Table: ${!!hasTable ? 'YES' : 'NO'}`);
      console.log(`  Data rows: ${hasRows}`);
      await page.screenshot({ path: `/tmp/ui-${p.name}.png`, fullPage: true });
      console.log(`  Screenshot: saved`);
    } catch (e) {
      console.log(`  ERROR: ${e.message}`);
    }
  }
  
  await browser.close();
  console.log('\nDone.');
}
main().catch(e => { console.error(e); process.exit(1); });
