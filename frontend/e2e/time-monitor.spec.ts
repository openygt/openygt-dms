/**
 * 时效监控 E2E — 登录后抓取关键界面证据（全页 PNG）
 *
 * 运行前：启动后端 + 前端（默认 http://localhost:5172），并设置账号环境变量。
 *   PLAYWRIGHT_BASE_URL=http://localhost:5172 E2E_USER=admin E2E_PASSWORD=admin123 npm run test:e2e:time-monitor
 */
import { test, expect } from '@playwright/test'

const user = process.env.E2E_USER ?? 'admin'
const password = process.env.E2E_PASSWORD ?? 'admin123'

async function login(page: import('@playwright/test').Page) {
  await page.goto('/login')
  await page.getByPlaceholder('用户名').fill(user)
  await page.getByPlaceholder('密码').fill(password)
  await page.getByRole('button', { name: '登录' }).click()
  await page.locator('.layout-container').waitFor({ state: 'visible', timeout: 30_000 })
}

test.describe('时效监控 · 抓屏证据', () => {
  test('TM-E2E-001 登录并打开时效监控总览', async ({ page }, testInfo) => {
    await login(page)
    await page.goto('/time-monitor')
    await expect(page.getByText('时效监控', { exact: false }).first()).toBeVisible({ timeout: 20_000 })
    await expect(page.getByText('正常任务')).toBeVisible()
    await expect(page.getByText('活动预警')).toBeVisible()
    await page.screenshot({
      path: testInfo.outputPath('TM-E2E-001-overview.png'),
      fullPage: true,
    })
  })

  test('TM-E2E-002 切换「超时任务」分类', async ({ page }, testInfo) => {
    await login(page)
    await page.goto('/time-monitor')
    await expect(page.locator('.stat-timeout')).toBeVisible({ timeout: 20_000 })
    await page.locator('.stat-timeout').click()
    await expect(page.getByText(/监控明细（超时任务）/)).toBeVisible({ timeout: 10_000 })
    await page.screenshot({
      path: testInfo.outputPath('TM-E2E-002-category-timeout.png'),
      fullPage: true,
    })
  })

  test('TM-E2E-003 查看全部 + 规则区 + 新增规则弹窗', async ({ page }, testInfo) => {
    await login(page)
    await page.goto('/time-monitor')
    await page.locator('.stat-timeout').click()
    await page.getByRole('button', { name: '查看全部' }).click()
    await expect(page.getByText(/监控明细（全部）/)).toBeVisible({ timeout: 10_000 })
    await page.screenshot({
      path: testInfo.outputPath('TM-E2E-003-all-category.png'),
      fullPage: true,
    })

    await page.getByRole('button', { name: '新增规则' }).click()
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 10_000 })
    await page.screenshot({
      path: testInfo.outputPath('TM-E2E-006-rule-dialog.png'),
      fullPage: true,
    })
    await page.getByRole('button', { name: '取消' }).click()
  })

  test('TM-E2E-007 点击刷新', async ({ page }, testInfo) => {
    await login(page)
    await page.goto('/time-monitor')
    await page.getByRole('button', { name: '刷新' }).click()
    await page.waitForTimeout(800)
    await expect(page.getByText('时效监控', { exact: false }).first()).toBeVisible()
    await page.screenshot({
      path: testInfo.outputPath('TM-E2E-007-after-refresh.png'),
      fullPage: true,
    })
  })
})
