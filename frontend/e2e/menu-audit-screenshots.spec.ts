/**
 * 按侧边栏约定路由逐页打开并全页截图（与 Layout.vue 菜单一致，便于需求对照）。
 * PW_USER / PW_PASS 传入凭据；勿将真实密码提交到仓库。
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const USER = process.env.PW_USER ?? 'admin'
const PASS = process.env.PW_PASS ?? '' // 勿写死密码；运行示例：PW_PASS=*** npx playwright test e2e/menu-audit-screenshots.spec.ts

/** 与 src/components/Layout.vue 中 el-menu-item index 一致（含权限菜单；admin 通常全部可见） */
const SIDEBAR_MENU: { group: string; item: string; path: string }[] = [
  { group: '处方管理', item: '处方录入', path: '/prescriptions' },
  { group: '处方管理', item: '紧急处方', path: '/emergency' },
  { group: '生产指挥', item: '生产看板', path: '/dashboard' },
  { group: '生产指挥', item: '排产调度', path: '/task-assignment' },
  { group: '生产指挥', item: '时效监控', path: '/time-monitor' },
  { group: '生产指挥', item: '告警管理', path: '/alarms' },
  { group: '生产指挥', item: '设备监控', path: '/device-monitor' },
  { group: '生产指挥', item: '产能统计', path: '/capacity' },
  { group: '生产指挥', item: '设备效能', path: '/device-utilization' },
  { group: '生产指挥', item: '设备操控', path: '/device-command' },
  { group: '生产指挥', item: '数字孪生', path: '/digital-twin' },
  { group: '煎药作业', item: '煎药任务', path: '/tasks' },
  { group: '煎药作业', item: '流程跟踪', path: '/step-visualization' },
  { group: '煎药作业', item: '分组投料', path: '/herb-group' },
  { group: '煎药作业', item: '语音播报', path: '/voice-setting' },
  { group: '质量检验', item: '质量检验', path: '/quality' },
  { group: '质量检验', item: '留样管理', path: '/retain-sample' },
  { group: '质量检验', item: '异常工单', path: '/exception-order' },
  { group: '质量检验', item: '返工处理', path: '/task-rollback' },
  { group: '质量检验', item: '温曲查询', path: '/temperature-curve' },
  { group: '质量检验', item: '合格统计', path: '/report/qc-rate' },
  { group: '发药管理', item: '成品暂存', path: '/shelf-manage' },
  { group: '发药管理', item: '发药确认', path: '/prod/delivery' },
  { group: '发药管理', item: '进度查询', path: '/patient-query' },
  { group: '追溯查询', item: '处方追溯', path: '/traces' },
  { group: '追溯查询', item: '批次追溯', path: '/trace/batch' },
  { group: '追溯查询', item: '异常追溯', path: '/trace/exception' },
  { group: '追溯查询', item: '操作日志', path: '/logs' },
  { group: '工艺配置', item: '煎药方案', path: '/schemes' },
  { group: '工艺配置', item: '加水公式', path: '/water-formulas' },
  { group: '工艺配置', item: '包装规格', path: '/formula/package-spec' },
  { group: '工艺配置', item: '告警配置', path: '/alarm-configs' },
  { group: '工艺配置', item: '设备维保', path: '/device-maintenance' },
  { group: '工艺配置', item: '清洗记录', path: '/wash-record' },
  { group: '基础数据', item: '医院管理', path: '/hospitals' },
  { group: '基础数据', item: '科室管理', path: '/base/department' },
  { group: '基础数据', item: '医师管理', path: '/base/doctor' },
  { group: '基础数据', item: '药材管理', path: '/base/medicine' },
  { group: '基础数据', item: '成品货架管理', path: '/base/finished-shelf' },
  { group: '基础数据', item: '毒性药材管理', path: '/toxic-medicine' },
  { group: '基础数据', item: '人员管理', path: '/users' },
  { group: '基础数据', item: '身份条码', path: '/employee-barcode' },
  { group: '打印中心', item: '标签打印', path: '/print/template' },
  { group: '打印中心', item: '打印管理', path: '/print/printer' },
  { group: '打印中心', item: '打印记录', path: '/print/log' },
  { group: '打印中心', item: '工单打印', path: '/work-order-print' },
  { group: '系统管理', item: '用户管理', path: '/sys/users' },
  { group: '系统管理', item: '权限管理', path: '/roles' },
  { group: '系统管理', item: '菜单管理', path: '/menus' },
  { group: '系统管理', item: '参数配置', path: '/configs' },
  { group: '系统管理', item: '系统日志', path: '/sys/logs' },
]

function safeFileName(group: string, item: string, idx: number) {
  const t = `${group}_${item}`.replace(/\s+/g, '_').replace(/[^\w\u4e00-\u9fa5_-]/g, '')
  return `${String(idx).padStart(3, '0')}_${t.slice(0, 56)}`
}

test.describe.configure({ mode: 'serial' })

test('登录后按路由表逐页截图', async ({ page }, testInfo) => {
  if (!PASS) {
    test.skip(true, '请设置环境变量 PW_PASS（及可选 PW_USER）后再运行本用例')
  }
  test.setTimeout(300_000)
  const shotDir = path.join(testInfo.project.outputDir, 'menu-audit-screenshots')
  fs.mkdirSync(shotDir, { recursive: true })

  await page.goto('/login')
  await page.getByPlaceholder('用户名').fill(USER)
  await page.getByPlaceholder('密码').fill(PASS)
  await page.getByRole('button', { name: '登录' }).click()
  await page.waitForURL((u) => !u.pathname.endsWith('/login'), { timeout: 120_000 })

  const summary: { group: string; item: string; path: string; url: string; shot: string; note?: string }[] = []

  for (let k = 0; k < SIDEBAR_MENU.length; k++) {
    const { group, item, path: p } = SIDEBAR_MENU[k]
    await page.goto(p)
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(500)
    const url = page.url()
    let note: string | undefined
    if (url.includes('/dashboard') && !url.endsWith(p) && p !== '/dashboard') {
      note = '无权限或路由被重定向到生产看板'
    }
    const file = `${safeFileName(group, item, k)}.png`
    const full = path.join(shotDir, file)
    await page.screenshot({ path: full, fullPage: true })
    summary.push({ group, item, path: p, url, shot: full, note })
  }

  fs.writeFileSync(path.join(shotDir, 'summary.json'), JSON.stringify(summary, null, 2), 'utf-8')
  await expect.soft(summary.length).toBe(SIDEBAR_MENU.length)
})
