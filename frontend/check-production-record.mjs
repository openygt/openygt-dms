import { chromium } from 'playwright';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  // Login
  await page.goto('http://127.0.0.1:5172/login');
  await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 10000 });
  await page.fill('input[placeholder="请输入用户名"]', 'admin');
  await page.fill('input[placeholder="请输入密码"]', 'admin123');
  await page.click('button:has-text("登录")');
  await page.waitForTimeout(3000);

  // Navigate to 生产记录
  await page.click('text=生产指挥');
  await page.waitForTimeout(500);
  await page.click('text=生产记录');
  await page.waitForTimeout(3000);

  await page.screenshot({ path: '/data2/docker/decoction/openygt-dms-docs/docs/20260505/01bug/screenshots/19-生产记录.png', fullPage: false });

  // Also capture API responses
  const apiRes = await page.evaluate(async () => {
    try {
      const res = await fetch('/api/v1/production/record?page=1&size=10', {
        headers: { 'Content-Type': 'application/json' }
      });
      return await res.json();
    } catch (e) {
      return { error: e.message };
    }
  });
  console.log('API response:', JSON.stringify(apiRes, null, 2));

  await browser.close();
})();
