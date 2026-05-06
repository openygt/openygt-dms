import { test, expect } from '@playwright/test'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const screenshotsDir = path.join(__dirname, '../../tests/e2e-records/menu-audit-screenshots')

// 设置全局超时
test.setTimeout(120000)

// 确保截图目录存在
if (!fs.existsSync(screenshotsDir)) {
  fs.mkdirSync(screenshotsDir, { recursive: true })
}

// 主要菜单页面配置（精简版）
const mainPages = [
  { path: '/dashboard', name: '生产看板', category: '核心业务' },
  { path: '/tasks', name: '煎药任务', category: '核心业务' },
  { path: '/prescriptions', name: '处方管理', category: '核心业务' },
  { path: '/devices', name: '设备台账', category: '设备管理' },
  { path: '/device-monitor', name: '设备监控', category: '设备管理' },
  { path: '/alarms', name: '告警中心', category: '监控告警' },
  { path: '/quality', name: '质量检验', category: '质量管理' },
  { path: '/capacity', name: '产能统计', category: '数据报表' },
  { path: '/users', name: '人员管理', category: '系统管理' },
  { path: '/roles', name: '权限管理', category: '系统管理' },
  { path: '/hospitals', name: '医院管理', category: '基础数据' },
  { path: '/schemes', name: '煎药方案', category: '基础数据' },
]

test.describe('菜单页面审核', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login')
    await page.waitForLoadState('domcontentloaded')

    // 填写登录表单
    await page.locator('input[placeholder="用户名"]').fill('admin')
    await page.locator('input[placeholder="密码"]').fill('admin123')

    // 点击登录按钮（按钮没有 type="submit"，使用文本匹配）
    await page.locator('button:has-text("登录")').click()

    // 等待跳转到首页
    await page.waitForURL('**/dashboard', { timeout: 15000 })
  })

  for (const pageInfo of mainPages) {
    test(`截图: ${pageInfo.category} - ${pageInfo.name}`, async ({ page }) => {
      // 访问页面
      await page.goto(pageInfo.path)
      await page.waitForLoadState('domcontentloaded')

      // 等待页面加载完成
      await page.waitForTimeout(1000)

      // 截图
      const screenshotPath = path.join(
        screenshotsDir,
        `${pageInfo.category}`,
        `${pageInfo.name.replace(/\//g, '-')}.png`
      )

      // 确保分类目录存在
      const categoryDir = path.dirname(screenshotPath)
      if (!fs.existsSync(categoryDir)) {
        fs.mkdirSync(categoryDir, { recursive: true })
      }

      await page.screenshot({
        path: screenshotPath,
        fullPage: true
      })

      // 验证页面标题
      const title = await page.title()
      expect(title).toContain('智能煎药管理系统')
    })
  }
})
