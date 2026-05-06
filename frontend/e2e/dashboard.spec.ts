/**
 * 生产看板 — 以界面交互与可感知体验为主的验证（非仅接口契约）
 *
 * 环境：openygt-dms-05 → 前端 5175、后端 9095（见 docs/README.md）
 * 凭据：PW_USER / PW_PASS，勿写入仓库
 */
import { test, expect } from '@playwright/test'

const USER = process.env.PW_USER ?? 'admin'
const PASS = process.env.PW_PASS ?? ''

async function login(page: import('@playwright/test').Page) {
  await page.goto('/login')
  await page.getByPlaceholder('用户名').fill(USER)
  await page.getByPlaceholder('密码').fill(PASS)
  await page.getByRole('button', { name: '登录' }).click()
  await page.waitForURL((u) => !u.pathname.endsWith('/login'), { timeout: 60_000 })
}

test.describe('生产看板 /dashboard', () => {
  test('未登录访问看板会跳到登录页', async ({ page }) => {
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/login/)
  })

  test('登录后：版式、KPI 可读、分区标题与系统信息', async ({ page }) => {
    test.skip(!PASS, '设置 PW_PASS 后运行完整用例')
    test.setTimeout(120_000)

    await login(page)
    await page.goto('/dashboard')

    await expect(page.locator('.page-header-title')).toContainText('生产看板')
    await expect(page.getByRole('button', { name: '刷新' })).toBeVisible()

    const kpiCards = page.locator('.kpi-grid .kpi-card')
    await expect(kpiCards).toHaveCount(4)
    // 每张 KPI 主数字区可见（用户感知「有数」而非空白骨架）
    for (let i = 0; i < 4; i++) {
      const num = kpiCards.nth(i).locator('.ygt-num')
      await expect(num).toBeVisible()
      await expect(num).toHaveText(/\d+/)
    }

    await expect(page.locator('.quick-actions')).toBeVisible()
    await expect(page.locator('.dashboard-card').getByText('任务状态分布', { exact: true })).toBeVisible()
    await expect(page.locator('.dashboard-card').getByText('设备类型分布', { exact: true })).toBeVisible()
    await expect(page.getByText('异常预警', { exact: true })).toBeVisible()
    await expect(page.getByText('系统信息')).toBeVisible()
    await expect(page.getByText('当前用户', { exact: true })).toBeVisible()
  })

  test('刷新：触发真实请求且仍停留在看板', async ({ page }) => {
    test.skip(!PASS, '设置 PW_PASS 后运行完整用例')
    test.setTimeout(120_000)

    await login(page)
    await page.goto('/dashboard')
    await expect(page.getByRole('button', { name: '刷新' })).toBeEnabled()

    await Promise.all([
      page.waitForResponse((r) => r.url().includes('/ops/dashboard/realtime') && r.request().method() === 'GET'),
      page.waitForResponse((r) => r.url().includes('/eq/alarms') && r.request().method() === 'GET'),
      page.getByRole('button', { name: '刷新' }).click(),
    ])

    await expect(page).toHaveURL(/\/dashboard/)
    await expect(page.locator('.kpi-grid .kpi-card')).toHaveCount(4)
  })

  test('快捷入口：鼠标点击进入目标页并可回看板', async ({ page }) => {
    test.skip(!PASS, '设置 PW_PASS 后运行完整用例')
    test.setTimeout(120_000)

    await login(page)
    await page.goto('/dashboard')

    const quick = page.locator('.quick-item').first()
    await expect(quick).toBeVisible()
    const label = (await quick.locator('.quick-label').textContent())?.trim() || '快捷入口'
    await quick.click()

    await expect(page).not.toHaveURL(/\/dashboard\/?$/)
    // 应进入具体业务页（由权限决定首项路径）
    await expect(page.locator('.layout-main, .el-main, main').first()).toBeVisible()

    await page.goto('/dashboard')
    await expect(page.locator('.page-header-title')).toContainText('生产看板')
    // 回归：仍能看到刚点的入口（证明未白屏）
    await expect(page.locator('.quick-actions .quick-item').filter({ hasText: label })).toBeVisible()
  })

  test('快捷入口：键盘 Enter 触发跳转（无障碍路径）', async ({ page }) => {
    test.skip(!PASS, '设置 PW_PASS 后运行完整用例')
    test.setTimeout(120_000)

    await login(page)
    await page.goto('/dashboard')

    const first = page.locator('.quick-item').first()
    await first.focus()
    await page.keyboard.press('Enter')
    await expect(page).not.toHaveURL(/\/dashboard\/?$/)
  })

  test('设备分布：有数据时展示图例与条带；无数据时友好空态', async ({ page }) => {
    test.skip(!PASS, '设置 PW_PASS 后运行完整用例')
    test.setTimeout(120_000)

    await login(page)
    await page.goto('/dashboard')
    await page.getByRole('button', { name: '刷新' }).click()
    await page.waitForLoadState('networkidle').catch(() => {})

    const deviceCard = page.locator('.dashboard-card').filter({ hasText: '设备类型分布' })
    const legend = deviceCard.locator('.device-legend')
    const empty = deviceCard.getByText('还没有录入设备信息')

    const hasLegend = await legend.isVisible().catch(() => false)
    const hasEmpty = await empty.isVisible().catch(() => false)
    expect(hasLegend || hasEmpty).toBeTruthy()

    if (hasLegend) {
      await expect(legend.getByText('离线', { exact: true })).toBeVisible()
      await expect(legend.getByText('在线', { exact: true })).toBeVisible()
      await expect(legend.getByText('在线且异常', { exact: true })).toBeVisible()
    }
  })

  test('异常预警：头部状态与列表态之一成立（用户可理解）', async ({ page }) => {
    test.skip(!PASS, '设置 PW_PASS 后运行完整用例')
    test.setTimeout(120_000)

    await login(page)
    await page.goto('/dashboard')

    const alarmCard = page.getByTestId('dashboard-alarm-card')
    await expect(alarmCard).toBeVisible()

    const header = alarmCard.locator('.card-header').first()
    await expect(
      header.locator('.el-tag').filter({ hasText: /条待处理|无异常|告警未加载|加载中/ }).first()
    ).toBeVisible({ timeout: 30_000 })

    const body = alarmCard.locator('.ygt-empty, .alarm-list, .card-error').first()
    await expect(body).toBeVisible({ timeout: 30_000 })

    const isError = await alarmCard.locator('.card-error').isVisible()
    const isEmpty = await alarmCard.locator('.ygt-empty').isVisible()
    if (isError) {
      await expect(alarmCard.getByRole('button', { name: '重试' })).toBeVisible()
    } else if (isEmpty) {
      await expect(alarmCard.locator('.ygt-state-title')).toContainText('暂无异常预警')
      await expect(alarmCard.getByText('系统运行正常', { exact: false })).toBeVisible()
    } else {
      await expect(alarmCard.locator('.alarm-item').first()).toBeVisible()
      await expect(alarmCard.getByRole('button', { name: '处理' }).first()).toBeVisible()
    }
  })
})
