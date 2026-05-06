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
    { name: '24-生产记录', path: '/task-assignment' },
    { name: '25-产能统计', path: '/capacity' },
    { name: '26-操作日志', path: '/logs' },
    { name: '27-生产设置', path: '/production-setting' },
    { name: '28-分组投料', path: '/herb-group' },
    { name: '29-批次追溯', path: '/trace/batch' },
    { name: '30-异常追溯', path: '/trace/exception' },
    { name: '31-处方接收', path: '/prod/receive' },
    { name: '32-返工处理', path: '/task-rollback' },
    { name: '33-成品暂存', path: '/shelf-manage' },
    { name: '34-进度查询', path: '/patient-query' },
    { name: '35-语音播报', path: '/voice-setting' },
    { name: '36-急诊通道', path: '/emergency' },
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
