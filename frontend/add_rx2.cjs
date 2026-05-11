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
  await page.click('button:has-text("新增处方")');
  await page.waitForTimeout(1500);
  
  // 填写文本字段
  await page.fill('input[placeholder="处方编号"]', 'RX20260510001');
  await page.fill('input[placeholder="必填"]', '王小明');
  
  // 获取所有el-select，按顺序点击（第2个是医院，第3个是医师）
  const selects = await page.locator('.el-dialog .el-select').all();
  console.log('找到select数量:', selects.length);
  
  // 医院（第2个select，索引1）
  if (selects.length > 1) {
    await selects[1].click();
    await page.waitForTimeout(800);
    // 点击第一个可见选项
    const firstOption = await page.locator('.el-select-dropdown__item').first();
    const optText = await firstOption.textContent();
    console.log('医院选项:', optText?.trim());
    await firstOption.click();
    await page.waitForTimeout(500);
  }
  
  // 医师（第3个select，索引2）
  if (selects.length > 2) {
    await selects[2].click();
    await page.waitForTimeout(800);
    const firstOption = await page.locator('.el-select-dropdown__item').first();
    const optText = await firstOption.textContent();
    console.log('医师选项:', optText?.trim());
    await firstOption.click();
    await page.waitForTimeout(500);
  }
  
  await page.screenshot({ path: '/tmp/11-filled-form2.png', fullPage: true });
  
  // 点击保存
  await page.click('button:has-text("保存")');
  await page.waitForTimeout(3000);
  await page.screenshot({ path: '/tmp/12-after-submit2.png', fullPage: true });
  console.log('提交完成');
  
  await browser.close();
})();
