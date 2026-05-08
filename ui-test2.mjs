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
  
  const page = await context.newPage();
  
  // Collect console errors
  const errors = [];
  page.on('console', msg => { if (msg.type() === 'error') errors.push(msg.text()); });
  page.on('pageerror', err => errors.push(err.message));
  
  await page.goto(BASE + '/#/login');
  await page.evaluate((t) => { localStorage.setItem('token', t); }, token);
  
  // Test each page and capture text content + errors
  const pages = [
    { name: '标准药材主数据', url: '/#/medicines' },
    { name: '科室管理', url: '/#/base/department' },
  ];
  
  for (const p of pages) {
    console.log(`\n=== ${p.name} (${p.url}) ===`);
    errors.length = 0;
    try {
      await page.goto(BASE + p.url, { timeout: 15000, waitUntil: 'networkidle' });
      await page.waitForTimeout(3000);
      
      // Get visible text content
      const visibleText = await page.evaluate(() => {
        return document.body.innerText.substring(0, 500);
      });
      console.log(`  Visible text: ${visibleText.substring(0, 300)}`);
      console.log(`  Console errors: ${errors.length > 0 ? errors.slice(0,3).join('; ') : 'none'}`);
    } catch (e) {
      console.log(`  ERROR: ${e.message}`);
    }
  }
  
  await browser.close();
}
main().catch(e => { console.error(e); process.exit(1); });
