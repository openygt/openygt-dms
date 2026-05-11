const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  
  await page.goto('http://localhost:5174', { waitUntil: 'networkidle' });
  await page.fill('input[type="text"]', 'admin');
  await page.fill('input[type="password"]', 'admin123');
  await page.click('button:has-text("登录")');
  await page.waitForURL('**/dashboard', { timeout: 5000 });
  
  await page.click('text=处方管理');
  await page.waitForTimeout(500);
  await page.click('text=处方录入');
  await page.waitForTimeout(2000);
  await page.click('button:has-text("新增处方")');
  await page.waitForTimeout(2000);
  
  // 点击医院下拉框
  console.log('=== 医院选项 ===');
  await page.click('.el-form-item:has-text("医院") .el-select');
  await page.waitForTimeout(1000);
  const hospitalOptions = await page.$$eval('.el-select-dropdown__item', items => items.map(i => i.textContent.trim()));
  console.log(hospitalOptions);
  await page.keyboard.press('Escape');
  await page.waitForTimeout(500);
  
  // 点击医师下拉框
  console.log('=== 医师选项 ===');
  await page.click('.el-form-item:has-text("医师") .el-select');
  await page.waitForTimeout(1000);
  const doctorOptions = await page.$$eval('.el-select-dropdown__item', items => items.map(i => i.textContent.trim()));
  console.log(doctorOptions);
  await page.keyboard.press('Escape');
  await page.waitForTimeout(500);
  
  // 点击煎煮方案下拉框
  console.log('=== 煎煮方案选项 ===');
  await page.click('.el-form-item:has-text("煎煮方案") .el-select');
  await page.waitForTimeout(1000);
  const schemeOptions = await page.$$eval('.el-select-dropdown__item', items => items.map(i => i.textContent.trim()));
  console.log(schemeOptions);
  await page.keyboard.press('Escape');
  await page.waitForTimeout(500);
  
  // 点击制剂类型下拉框
  console.log('=== 制剂类型选项 ===');
  await page.click('.el-form-item:has-text("制剂类型") .el-select');
  await page.waitForTimeout(1000);
  const typeOptions = await page.$$eval('.el-select-dropdown__item', items => items.map(i => i.textContent.trim()));
  console.log(typeOptions);
  await page.keyboard.press('Escape');
  
  await browser.close();
})();
