const { chromium } = require('playwright-core');

const BASE = 'http://127.0.0.1:5174';
const DIR = './test-results';

const fs = require('fs');
if (!fs.existsSync(DIR)) fs.mkdirSync(DIR, { recursive: true });

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });

  // 登录
  console.log('=== 登录 ===');
  await page.goto(`${BASE}/login`);
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登录")');
  await page.waitForURL(/\/(dashboard|traces)/, { timeout: 15000 }).catch(() => {});
  await page.waitForTimeout(1500);

  const menus = [
    { name: '设备台账', path: '/devices', file: 'eq-01-devices.png' },
    { name: '设备监控', path: '/device-monitor', file: 'eq-02-monitor.png' },
    { name: '告警中心', path: '/alarms', file: 'eq-03-alarms.png' },
    { name: '设备效能', path: '/device-utilization', file: 'eq-04-utilization.png' },
    { name: '告警配置', path: '/alarm-configs', file: 'eq-05-alarm-configs.png' },
    { name: '远程操控', path: '/device-command', file: 'eq-06-command.png' },
    { name: '设备维保', path: '/device-maintenance', file: 'eq-07-maintenance.png' },
  ];

  const results = [];

  for (const menu of menus) {
    console.log(`\n=== ${menu.name} (${menu.path}) ===`);
    try {
      await page.goto(`${BASE}${menu.path}`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2500);

      const bodyText = await page.locator('body').textContent();
      const has500 = bodyText.includes('500') || bodyText.includes('Internal Server Error') || bodyText.includes('系统错误');

      await page.screenshot({ path: `${DIR}/${menu.file}`, fullPage: true });

      const title = await page.locator('.page-header-title').textContent().catch(() => '');
      console.log(`  标题: ${title || '(未识别)'}`);
      console.log(`  截图: ${menu.file}`);

      if (has500) {
        console.log(`  ❌ 发现500错误`);
        results.push({ name: menu.name, status: 'FAIL', reason: '500错误' });
      } else {
        console.log(`  ✅ 正常`);
        results.push({ name: menu.name, status: 'PASS' });
      }
    } catch (e) {
      console.log(`  ❌ 异常: ${e.message}`);
      results.push({ name: menu.name, status: 'FAIL', reason: e.message });
    }
  }

  await browser.close();

  console.log('\n========== E2E测试结果汇总 ==========');
  for (const r of results) {
    const icon = r.status === 'PASS' ? '✅' : '❌';
    console.log(`${icon} ${r.name}: ${r.status}${r.reason ? ' (' + r.reason + ')' : ''}`);
  }
  const passCount = results.filter(r => r.status === 'PASS').length;
  console.log(`\n总计: ${passCount}/${results.length} 通过`);

})();
