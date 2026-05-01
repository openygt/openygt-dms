# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: decoction-workflow.spec.ts >> Flow 1: 煎煮核心流程 >> 处方接收页面列表渲染
- Location: e2e/decoction-workflow.spec.ts:19:3

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: locator('.el-table__body-wrapper tbody tr').first()
Expected: visible
Timeout: 5000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 5000ms
  - waiting for locator('.el-table__body-wrapper tbody tr').first()

```

# Page snapshot

```yaml
- generic [ref=e4]:
  - complementary [ref=e5]:
    - generic [ref=e6]:
      - img [ref=e8]
      - generic [ref=e11]: 智能煎药管理系统
    - menubar [ref=e12]:
      - menuitem "实时看板" [ref=e13] [cursor=pointer]:
        - img [ref=e15]
        - generic [ref=e17]: 实时看板
      - menuitem "生产执行" [expanded] [ref=e18]:
        - generic [ref=e19] [cursor=pointer]:
          - img [ref=e21]
          - generic [ref=e24]: 生产执行
          - img [ref=e26]
        - menu [ref=e28]:
          - menuitem "处方接收" [ref=e29] [cursor=pointer]
          - menuitem "煎煮任务" [ref=e30] [cursor=pointer]
          - menuitem "质检管理" [ref=e31] [cursor=pointer]
          - menuitem "交付管理" [ref=e32] [cursor=pointer]
      - menuitem "设备管理" [ref=e33]:
        - generic [ref=e34] [cursor=pointer]:
          - img [ref=e36]
          - generic [ref=e39]: 设备管理
          - img [ref=e41]
      - menuitem "工艺配置" [ref=e43]:
        - generic [ref=e44] [cursor=pointer]:
          - img [ref=e46]
          - generic [ref=e48]: 工艺配置
          - img [ref=e50]
      - menuitem "质量追溯" [ref=e52]:
        - generic [ref=e53] [cursor=pointer]:
          - img [ref=e55]
          - generic [ref=e57]: 质量追溯
          - img [ref=e59]
      - menuitem "数据分析" [ref=e61]:
        - generic [ref=e62] [cursor=pointer]:
          - img [ref=e64]
          - generic [ref=e66]: 数据分析
          - img [ref=e68]
      - menuitem "打印配置" [ref=e70]:
        - generic [ref=e71] [cursor=pointer]:
          - img [ref=e73]
          - generic [ref=e75]: 打印配置
          - img [ref=e77]
      - menuitem "基础数据" [ref=e79]:
        - generic [ref=e80] [cursor=pointer]:
          - img [ref=e82]
          - generic [ref=e86]: 基础数据
          - img [ref=e88]
      - menuitem "系统管理" [ref=e90]:
        - generic [ref=e91] [cursor=pointer]:
          - img [ref=e93]
          - generic [ref=e96]: 系统管理
          - img [ref=e98]
  - generic [ref=e100]:
    - generic [ref=e101]:
      - generic [ref=e102]: 处方接收
      - generic [ref=e103]:
        - 'button "当前主题: 跟随系统" [ref=e105] [cursor=pointer]':
          - img [ref=e107]
        - separator [ref=e110]
        - generic [ref=e111]: 当前用户：admin
        - button "退出" [ref=e112] [cursor=pointer]:
          - generic [ref=e113]: 退出
    - main [ref=e114]:
      - generic [ref=e116]:
        - generic [ref=e119]: 处方接收
        - generic [ref=e120]:
          - generic [ref=e121]:
            - radiogroup [ref=e124]:
              - generic [ref=e125]:
                - radio "全部" [checked] [ref=e126]
                - generic [ref=e127] [cursor=pointer]: 全部
              - generic [ref=e128]:
                - radio "待接收" [ref=e129]
                - generic [ref=e130] [cursor=pointer]: 待接收
              - generic [ref=e131]:
                - radio "已接收" [ref=e132]
                - generic [ref=e133] [cursor=pointer]: 已接收
              - generic [ref=e134]:
                - radio "已驳回" [ref=e135]
                - generic [ref=e136] [cursor=pointer]: 已驳回
            - generic [ref=e137]:
              - generic [ref=e138]: 关键词
              - textbox "关键词" [ref=e142]:
                - /placeholder: 患者姓名/处方号
            - generic [ref=e144]:
              - button "查询" [ref=e145] [cursor=pointer]:
                - generic [ref=e146]: 查询
              - button "重置" [ref=e147] [cursor=pointer]:
                - generic [ref=e148]: 重置
          - generic [ref=e150]:
            - table [ref=e152]:
              - rowgroup [ref=e163]:
                - row "处方号 患者姓名 医院 科室 医师 剂数 状态 备注 操作" [ref=e164]:
                  - columnheader "处方号" [ref=e165]:
                    - generic [ref=e166]: 处方号
                  - columnheader "患者姓名" [ref=e167]:
                    - generic [ref=e168]: 患者姓名
                  - columnheader "医院" [ref=e169]:
                    - generic [ref=e170]: 医院
                  - columnheader "科室" [ref=e171]:
                    - generic [ref=e172]: 科室
                  - columnheader "医师" [ref=e173]:
                    - generic [ref=e174]: 医师
                  - columnheader "剂数" [ref=e175]:
                    - generic [ref=e176]: 剂数
                  - columnheader "状态" [ref=e177]:
                    - generic [ref=e178]: 状态
                  - columnheader "备注" [ref=e179]:
                    - generic [ref=e180]: 备注
                  - columnheader "操作" [ref=e181]:
                    - generic [ref=e182]: 操作
            - generic [ref=e186]:
              - table:
                - rowgroup
              - generic [ref=e188]: 暂无数据
          - generic [ref=e190]:
            - img [ref=e192]
            - paragraph [ref=e209]: 暂无数据
```

# Test source

```ts
  1   | import { test, expect } from '@playwright/test'
  2   | import { login, navigate, waitTable, assertCard, assertChart } from './helpers'
  3   | 
  4   | test.describe('Flow 1: 煎煮核心流程', () => {
  5   | 
  6   |   test('登录并验证 Dashboard KPI', async ({ page }) => {
  7   |     await login(page)
  8   | 
  9   |     // Should redirect to dashboard
  10  |     await page.waitForURL(/\/(dashboard|\/)/, { timeout: 10000 })
  11  | 
  12  |     // Verify KPI cards are rendered (main dashboard)
  13  |     const kpiCards = page.locator('.kpi-card')
  14  |     await expect(kpiCards.first()).toBeVisible({ timeout: 10000 })
  15  |     const kpiCount = await kpiCards.count()
  16  |     expect(kpiCount).toBeGreaterThanOrEqual(4)
  17  |   })
  18  | 
  19  |   test('处方接收页面列表渲染', async ({ page }) => {
  20  |     await login(page)
  21  |     await navigate(page, '/prod/receive')
  22  | 
  23  |     // Verify table renders
  24  |     await waitTable(page)
  25  |     const rows = page.locator('.el-table__body-wrapper tbody tr')
> 26  |     await expect(rows.first()).toBeVisible()
      |                                ^ Error: expect(locator).toBeVisible() failed
  27  |   })
  28  | 
  29  |   test('处方管理列表渲染', async ({ page }) => {
  30  |     await login(page)
  31  |     await navigate(page, '/prescriptions')
  32  | 
  33  |     await waitTable(page)
  34  |     const rows = page.locator('.el-table__body-wrapper tbody tr')
  35  |     await expect(rows.first()).toBeVisible()
  36  |   })
  37  | 
  38  |   test('任务看板状态标签', async ({ page }) => {
  39  |     await login(page)
  40  |     await navigate(page, '/tasks')
  41  | 
  42  |     await waitTable(page)
  43  |     // Verify status tags exist in the table
  44  |     const tags = page.locator('.el-table .el-tag')
  45  |     await expect(tags.first()).toBeVisible({ timeout: 5000 })
  46  |   })
  47  | 
  48  |   test('设备监控页面渲染', async ({ page }) => {
  49  |     await login(page)
  50  |     await navigate(page, '/device-monitor')
  51  | 
  52  |     // Wait for device cards to load
  53  |     await page.waitForTimeout(2000)
  54  |     const deviceCards = page.locator('.device-card, .el-card')
  55  |     const cardCount = await deviceCards.count()
  56  |     expect(cardCount).toBeGreaterThanOrEqual(1)
  57  |   })
  58  | 
  59  |   test('质检管理列表渲染', async ({ page }) => {
  60  |     await login(page)
  61  |     await navigate(page, '/quality')
  62  | 
  63  |     await waitTable(page)
  64  |     const rows = page.locator('.el-table__body-wrapper tbody tr')
  65  |     await expect(rows.first()).toBeVisible()
  66  |   })
  67  | 
  68  |   test('交付管理列表渲染', async ({ page }) => {
  69  |     await login(page)
  70  |     await navigate(page, '/prod/delivery')
  71  | 
  72  |     await waitTable(page)
  73  |     const rows = page.locator('.el-table__body-wrapper tbody tr')
  74  |     await expect(rows.first()).toBeVisible()
  75  |   })
  76  | 
  77  |   test('数据看板图表渲染', async ({ page }) => {
  78  |     await login(page)
  79  |     await navigate(page, '/eq-dashboard')
  80  | 
  81  |     // Check KPI metric cards
  82  |     await page.waitForTimeout(2000)
  83  |     const metricCards = page.locator('.metric-card')
  84  |     await expect(metricCards.first()).toBeVisible({ timeout: 10000 })
  85  | 
  86  |     // Check for ECharts canvas (stage distribution or hourly trend)
  87  |     const charts = page.locator('.chart-container canvas')
  88  |     const chartCount = await charts.count()
  89  |     expect(chartCount).toBeGreaterThanOrEqual(1)
  90  |   })
  91  | 
  92  |   test('异常: 错误密码登录', async ({ page }) => {
  93  |     await page.goto('/login')
  94  |     await page.waitForSelector('.login-card', { timeout: 10000 })
  95  | 
  96  |     // Fill wrong password
  97  |     await page.locator('.login-card input').first().fill('admin')
  98  |     await page.locator('.login-card input').nth(1).fill('wrongpassword')
  99  |     await page.locator('.login-card .el-button--primary').click()
  100 | 
  101 |     // Should show error message
  102 |     await page.waitForTimeout(1000)
  103 |     const errMsg = page.locator('.el-message--error')
  104 |     await expect(errMsg).toBeVisible({ timeout: 5000 })
  105 |   })
  106 | 
  107 |   test('异常: 空数据空状态', async ({ page }) => {
  108 |     await login(page)
  109 |     // Navigate to a page that may have no data
  110 |     await navigate(page, '/trace/exception')
  111 | 
  112 |     // Either the table renders or empty state shows
  113 |     await page.waitForTimeout(2000)
  114 |     const empty = page.locator('.el-empty')
  115 |     const table = page.locator('.el-table')
  116 |     const anyVisible = (await empty.isVisible().catch(() => false)) || (await table.isVisible().catch(() => false))
  117 |     expect(anyVisible).toBeTruthy()
  118 |   })
  119 | })
  120 | 
```