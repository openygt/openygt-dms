const { chromium } = require('playwright-core');

const BASE = 'http://127.0.0.1:5174';
const DIR = './test-results';
const fs = require('fs');
if (!fs.existsSync(DIR)) fs.mkdirSync(DIR, { recursive: true });

const TEST_USER = 'testuser_' + Date.now();
const TEST_NAME = '测试用户' + Date.now();
const TEST_PHONE = '138' + String(Math.random()).slice(2, 10);

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });

  const results = [];
  const fail = (step, reason) => {
    console.log(`  ❌ ${step}: ${reason}`);
    results.push({ step, status: 'FAIL', reason });
  };
  const pass = (step) => {
    console.log(`  ✅ ${step}`);
    results.push({ step, status: 'PASS' });
  };

  try {
    // 1. 登录
    console.log('=== Step 1: 登录 ===');
    await page.goto(`${BASE}/login`);
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');
    await page.click('button:has-text("登录")');
    await page.waitForURL(/\/(dashboard|traces)/, { timeout: 15000 }).catch(() => {});
    await page.waitForTimeout(1000);
    const url = page.url();
    if (url.includes('login')) { fail('登录', '登录后仍在登录页'); }
    else { pass('登录'); }

    // 2. 进入用户管理
    console.log('\n=== Step 2: 进入用户管理 ===');
    await page.goto(`${BASE}/sys/users`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    await page.screenshot({ path: `${DIR}/crud-01-users-list.png` });
    const title = await page.locator('.page-header-title').textContent().catch(() => '');
    if (title.includes('用户管理')) { pass('进入用户管理'); }
    else { fail('进入用户管理', `标题为: ${title}`); }

    // 记录当前行数
    const beforeRows = await page.locator('.el-table__row').count();
    console.log(`  当前表格行数: ${beforeRows}`);

    // 3. 点击新增用户
    console.log('\n=== Step 3: 点击新增用户 ===');
    const addBtn = page.locator('button:has-text("新增用户")').first();
    if (!await addBtn.isVisible().catch(() => false)) {
      fail('点击新增用户', '新增用户按钮不可见');
    } else {
      await addBtn.click();
      await page.waitForTimeout(1000);
      const dialog = page.locator('.el-dialog').first();
      const dialogVisible = await dialog.isVisible().catch(() => false);
      if (dialogVisible) { pass('点击新增用户（弹窗出现）'); }
      else { fail('点击新增用户', '弹窗未出现'); }
    }

    // 4. 填写表单
    console.log('\n=== Step 4: 填写新增表单 ===');
    await page.screenshot({ path: `${DIR}/crud-02-add-dialog.png` });

    // 探测表单字段
    const inputs = await page.locator('.el-dialog input').all();
    console.log(`  弹窗内输入框数量: ${inputs.length}`);
    for (let i = 0; i < inputs.length; i++) {
      const placeholder = await inputs[i].getAttribute('placeholder').catch(() => '无placeholder');
      console.log(`    输入框${i}: placeholder=${placeholder}`);
    }

    // 尝试填写
    try {
      // 用户名
      const userInput = page.locator('.el-dialog input[placeholder*="用户名"], .el-dialog input').first();
      await userInput.fill(TEST_USER);
      // 姓名
      const nameInputs = await page.locator('.el-dialog input').all();
      if (nameInputs.length >= 2) await nameInputs[1].fill(TEST_NAME);
      // 手机号
      if (nameInputs.length >= 3) await nameInputs[2].fill(TEST_PHONE);
      pass('填写新增表单');
    } catch (e) {
      fail('填写新增表单', e.message);
    }

    // 5. 点击确定
    console.log('\n=== Step 5: 点击确定提交 ===');
    const confirmBtn = page.locator('.el-dialog button:has-text("确定"), .el-dialog button:has-text("保存"), .el-dialog .el-button--primary').first();
    if (!await confirmBtn.isVisible().catch(() => false)) {
      fail('点击确定', '确定按钮不可见');
    } else {
      await confirmBtn.click();
      await page.waitForTimeout(2500);
      await page.screenshot({ path: `${DIR}/crud-03-after-add.png` });

      // 检查是否有错误提示
      const bodyText = await page.locator('body').textContent();
      const hasError = bodyText.includes('错误') || bodyText.includes('失败') || bodyText.includes('500') || bodyText.includes('系统异常');
      const hasSuccess = bodyText.includes('成功') || bodyText.includes('添加成功');

      if (hasError) {
        fail('提交新增', '页面显示错误提示');
      } else if (hasSuccess) {
        pass('提交新增（显示成功）');
      } else {
        pass('提交新增（无错误提示）');
      }
    }

    // 6. 验证表格中出现新用户
    console.log('\n=== Step 6: 验证新增结果 ===');
    await page.waitForTimeout(1000);
    const afterRows = await page.locator('.el-table__row').count();
    console.log(`  提交后表格行数: ${afterRows}`);
    const hasNewUser = await page.locator('.el-table__row').filter({ hasText: TEST_USER }).count() > 0;
    if (hasNewUser) { pass(`验证新增结果（找到 ${TEST_USER}）`); }
    else { fail('验证新增结果', `表格中未找到 ${TEST_USER}，行数 ${beforeRows}→${afterRows}`); }

    // 7. 点击编辑
    console.log('\n=== Step 7: 点击编辑 ===');
    const newRow = page.locator('.el-table__row').filter({ hasText: TEST_USER }).first();
    const editBtn = newRow.locator('button:has-text("编辑"), .el-button:has-text("编辑")').first();
    if (!await editBtn.isVisible().catch(() => false)) {
      fail('点击编辑', '编辑按钮不可见');
    } else {
      await editBtn.click();
      await page.waitForTimeout(1000);
      const editDialog = page.locator('.el-dialog').first();
      if (await editDialog.isVisible().catch(() => false)) { pass('点击编辑（弹窗出现）'); }
      else { fail('点击编辑', '编辑弹窗未出现'); }
    }

    // 8. 修改数据并提交
    console.log('\n=== Step 8: 修改并提交 ===');
    await page.screenshot({ path: `${DIR}/crud-04-edit-dialog.png` });
    const newName = TEST_NAME + '_修改';
    try {
      const editInputs = await page.locator('.el-dialog input').all();
      if (editInputs.length >= 2) {
        await editInputs[1].fill('');
        await editInputs[1].fill(newName);
      }
      const saveBtn = page.locator('.el-dialog button:has-text("确定"), .el-dialog .el-button--primary').first();
      await saveBtn.click();
      await page.waitForTimeout(2500);
      await page.screenshot({ path: `${DIR}/crud-05-after-edit.png` });

      const bodyText2 = await page.locator('body').textContent();
      const hasErr2 = bodyText2.includes('错误') || bodyText2.includes('失败') || bodyText2.includes('500');
      if (hasErr2) { fail('提交编辑', '页面显示错误'); }
      else { pass('提交编辑'); }
    } catch (e) {
      fail('修改并提交', e.message);
    }

    // 9. 验证修改结果
    console.log('\n=== Step 9: 验证修改结果 ===');
    const hasEdited = await page.locator('.el-table__row').filter({ hasText: newName }).count() > 0;
    if (hasEdited) { pass(`验证修改结果（找到 ${newName}）`); }
    else { fail('验证修改结果', `表格中未找到修改后的名称 ${newName}`); }

    // 10. 点击删除
    console.log('\n=== Step 10: 点击删除 ===');
    const editedRow = page.locator('.el-table__row').filter({ hasText: newName }).first();
    const delBtn = editedRow.locator('button:has-text("删除"), .el-button:has-text("删除")').first();
    if (!await delBtn.isVisible().catch(() => false)) {
      fail('点击删除', '删除按钮不可见');
    } else {
      await delBtn.click();
      await page.waitForTimeout(800);
      // 确认删除弹窗
      const confirmDel = page.locator('.el-message-box__btns button:has-text("确定"), .el-message-box .el-button--primary').first();
      if (await confirmDel.isVisible().catch(() => false)) {
        await confirmDel.click();
        await page.waitForTimeout(2000);
        await page.screenshot({ path: `${DIR}/crud-06-after-delete.png` });
        pass('点击删除并确认');
      } else {
        fail('点击删除', '未出现确认弹窗');
      }
    }

    // 11. 验证删除结果
    console.log('\n=== Step 11: 验证删除结果 ===');
    const hasDeleted = await page.locator('.el-table__row').filter({ hasText: TEST_USER }).count() > 0;
    const finalRows = await page.locator('.el-table__row').count();
    console.log(`  删除后表格行数: ${finalRows}`);
    if (!hasDeleted) { pass('验证删除结果（已删除）'); }
    else { fail('验证删除结果', `表格中仍存在 ${TEST_USER}`); }

  } catch (e) {
    console.log(`\n❌ 测试异常中断: ${e.message}`);
    await page.screenshot({ path: `${DIR}/crud-error.png` });
  }

  await browser.close();

  // 汇总
  console.log('\n========== CRUD测试结果汇总 ==========');
  const passCount = results.filter(r => r.status === 'PASS').length;
  const failCount = results.filter(r => r.status === 'FAIL').length;
  for (const r of results) {
    const icon = r.status === 'PASS' ? '✅' : '❌';
    console.log(`${icon} ${r.step}${r.reason ? ' → ' + r.reason : ''}`);
  }
  console.log(`\n通过: ${passCount} / 失败: ${failCount} / 总计: ${results.length}`);

})();
