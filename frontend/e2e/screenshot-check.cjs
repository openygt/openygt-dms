const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  const shotsDir = '/data2/docker/decoction/openygt-dms-docs/docs/20260505/01bug/screenshots';

  // 登录
  await page.goto('http://127.0.0.1:5172/login');
  await page.fill('input[placeholder="请输入用户名"], input[type="text"]', 'admin');
  await page.fill('input[placeholder="请输入密码"], input[type="password"]', 'admin123');
  await page.click('button:has-text("登录"), button[type="submit"]');
  await page.waitForTimeout(3000);

  const pages = [
    { name: '01-医院管理', path: '/hospitals' },
    { name: '02-科室管理', path: '/base/department' },
    { name: '03-医师管理', path: '/base/doctor' },
    { name: '04-药材管理', path: '/base/medicine' },
    { name: '05-毒性药材', path: '/toxic-medicine' },
    { name: '06-处方管理', path: '/prescriptions' },
    { name: '07-煎药任务', path: '/tasks' },
    { name: '08-流程跟踪', path: '/step-visualization' },
    { name: '09-生产看板', path: '/dashboard' },
    { name: '10-质量检验', path: '/quality' },
    { name: '11-留样管理', path: '/retain-sample' },
    { name: '12-发药管理', path: '/prod/delivery' },
    { name: '13-设备台账', path: '/devices' },
    { name: '14-告警中心', path: '/alarms' },
    { name: '15-设备维保', path: '/device-maintenance' },
    { name: '16-清洗记录', path: '/wash-record' },
    { name: '17-系统用户', path: '/users' },
    { name: '18-菜单管理', path: '/menus' },
  ];

  for (const p of pages) {
    try {
      await page.goto(`http://127.0.0.1:5172${p.path}`);
      await page.waitForTimeout(2500);
      await page.screenshot({ path: `${shotsDir}/${p.name}.png`, fullPage: false });
      console.log(`✅ ${p.name} — 截图完成`);
    } catch (e) {
      console.log(`❌ ${p.name} — 截图失败: ${e.message}`);
    }
  }

  await browser.close();
  console.log('\n全部截图完成，存放目录: ' + shotsDir);
})();
