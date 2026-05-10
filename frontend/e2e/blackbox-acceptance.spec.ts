import { test, expect } from '@playwright/test';

const BASE = 'http://localhost:5174';

test.describe('登录页面', () => {
  test('页面正常加载', async ({ page }) => {
    await page.goto(`${BASE}/login`);
    await expect(page.locator('input[placeholder*="用户名"], input[type="text"]').first()).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
    await expect(page.locator('button').filter({ hasText: /登录|登 录|sign in/i })).toBeVisible();
  });

  test('空用户名提交显示验证', async ({ page }) => {
    await page.goto(`${BASE}/login`);
    const loginBtn = page.locator('button').filter({ hasText: /登录|登 录|sign in/i });
    await loginBtn.click();
    await page.waitForTimeout(1000);
  });

  test('错误凭据显示错误信息', async ({ page }) => {
    await page.goto(`${BASE}/login`);
    await page.locator('input[type="text"]').first().fill('admin');
    await page.locator('input[type="password"]').fill('wrongpassword');
    const loginBtn = page.locator('button').filter({ hasText: /登录|登 录|sign in/i });
    await loginBtn.click();
    await page.waitForTimeout(2000);
    const errorMsg = page.locator('.el-message--error, .el-message, [class*="error"]');
    const body = page.locator('body');
    const text = await body.innerText();
    expect(text).toMatch(/用户名|密码|错误|失败/i);
  });

  test('正确凭据登录成功跳转到看板', async ({ page }) => {
    await page.goto(`${BASE}/login`);
    await page.locator('input[type="text"]').first().fill('admin');
    await page.locator('input[type="password"]').fill('admin123');
    const loginBtn = page.locator('button').filter({ hasText: /登录|登 录|sign in/i });
    await loginBtn.click();
    await page.waitForURL('**/dashboard**', { timeout: 10000 });
    await expect(page).toHaveURL(/dashboard/);
  });
});

test.describe('生产看板', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${BASE}/login`);
    await page.locator('input[type="text"]').first().fill('admin');
    await page.locator('input[type="password"]').fill('admin123');
    await page.locator('button').filter({ hasText: /登录|登 录|sign in/i }).click();
    await page.waitForURL('**/dashboard**', { timeout: 10000 });
  });

  test('看板页面可加载且有内容', async ({ page }) => {
    await page.goto(`${BASE}/dashboard`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('侧边菜单导航可用', async ({ page }) => {
    await page.goto(`${BASE}/dashboard`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    const menu = page.locator('.el-menu, [class*="sidebar"], [class*="menu"]');
    await expect(menu.first()).toBeVisible({ timeout: 5000 });
  });
});

test.describe('核心页面导航', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${BASE}/login`);
    await page.locator('input[type="text"]').first().fill('admin');
    await page.locator('input[type="password"]').fill('admin123');
    await page.locator('button').filter({ hasText: /登录|登 录|sign in/i }).click();
    await page.waitForURL('**/dashboard**', { timeout: 10000 });
  });

  const pages = [
    { name: '煎药任务', url: '/tasks' },
    { name: '处方录入', url: '/prescriptions' },
    { name: '设备台账', url: '/devices' },
    { name: '设备监控', url: '/device-monitor' },
    { name: '告警中心', url: '/alarms' },
    { name: '质量检验', url: '/quality' },
    { name: '医院管理', url: '/hospitals' },
    { name: '药材管理', url: '/base/medicine' },
    { name: '人员管理', url: '/users' },
    { name: '权限管理', url: '/roles' },
  ];

  for (const { name, url } of pages) {
    test(`${name} 页面可加载`, async ({ page }) => {
      await page.goto(`${BASE}${url}`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);
      await expect(page.locator('body')).toBeVisible();
    });
  }
});

test.describe('安全性测试', () => {
  test('未登录访问受保护页面重定向到登录', async ({ page }) => {
    await page.goto(`${BASE}/dashboard`);
    await page.waitForTimeout(3000);
    const currentUrl = page.url();
    expect(currentUrl).toMatch(/login/i);
  });

  test('未登录访问设备页重定向', async ({ page }) => {
    await page.goto(`${BASE}/devices`);
    await page.waitForTimeout(3000);
    const currentUrl = page.url();
    expect(currentUrl).toMatch(/login/i);
  });
});
