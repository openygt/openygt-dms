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

  const shots = [
    { name: '20-设备监控', path: '/device-monitor' },
    { name: '21-远程操控', path: '/device-command' },
    { name: '22-设备联网', path: '/device-network' },
    { name: '23-分组配对', path: '/device-group-manage' },
  ];

  for (const s of shots) {
    try {
      await page.goto(`http://127.0.0.1:5172${s.path}`);
      await page.waitForTimeout(3000);
      await page.screenshot({ path: `/data2/docker/decoction/openygt-dms-docs/docs/20260505/01bug/screenshots/${s.name}.png`, fullPage: false });
      console.log(`✅ ${s.name}`);
    } catch (e) {
      console.log(`❌ ${s.name}: ${e.message}`);
    }
  }

  await browser.close();
})();
