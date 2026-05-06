import { chromium } from 'playwright';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  await page.goto('http://127.0.0.1:5172/login');
  await page.waitForTimeout(2000);
  await page.fill('input[placeholder="请输入用户名"], input[type="text"]', 'admin');
  await page.fill('input[placeholder="请输入密码"], input[type="password"]', 'admin123');
  await page.click('button:has-text("登录"), button[type="submit"]');
  await page.waitForTimeout(3000);

  // Click 生产指挥 to expand submenu
  const prodMenu = await page.locator('.el-sub-menu__title:has-text("生产指挥"), .el-menu-item:has-text("生产指挥")').first();
  await prodMenu.click();
  await page.waitForTimeout(1500);

  // Click 生产记录
  await page.locator('.el-menu-item:has-text("生产记录")').first().click();
  await page.waitForTimeout(3000);

  await page.screenshot({ path: '/data2/docker/decoction/openygt-dms-docs/docs/20260505/01bug/screenshots/19-生产记录.png', fullPage: false });

  // Capture API response
  const apiRes = await page.evaluate(async () => {
    try {
      const token = localStorage.getItem('token') || '';
      const res = await fetch('/api/v1/production/record?page=1&size=10', {
        headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' }
      });
      return await res.json();
    } catch (e) {
      return { error: e.message };
    }
  });
  console.log('production/record API:', JSON.stringify(apiRes, null, 2));

  await browser.close();
})();
