const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 900 } });
  
  await page.goto('http://localhost:5174/prescriptions', { waitUntil: 'networkidle' });
  await page.fill('input[type="text"]', 'admin');
  await page.fill('input[type="password"]', 'admin123');
  await page.click('button:has-text("登录")');
  await page.waitForURL('**/prescriptions', { timeout: 5000 });
  await page.waitForTimeout(2000);
  
  // 1. 先看列表
  await page.screenshot({ path: '/tmp/27-list-after-all-fixes.png', fullPage: true });
  
  // 2. 点击新增处方
  await page.click('button:has-text("新增处方")');
  await page.waitForTimeout(2000);
  
  // 3. 用JavaScript读取hospitals数组长度
  const hospitalsCount = await page.evaluate(() => {
    const vm = document.querySelector('.el-dialog')?.__vueParentComponent?.ctx || {};
    return vm.hospitals?.length || 'unknown';
  });
  console.log(' hospitals数组长度:', hospitalsCount);
  
  // 4. 通过input事件触发医院select
  const hospitalInput = await page.locator('.el-dialog .el-form-item:has-text("医院") input').first();
  await hospitalInput.click();
  await page.waitForTimeout(1500);
  
  // 获取当前激活的下拉框选项（通过可见性）
  const visibleOptions = await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').allInnerTexts();
  console.log('可见选项:', visibleOptions.slice(0, 10));
  
  await page.screenshot({ path: '/tmp/28-dialog-hospital-test.png', fullPage: true });
  await browser.close();
})();
