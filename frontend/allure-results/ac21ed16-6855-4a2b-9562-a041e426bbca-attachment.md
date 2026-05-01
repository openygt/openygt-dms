# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: print-management.spec.ts >> Flow 8: 打印管理 >> 打印日志列表
- Location: e2e/print-management.spec.ts:37:3

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
      - menuitem "生产执行" [ref=e18]:
        - generic [ref=e19] [cursor=pointer]:
          - img [ref=e21]
          - generic [ref=e24]: 生产执行
          - img [ref=e26]
      - menuitem "设备管理" [ref=e28]:
        - generic [ref=e29] [cursor=pointer]:
          - img [ref=e31]
          - generic [ref=e34]: 设备管理
          - img [ref=e36]
      - menuitem "工艺配置" [ref=e38]:
        - generic [ref=e39] [cursor=pointer]:
          - img [ref=e41]
          - generic [ref=e43]: 工艺配置
          - img [ref=e45]
      - menuitem "质量追溯" [ref=e47]:
        - generic [ref=e48] [cursor=pointer]:
          - img [ref=e50]
          - generic [ref=e52]: 质量追溯
          - img [ref=e54]
      - menuitem "数据分析" [ref=e56]:
        - generic [ref=e57] [cursor=pointer]:
          - img [ref=e59]
          - generic [ref=e61]: 数据分析
          - img [ref=e63]
      - menuitem "打印配置" [expanded] [ref=e65]:
        - generic [ref=e66] [cursor=pointer]:
          - img [ref=e68]
          - generic [ref=e70]: 打印配置
          - img [ref=e72]
        - menu [ref=e74]:
          - menuitem "标签模板" [ref=e75] [cursor=pointer]
          - menuitem "打印机管理" [ref=e76] [cursor=pointer]
          - menuitem "打印记录" [ref=e77] [cursor=pointer]
      - menuitem "基础数据" [ref=e78]:
        - generic [ref=e79] [cursor=pointer]:
          - img [ref=e81]
          - generic [ref=e85]: 基础数据
          - img [ref=e87]
      - menuitem "系统管理" [ref=e89]:
        - generic [ref=e90] [cursor=pointer]:
          - img [ref=e92]
          - generic [ref=e95]: 系统管理
          - img [ref=e97]
  - generic [ref=e99]:
    - generic [ref=e100]:
      - generic [ref=e101]: 打印记录
      - generic [ref=e102]:
        - 'button "当前主题: 跟随系统" [ref=e104] [cursor=pointer]':
          - img [ref=e106]
        - separator [ref=e109]
        - generic [ref=e110]: 当前用户：未知用户
        - button "退出" [ref=e111] [cursor=pointer]:
          - generic [ref=e112]: 退出
    - main [ref=e113]:
      - generic [ref=e114]:
        - generic [ref=e117]:
          - button "打印记录 打印记录" [ref=e118] [cursor=pointer]:
            - generic "打印记录" [ref=e119]:
              - img [ref=e121]
            - generic [ref=e124]: 打印记录
          - separator [ref=e125]
          - generic [ref=e126]: 查询历史打印任务及状态
        - generic [ref=e129]:
          - generic [ref=e130]:
            - generic [ref=e131]: 打印状态
            - generic [ref=e134] [cursor=pointer]:
              - generic:
                - combobox "打印状态" [ref=e136]
                - generic [ref=e137]: 全部
              - img [ref=e140]
          - generic [ref=e142]:
            - generic [ref=e143]: 设备编码
            - textbox "设备编码" [ref=e147]:
              - /placeholder: 请输入设备编码
          - button "查询" [ref=e150] [cursor=pointer]:
            - generic [ref=e151]: 查询
        - generic [ref=e154]:
          - generic [ref=e155]:
            - table [ref=e157]:
              - rowgroup [ref=e169]:
                - row "任务ID 设备编码 打印份数 任务状态 打印结果 打印机 重试次数 打印时间 创建时间" [ref=e170]:
                  - columnheader [ref=e171]
                  - columnheader "任务ID" [ref=e172]:
                    - generic [ref=e173]: 任务ID
                  - columnheader "设备编码" [ref=e174]:
                    - generic [ref=e175]: 设备编码
                  - columnheader "打印份数" [ref=e176]:
                    - generic [ref=e177]: 打印份数
                  - columnheader "任务状态" [ref=e178]:
                    - generic [ref=e179]: 任务状态
                  - columnheader "打印结果" [ref=e180]:
                    - generic [ref=e181]: 打印结果
                  - columnheader "打印机" [ref=e182]:
                    - generic [ref=e183]: 打印机
                  - columnheader "重试次数" [ref=e184]:
                    - generic [ref=e185]: 重试次数
                  - columnheader "打印时间" [ref=e186]:
                    - generic [ref=e187]: 打印时间
                  - columnheader "创建时间" [ref=e188]:
                    - generic [ref=e189]: 创建时间
            - generic [ref=e193]:
              - table:
                - rowgroup
              - generic [ref=e195]: 暂无数据
          - img [ref=e198]
```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test'
  2  | import { login, navigate, waitTable } from './helpers'
  3  | 
  4  | test.describe('Flow 8: 打印管理', () => {
  5  | 
  6  |   test('打印中心队列列表', async ({ page }) => {
  7  |     await login(page)
  8  |     await navigate(page, '/print-center')
  9  | 
  10 |     await waitTable(page)
  11 |     const rows = page.locator('.el-table__body-wrapper tbody tr')
  12 |     await expect(rows.first()).toBeVisible({ timeout: 5000 })
  13 | 
  14 |     // Verify status tags
  15 |     const tags = page.locator('.el-table .el-tag')
  16 |     await expect(tags.first()).toBeVisible({ timeout: 5000 })
  17 |   })
  18 | 
  19 |   test('标签模板列表', async ({ page }) => {
  20 |     await login(page)
  21 |     await navigate(page, '/print/template')
  22 | 
  23 |     await waitTable(page)
  24 |     const rows = page.locator('.el-table__body-wrapper tbody tr')
  25 |     await expect(rows.first()).toBeVisible({ timeout: 5000 })
  26 |   })
  27 | 
  28 |   test('打印机管理列表', async ({ page }) => {
  29 |     await login(page)
  30 |     await navigate(page, '/print/printer')
  31 | 
  32 |     await waitTable(page)
  33 |     const rows = page.locator('.el-table__body-wrapper tbody tr')
  34 |     await expect(rows.first()).toBeVisible({ timeout: 5000 })
  35 |   })
  36 | 
  37 |   test('打印日志列表', async ({ page }) => {
  38 |     await login(page)
  39 |     await navigate(page, '/print/log')
  40 | 
  41 |     await waitTable(page)
  42 |     const rows = page.locator('.el-table__body-wrapper tbody tr')
> 43 |     await expect(rows.first()).toBeVisible({ timeout: 5000 })
     |                                ^ Error: expect(locator).toBeVisible() failed
  44 |   })
  45 | 
  46 |   test('异常: 打印重试操作', async ({ page }) => {
  47 |     await login(page)
  48 |     await navigate(page, '/print-center')
  49 | 
  50 |     await waitTable(page)
  51 | 
  52 |     // Look for retry button
  53 |     const retryBtn = page.locator('.el-table .el-button').filter({ hasText: /重试/ })
  54 |     if (await retryBtn.first().isVisible().catch(() => false)) {
  55 |       await retryBtn.first().click()
  56 |       await page.waitForTimeout(1000)
  57 |     }
  58 |   })
  59 | })
  60 | 
```