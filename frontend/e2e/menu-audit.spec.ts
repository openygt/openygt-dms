import { test, expect, Page } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const SCREENSHOT_DIR = path.resolve(__dirname, '..', '..', 'tests', 'menu-audit-screenshots')
const REPORT_PATH = path.resolve(__dirname, '..', '..', 'tests', 'menu-audit-report.md')

// All menu items organized by menu group
const MENU_GROUPS: { group: string; items: { label: string; path: string }[] }[] = [
  {
    group: '处方管理',
    items: [
      { label: '处方录入', path: '/prescriptions' },
      { label: '紧急处方', path: '/emergency' },
    ],
  },
  {
    group: '生产指挥',
    items: [
      { label: '生产看板', path: '/dashboard' },
      { label: '生产记录', path: '/task-assignment' },
      { label: '流程跟踪', path: '/step-visualization' },
      { label: '产能统计', path: '/capacity' },
      { label: '操作日志', path: '/logs' },
      { label: '生产设置', path: '/production-setting' },
    ],
  },
  {
    group: '设备管理',
    items: [
      { label: '设备台账', path: '/devices' },
      { label: '设备联网', path: '/device-network' },
      { label: '分组配对', path: '/device-group-manage' },
      { label: '设备监控', path: '/device-monitor' },
      { label: '远程操控', path: '/device-command' },
      { label: '告警中心', path: '/alarms' },
      { label: '设备维保', path: '/device-maintenance' },
      { label: '清洗记录', path: '/wash-record' },
    ],
  },
  {
    group: '煎药作业',
    items: [
      { label: '煎药任务', path: '/tasks' },
      { label: '分组投料', path: '/herb-group' },
      { label: '语音播报', path: '/voice-setting' },
    ],
  },
  {
    group: '质量检验',
    items: [
      { label: '质量检验', path: '/quality' },
      { label: '留样管理', path: '/retain-sample' },
      { label: '异常工单', path: '/exception-order' },
      { label: '返工处理', path: '/task-rollback' },
      { label: '温曲查询', path: '/temperature-curve' },
      { label: '合格统计', path: '/report/qc-rate' },
    ],
  },
  {
    group: '发药管理',
    items: [
      { label: '成品暂存', path: '/shelf-manage' },
      { label: '发药确认', path: '/prod/delivery' },
      { label: '进度查询', path: '/patient-query' },
    ],
  },
  {
    group: '追溯查询',
    items: [
      { label: '处方追溯', path: '/traces' },
      { label: '批次追溯', path: '/trace/batch' },
      { label: '异常追溯', path: '/trace/exception' },
    ],
  },
  {
    group: '工艺配置',
    items: [
      { label: '煎药方案', path: '/schemes' },
      { label: '加水公式', path: '/water-formulas' },
      { label: '包装规格', path: '/formula/package-spec' },
      { label: '告警配置', path: '/alarm-configs' },
    ],
  },
  {
    group: '基础数据',
    items: [
      { label: '医院管理', path: '/hospitals' },
      { label: '科室管理', path: '/base/department' },
      { label: '医师管理', path: '/base/doctor' },
      { label: '药材管理', path: '/base/medicine' },
      { label: '成品货架管理', path: '/base/finished-shelf' },
      { label: '毒性药材管理', path: '/toxic-medicine' },
      { label: '人员管理', path: '/users' },
      { label: '身份条码', path: '/employee-barcode' },
    ],
  },
  {
    group: '打印中心',
    items: [
      { label: '标签打印', path: '/print/template' },
      { label: '打印管理', path: '/print/printer' },
      { label: '打印记录', path: '/print/log' },
      { label: '工单打印', path: '/work-order-print' },
    ],
  },
  {
    group: '系统管理',
    items: [
      { label: '用户管理', path: '/sys/users' },
      { label: '权限管理', path: '/roles' },
      { label: '菜单管理', path: '/menus' },
      { label: '参数配置', path: '/configs' },
      { label: '系统日志', path: '/sys/logs' },
    ],
  },
  {
    group: '未归入菜单的独立页面',
    items: [
      { label: '处方接收', path: '/prod/receive' },
      { label: '药材消耗', path: '/consume-log' },
      { label: '设备效能', path: '/device-utilization' },
      { label: '数据看板', path: '/eq-dashboard' },
      { label: '工作量统计', path: '/workload' },
      { label: '处方默认设置', path: '/prescription-defaults' },
      { label: '时效监控', path: '/time-monitor' },
      { label: '打印中心(路由)', path: '/print-center' },
      { label: '接口中心', path: '/interface-center' },
    ],
  },
]

interface PageAuditResult {
  label: string
  path: string
  group: string
  screenshot: string
  status: 'ok' | 'error' | 'empty' | 'loading' | 'placeholder'
  issue: string
  hasData: boolean
  loadTimeMs: number
}

const results: PageAuditResult[] = []

async function auditPage(page: Page, group: string, label: string, urlPath: string): Promise<PageAuditResult> {
  const sanitizedGroup = group.replace(/[/\\]/g, '-')
  const sanitizedLabel = label.replace(/[/\\]/g, '-')
  const screenshotName = `${sanitizedGroup}__${sanitizedLabel}.png`
  const screenshotPath = path.join(SCREENSHOT_DIR, screenshotName)

  const startTime = Date.now()

  let status: PageAuditResult['status'] = 'ok'
  let issue = ''
  let hasData = false

  try {
    await page.goto(urlPath, { waitUntil: 'networkidle', timeout: 15000 })
  } catch {
    // Some pages may have long-running requests; try with 'load' instead
    try {
      await page.goto(urlPath, { waitUntil: 'load', timeout: 10000 })
    } catch {
      status = 'error'
      issue = '页面加载超时或失败'
    }
  }

  const loadTimeMs = Date.now() - startTime

  if (status !== 'error') {
    // Wait a bit for Vue rendering
    await page.waitForTimeout(1500)

    // Check for common "empty/no-data" patterns
    const pageText = await page.textContent('body').catch(() => '')

    // Check for "功能开发中" placeholder
    const devPlaceholder = pageText.includes('功能开发中') || pageText.includes('开发中') || pageText.includes('敬请期待')

    // Check for "暂无数据" empty state
    const noData = pageText.includes('暂无数据') || pageText.includes('No data') || pageText.includes('暂无')

    // Check for empty tables (el-empty component)
    const hasElEmpty = await page.locator('.el-empty').count() > 0
    const hasElTableEmpty = await page.locator('.el-table__empty-text').count() > 0

    // Check for actual data in tables or cards
    const tableRows = await page.locator('.el-table__body tr').count()
    const dataCards = await page.locator('.el-card').count()

    hasData = tableRows > 0 || dataCards > 0

    // Check for major errors on page
    const hasError = pageText.includes('Error') || pageText.includes('错误') || pageText.includes('500') || pageText.includes('404')

    if (devPlaceholder) {
      status = 'placeholder'
      issue = '页面显示"功能开发中"占位符'
    } else if (hasError) {
      status = 'error'
      issue = '页面存在错误信息'
    } else if (hasElEmpty || hasElTableEmpty || noData) {
      // It might be normal empty state (no data yet)
      status = 'empty'
      issue = hasElTableEmpty ? '表格显示"暂无数据"' : '页面显示空状态'
    }

    // Check if page redirected to dashboard (login/perm issue)
    const currentPath = new URL(page.url()).pathname
    if (currentPath === '/dashboard' && urlPath !== '/dashboard') {
      status = 'error'
      issue = '页面被重定向到首页（可能无权限或路由不存在）'
    }
  }

  // Take screenshot
  await page.screenshot({ path: screenshotPath, fullPage: true })

  return {
    label,
    path: urlPath,
    group,
    screenshot: screenshotName,
    status,
    issue,
    hasData,
    loadTimeMs,
  }
}

test.describe('全系统菜单页面评审', () => {
  test.setTimeout(30 * 60 * 1000) // 30 minutes

  test('登录并逐页截图评审', async ({ page }) => {
    // Ensure screenshot directory exists
    if (!fs.existsSync(SCREENSHOT_DIR)) {
      fs.mkdirSync(SCREENSHOT_DIR, { recursive: true })
    }

    // Step 1: Login
    console.log('\n=== Step 1: 登录 ===')
    await page.goto('http://localhost:5172/login', { waitUntil: 'networkidle' })
    await page.fill('input[name="username"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL('**/dashboard', { timeout: 10000 })
    console.log('✅ 登录成功')

    // Step 2: Iterate through all menu pages
    let totalPages = 0
    for (const group of MENU_GROUPS) {
      totalPages += group.items.length
    }

    let idx = 0
    for (const group of MENU_GROUPS) {
      console.log(`\n--- ${group.group} (${group.items.length} 页) ---`)

      for (const item of group.items) {
        idx++
        console.log(`[${idx}/${totalPages}] ${group.group} > ${item.label} (${item.path})`)

        const result = await auditPage(page, group.group, item.label, item.path)
        results.push(result)

        const emoji = result.status === 'ok' ? '✅' : result.status === 'empty' ? '⚠️' : '❌'
        console.log(`  ${emoji} ${result.status} | ${result.loadTimeMs}ms | ${result.issue || '正常'}`)
      }
    }

    // Step 3: Generate audit report
    const report = generateReport(results, totalPages)
    fs.writeFileSync(REPORT_PATH, report, 'utf-8')
    console.log(`\n📄 评审报告已生成: ${REPORT_PATH}`)
    console.log(`📸 截图目录: ${SCREENSHOT_DIR}`)
  })
})

function generateReport(results: PageAuditResult[], total: number): string {
  const okCount = results.filter(r => r.status === 'ok').length
  const emptyCount = results.filter(r => r.status === 'empty').length
  const placeholderCount = results.filter(r => r.status === 'placeholder').length
  const errorCount = results.filter(r => r.status === 'error').length
  const passRate = ((okCount / total) * 100).toFixed(1)

  let report = `# 全系统菜单页面截图评审报告

> 生成时间: ${new Date().toISOString()}
> 总计: ${total} 个页面 | ✅ 正常: ${okCount} | ⚠️ 空状态: ${emptyCount} | 🚧 占位符: ${placeholderCount} | ❌ 错误: ${errorCount}
> 通过率: ${passRate}%

---

## 评审结果总览

| 状态 | 数量 | 占比 |
|:---|:---:|:---:|
| ✅ 正常 | ${okCount} | ${((okCount / total) * 100).toFixed(1)}% |
| ⚠️ 空状态（无数据） | ${emptyCount} | ${((emptyCount / total) * 100).toFixed(1)}% |
| 🚧 占位符（功能开发中） | ${placeholderCount} | ${((placeholderCount / total) * 100).toFixed(1)}% |
| ❌ 错误 | ${errorCount} | ${((errorCount / total) * 100).toFixed(1)}% |

---

## 详细评审结果

`

  for (const group of MENU_GROUPS) {
    const groupResults = results.filter(r => r.group === group.group)
    if (groupResults.length === 0) continue

    report += `### ${group.group}\n\n`
    report += `| 页面 | 路径 | 状态 | 加载(ms) | 有数据 | 问题 |\n`
    report += `|:---|:---|:---:|:---:|:---:|:---|\n`

    for (const r of groupResults) {
      const emoji = r.status === 'ok' ? '✅' : r.status === 'empty' ? '⚠️' : r.status === 'placeholder' ? '🚧' : '❌'
      report += `| ${r.label} | \`${r.path}\` | ${emoji} ${r.status} | ${r.loadTimeMs} | ${r.hasData ? '有' : '无'} | ${r.issue || '-'} |\n`
    }
    report += '\n'
  }

  // Summary of issues
  const errors = results.filter(r => r.status === 'error')
  const placeholders = results.filter(r => r.status === 'placeholder')
  const empties = results.filter(r => r.status === 'empty')

  report += `---

## 需关注的问题

### ❌ 错误页面 (${errors.length})

`
  if (errors.length === 0) {
    report += '无\n'
  } else {
    for (const r of errors) {
      report += `- **${r.group} > ${r.label}** (\`${r.path}\`): ${r.issue}\n`
    }
  }

  report += `\n### 🚧 占位符页面 (${placeholders.length})

`
  if (placeholders.length === 0) {
    report += '无\n'
  } else {
    for (const r of placeholders) {
      report += `- **${r.group} > ${r.label}** (\`${r.path}\`): ${r.issue}\n`
    }
  }

  report += `\n### ⚠️ 空状态页面 (${empties.length})

`
  if (empties.length === 0) {
    report += '无\n'
  } else {
    for (const r of empties) {
      report += `- **${r.group} > ${r.label}** (\`${r.path}\`): ${r.issue}\n`
    }
  }

  report += `\n---

## 截图清单

所有截图存放于 \`${SCREENSHOT_DIR}\`

| 文件 | 页面 |
|:---|:---|
`
  for (const r of results) {
    report += `| ${r.screenshot} | ${r.group} > ${r.label} |\n`
  }

  report += `
---

*报告由 Playwright 自动化生成*
`

  return report
}
