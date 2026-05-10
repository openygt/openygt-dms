const { chromium } = require('playwright-core');

const BASE = 'http://127.0.0.1:5174';
const SCREENSHOT_DIR = './test-results';

const fs = require('fs');
if (!fs.existsSync(SCREENSHOT_DIR)) fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });

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
  await page.waitForTimeout(2000);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/system-00-login.png`, fullPage: true });
  console.log('登录完成');

  const menus = [
    { name: '用户管理', path: '/sys/users', file: 'system-01-users.png' },
    { name: '角色权限', path: '/roles', file: 'system-02-roles.png' },
    { name: '菜单管理', path: '/menus', file: 'system-03-menus.png' },
    { name: '参数配置', path: '/configs', file: 'system-04-configs.png' },
    { name: '日志管理', path: '/sys/logs', file: 'system-05-logs.png' },
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

      await page.screenshot({ path: `${SCREENSHOT_DIR}/${menu.file}`, fullPage: true });

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

  console.log('\n========== 测试结果汇总 ==========');
  for (const r of results) {
    const icon = r.status === 'PASS' ? '✅' : '❌';
    console.log(`${icon} ${r.name}: ${r.status}${r.reason ? ' (' + r.reason + ')' : ''}`);
  }
  const passCount = results.filter(r => r.status === 'PASS').length;
  console.log(`\n总计: ${passCount}/${results.length} 通过`);

})();
