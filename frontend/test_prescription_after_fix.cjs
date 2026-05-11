const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 900 } });
  
  await page.goto('http://localhost:5174', { waitUntil: 'networkidle' });
  await page.fill('input[type="text"]', 'admin');
  await page.fill('input[type="password"]', 'admin123');
  await page.click('button:has-text("登录")');
  await page.waitForURL('**/dashboard', { timeout: 5000 });
  
  await page.click('text=处方管理');
  await page.waitForTimeout(300);
  await page.click('text=处方录入');
  await page.waitForTimeout(1500);
  
  // 点击新增处方
  await page.click('button:has-text("新增处方")');
  await page.waitForTimeout(1500);
  
  // 点击医院下拉框
  const selects = await page.locator('.el-dialog .el-select').all();
  if (selects.length > 0) {
    await selects[0].click();
    await page.waitForTimeout(1000);
    const options = await page.locator('.el-select-dropdown__item').all();
    console.log('医院选项数:', options.length);
    for (const opt of options.slice(0, 6)) {
      const txt = await opt.textContent();
      console.log('  -', txt?.trim());
    }
    // 选第一家
    if (options.length > 0) await options[0].click();
    await page.waitForTimeout(1000);
  }
  
  // 点击医师下拉框看级联是否生效
  if (selects.length > 1) {
    await selects[1].click();
    await page.waitForTimeout(1000);
    const options = await page.locator('.el-select-dropdown__item').all();
    console.log('医师选项数:', options.length);
    for (const opt of options.slice(0, 6)) {
      const txt = await opt.textContent();
      console.log('  -', txt?.trim());
    }
    if (options.length > 0) await options[0].click();
  }
  
  await page.screenshot({ path: '/tmp/24-after-hospital-fix.png', fullPage: true });
  await browser.close();
})();
