/**
 * 时效监控 E2E — 登录后抓取关键界面证据（全页 PNG）
 *
 * openygt-dms-01：前端 5171，后端 9091。仓库内 vite 代理可能仍指向 9092，故默认走「直连 9091 换 token + 浏览器只跑页面」。
 *
 *   PLAYWRIGHT_BASE_URL=http://127.0.0.1:5171 \
 *   E2E_API_BASE=http://127.0.0.1:9091 \
 *   E2E_USER=admin E2E_PASSWORD=admin123 \
 *   npm run test:e2e:time-monitor
 */
import { test, expect } from '@playwright/test'

const user = process.env.E2E_USER ?? 'admin'
const password = process.env.E2E_PASSWORD ?? 'admin123'
/** 与 PLAYWRIGHT_BASE_URL 对应的后端（01→9091），避免 Vite 代理错端口导致 UI 登录失败 */
const apiBase = (process.env.E2E_API_BASE ?? 'http://127.0.0.1:9091').replace(/\/$/, '')

async function loginWithToken(page: import('@playwright/test').Page, request: import('@playwright/test').APIRequestContext) {
  const res = await request.post(`${apiBase}/api/v1/rbac/auth/login`, {
    data: { username: user, password },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(res.ok(), `登录接口失败 HTTP ${res.status()}，请检查 E2E_API_BASE=${apiBase}`).toBeTruthy()
  const body = await res.json()
  const token = body?.data?.token as string | undefined
  expect(token, '响应中无 token').toBeTruthy()
  await page.goto('/login')
  await page.evaluate((t) => localStorage.setItem('token', t), token!)
}

test.describe('时效监控 · 抓屏证据', () => {
  test('TM-E2E-001 登录并打开时效监控总览', async ({ page, request }, testInfo) => {
    await loginWithToken(page, request)
    await page.goto('/time-monitor')
    await expect(page.getByText('时效监控', { exact: false }).first()).toBeVisible({ timeout: 20_000 })
    await expect(page.getByText('正常任务')).toBeVisible()
    await expect(page.getByText('活动预警')).toBeVisible()
    await page.screenshot({
      path: testInfo.outputPath('TM-E2E-001-overview.png'),
      fullPage: true,
    })
  })

  test('TM-E2E-002 切换「超时任务」分类', async ({ page, request }, testInfo) => {
    await loginWithToken(page, request)
    await page.goto('/time-monitor')
    await expect(page.locator('.stat-timeout')).toBeVisible({ timeout: 20_000 })
    await page.locator('.stat-timeout').click()
    await expect(page.getByText(/监控明细（超时任务）/)).toBeVisible({ timeout: 10_000 })
    await page.screenshot({
      path: testInfo.outputPath('TM-E2E-002-category-timeout.png'),
      fullPage: true,
    })
  })

  test('TM-E2E-003 查看全部 + 规则区 + 新增规则弹窗', async ({ page, request }, testInfo) => {
    await loginWithToken(page, request)
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

  test('TM-E2E-007 点击刷新', async ({ page, request }, testInfo) => {
    await loginWithToken(page, request)
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
