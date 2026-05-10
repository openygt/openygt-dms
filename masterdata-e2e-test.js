const { chromium } = require('playwright-core');
const mysql = require('mysql2/promise');

const BASE = 'http://127.0.0.1:5174';
const DIR = './test-results';
const fs = require('fs');
if (!fs.existsSync(DIR)) fs.mkdirSync(DIR, { recursive: true });

async function dbQuery(sql) {
  const conn = await mysql.createConnection({ host: '127.0.0.1', port: 3306, user: 'root', password: 'mysql_pwd01', database: 'openygt_dms_clean' });
  const [rows] = await conn.execute(sql);
  await conn.end();
  return rows;
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });

  const results = [];
  const log = (step, ok, detail) => {
    const icon = ok ? '✅' : '❌';
    console.log(`  ${icon} ${step}${detail ? ' → ' + detail : ''}`);
    results.push({ step, ok, detail });
  };

  // 登录
  console.log('=== 登录 ===');
  await page.goto(`${BASE}/login`);
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登录")');
  await page.waitForURL(/\/(dashboard|traces)/, { timeout: 15000 }).catch(() => {});
  await page.waitForTimeout(1500);
  log('登录', !page.url().includes('login'), page.url());

  // 测试页面列表
  const menus = [
    { name: '医院管理', path: '/hospitals', table: 'md_hospital' },
    { name: '科室管理', path: '/base/department', table: 'md_department' },
    { name: '医师管理', path: '/base/doctor', table: 'md_doctor' },
    { name: '药材管理', path: '/base/medicine', table: 'md_medicine' },
    { name: '煎药方案', path: '/schemes', table: 'md_decoct_scheme' },
    { name: '毒性药材', path: '/toxic-medicine', table: 'md_toxic_medicine' },
  ];

  for (const menu of menus) {
    console.log(`\n=== ${menu.name} (${menu.path}) ===`);
    try {
      // 访问页面
      await page.goto(`${BASE}${menu.path}`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);
      await page.screenshot({ path: `${DIR}/md-${menu.path.replace(/\//g, '-')}.png`, fullPage: true });

      const bodyText = await page.locator('body').textContent();
      const has500 = bodyText.includes('500') || bodyText.includes('Internal Server Error') || bodyText.includes('系统错误');
      const title = await page.locator('.page-header-title').textContent().catch(() => '');

      if (has500) {
        log(`${menu.name} 页面加载`, false, '出现500错误');
        continue;
      }

      // 数据库数据量校验
      const dbRows = await dbQuery(`SELECT COUNT(*) as cnt FROM ${menu.table}`);
      const dbCount = dbRows[0].cnt;

      // 页面表格行数
      const tableRows = await page.locator('.el-table__row').count();

      log(`${menu.name} 页面加载`, true, `标题:"${title}" 数据库:${dbCount}条 表格:${tableRows}行`);

      // 如果数据库有数据但表格没显示，报问题
      if (dbCount > 0 && tableRows === 0) {
        log(`${menu.name} 数据展示`, false, `数据库有${dbCount}条但表格显示0行`);
      }

    } catch (e) {
      log(`${menu.name}`, false, e.message);
    }
  }

  // 对毒性药材页面做完整CRUD（数据少，适合测试）
  console.log('\n=== 毒性药材 CRUD（带数据库校验）===');
  try {
    await page.goto(`${BASE}/toxic-medicine`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // 记录新增前数据量
    const beforeAdd = (await dbQuery('SELECT COUNT(*) as cnt FROM md_toxic_medicine WHERE deleted=0'))[0].cnt;
    console.log(`  新增前数据库: ${beforeAdd}条`);

    // 点击新增
    const addBtn = page.locator('button:has-text("新增"), button:has-text("新增毒性药材")').first();
    if (await addBtn.isVisible().catch(() => false)) {
      await addBtn.click();
      await page.waitForTimeout(1000);
      await page.screenshot({ path: `${DIR}/md-toxic-add-dialog.png` });

      // 填写表单
      const inputs = await page.locator('.el-dialog input, .el-dialog .el-input__inner').all();
      console.log(`  弹窗输入框数: ${inputs.length}`);

      if (inputs.length >= 3) {
        await inputs[0].fill('999');
        await inputs[1].fill('测试毒性药材CRUD');
        if (inputs[2]) await inputs[2].fill('5.000');

        // 点击确定/保存
        const confirm = page.locator('.el-dialog button:has-text("确定"), .el-dialog button:has-text("保存"), .el-dialog .el-button--primary').first();
        await confirm.click();
        await page.waitForTimeout(2500);
        await page.screenshot({ path: `${DIR}/md-toxic-after-add.png` });

        // 数据库校验
        const afterAdd = (await dbQuery('SELECT COUNT(*) as cnt FROM md_toxic_medicine WHERE deleted=0'))[0].cnt;
        console.log(`  新增后数据库: ${afterAdd}条`);

        if (afterAdd === beforeAdd + 1) {
          log('毒性药材新增', true, `数据库 ${beforeAdd}→${afterAdd}`);
        } else {
          log('毒性药材新增', false, `数据库仍是${afterAdd}条，未增加`);
        }

        // 页面表格校验
        const hasNew = await page.locator('.el-table__row').filter({ hasText: '测试毒性药材CRUD' }).count() > 0;
        log('毒性药材表格展示', hasNew, hasNew ? '表格中出现新数据' : '表格中未出现');
      } else {
        log('毒性药材新增', false, `输入框不足(${inputs.length}个)，无法填表`);
      }
    } else {
      log('毒性药材新增', false, '新增按钮不可见');
    }
  } catch (e) {
    log('毒性药材CRUD', false, e.message);
  }

  await browser.close();

  // 汇总
  console.log('\n========== masterdata测试结果 ==========');
  const pass = results.filter(r => r.ok).length;
  const fail = results.filter(r => !r.ok).length;
  for (const r of results) {
    const icon = r.ok ? '✅' : '❌';
    console.log(`${icon} ${r.step}${r.detail ? ' | ' + r.detail : ''}`);
  }
  console.log(`\n通过: ${pass} / 失败: ${fail} / 总计: ${results.length}`);

})();
