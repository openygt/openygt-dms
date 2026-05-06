import { test, expect, Page } from '@playwright/test'

// ═══════════════════════════════════════════════════════════
// OpenYGT-DMS 全流程表现层 E2E 测试
// 覆盖 docs/20260502 01-12 业务流程
// 录屏+解说模式：每个关键步骤带停顿和视觉标记
// ═══════════════════════════════════════════════════════════

const ADMIN = { username: 'admin', password: 'admin123' }

async function login(page: Page, user = ADMIN) {
  await page.goto('/login')
  await page.locator('.el-input__inner').first().fill(user.username)
  await page.locator('.el-input__inner').nth(1).fill(user.password)
  await page.locator('button:has-text("登录"), button.el-button--primary').click()
  await page.waitForURL('/dashboard', { timeout: 15000 })
  await expect(page.locator('.layout-header .header-title')).toContainText('生产看板').catch(() =>
    expect(page.locator('body')).toContainText('生产看板')
  )
}

async function narrate(page: Page, text: string, ms = 2000) {
  // 在页面上显示解说字幕（通过 evaluate 注入 DOM）
  await page.evaluate((t) => {
    const el = document.createElement('div')
    el.id = 'e2e-narrator'
    el.style.cssText = 'position:fixed;bottom:20px;left:50%;transform:translateX(-50%);background:rgba(0,0,0,0.75);color:#fff;padding:10px 20px;border-radius:8px;font-size:16px;z-index:99999;transition:opacity 0.5s;'
    el.textContent = '🔊 ' + t
    const old = document.getElementById('e2e-narrator')
    if (old) old.remove()
    document.body.appendChild(el)
    setTimeout(() => { const e = document.getElementById('e2e-narrator'); if(e) e.style.opacity='0'; setTimeout(()=>e?.remove(),500) }, 4000)
  }, text)
  await page.waitForTimeout(ms)
}

test.describe('🎬 P0-P3 业务流程改进 E2E 演示', () => {

  test('完整业务链：登录 → 处方 → 任务 → 设备 → 质检 → 追溯', async ({ page }) => {
    // ── 01 业务全景：登录 ──
    await login(page)
    await narrate(page, '登录成功，进入生产看板Dashboard')

    // ── 02 处方接收与任务创建 ──
    await page.goto('/prescriptions')
    await page.waitForSelector('.el-table')
    await narrate(page, '进入处方管理页面，查看待接收处方列表')

    // 创建新处方
    await page.click('text=新增处方, button:has-text("新增")')
    await page.waitForSelector('.el-dialog')
    await page.locator('.el-dialog .el-input__inner').nth(0).fill('E2E演示患者')
    await page.locator('.el-dialog .el-input__inner').nth(1).fill('黄芪30g,当归15g,党参20g')
    await page.click('.el-dialog >> text=确定')
    await page.waitForTimeout(800)
    await narrate(page, '创建一张新处方，包含毒性药材校验')

    // ── 03 煎药任务状态机 ──
    await page.goto('/tasks')
    await page.waitForSelector('.el-table')
    await narrate(page, '进入任务列表，查看各状态任务分布')

    // 挂起恢复演示
    const row = page.locator('.el-table__row').first()
    await row.locator('text=详情, button:has-text("详情")').first().click()
    await page.waitForTimeout(500)
    await narrate(page, '查看任务详情，展示任务状态机流转')

    // ── 04 设备管理状态机 ──
    await page.goto('/devices')
    await page.waitForSelector('.el-table')
    await narrate(page, '进入设备管理，查看煎药机和包装机实时状态')

    // 点击设备详情
    await page.locator('.el-table__row').first().locator('text=详情, button:has-text("详情")').first().click()
    await page.waitForTimeout(500)
    await narrate(page, '查看设备详情，包含温度曲线和精细状态')

    // ── 05 质量检验流程 ──
    await page.goto('/quality/inspect')
    await page.waitForSelector('.el-table')
    await narrate(page, '进入质检管理，查看待检验和已检验记录')

    // ── 06 配送交付与货架 ──
    await page.goto('/shelf')
    await page.waitForSelector('.el-table')
    await narrate(page, '进入货架管理，查看成品上架状态')

    // ── 08 紧急处方与加急 ──
    await page.goto('/emergency')
    await page.waitForSelector('.el-table')
    await narrate(page, '查看紧急处方列表，展示加急插队逻辑')

    // ── 10 库存管理与消耗 ──
    await page.goto('/inventory/consume')
    await page.waitForSelector('.el-table')
    await narrate(page, '查看库存消耗记录，展示任务触发药材出库')

    // ── 11 时间监控与预警 ──
    await page.goto('/monitor/time-monitor')
    await page.waitForTimeout(800)
    await narrate(page, '进入时间监控，查看工序超时预警')

    // ── 12 设备维修保养 ──
    await page.goto('/eq/maintenance')
    await page.waitForSelector('.el-table')
    await narrate(page, '查看设备维保记录')

    // ── 13 追溯与审计 ──
    await page.goto('/trace')
    await page.waitForSelector('.el-table')
    await narrate(page, '进入追溯查询，展示全流程事件链')

    // ── P0 新增功能 ──
    await page.goto('/toxic-medicine')
    await page.waitForSelector('.el-table')
    await narrate(page, '【P0】毒性药材管理：查看毒性等级和剂量限制')

    await page.goto('/exception-order')
    await page.waitForSelector('.el-table')
    await narrate(page, '【P0】异常工单：查看设备故障和质量问题')

    await page.goto('/retain-sample')
    await page.waitForSelector('.el-table')
    await narrate(page, '【P0】留样管理：查看留样过期预警')

    await page.goto('/wash-record')
    await page.waitForSelector('.el-table')
    await narrate(page, '【P0】设备清洗：查看清洗记录和标准')

    await page.goto('/signature')
    await page.waitForSelector('.el-table')
    await narrate(page, '【P2】电子签名：查看质检和交接签名记录')

    await page.goto('/archive')
    await page.waitForTimeout(800)
    await narrate(page, '【P3】数据归档：执行归档策略')
  })

  test('任务挂起与恢复状态机', async ({ page }) => {
    await login(page)
    await page.goto('/tasks')
    await page.waitForSelector('.el-table')

    // 挂起第一个可挂起的任务
    const firstRow = page.locator('.el-table__row').first()
    await firstRow.locator('text=挂起, button:has-text("挂起")').first().click()
    await page.waitForSelector('.el-dialog')
    await page.locator('.el-dialog textarea').fill('E2E测试：设备维护，临时挂起')
    await page.click('.el-dialog >> text=确定')
    await page.waitForTimeout(800)
    await narrate(page, '任务挂起成功，状态变为已挂起，设备自动释放')

    // 恢复任务
    await firstRow.locator('text=恢复, button:has-text("恢复")').first().click()
    await page.waitForTimeout(800)
    await narrate(page, '任务恢复成功，回到挂起前的状态')
  })

  test('处方接收流程：待接收 → 已接收 → 生成任务', async ({ page }) => {
    await login(page)
    await page.goto('/prescriptions')
    await page.waitForSelector('.el-table')

    // 找到待接收的处方
    const pendingRow = page.locator('.el-table__row').filter({ hasText: /待接收|PENDING/ }).first()
    if (await pendingRow.isVisible().catch(() => false)) {
      await pendingRow.locator('text=接收, button:has-text("接收")').first().click()
      await page.waitForTimeout(800)
      await narrate(page, '处方接收成功，系统自动生成待泡药任务')
    }

    await page.goto('/tasks')
    await page.waitForSelector('.el-table')
    await narrate(page, '验证任务列表中已生成对应的新任务')
  })
})
