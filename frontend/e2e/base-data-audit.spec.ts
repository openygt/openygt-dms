import { test, expect, Page } from '@playwright/test'

const BASE_URL = 'http://localhost:5175'
const ADMIN = { username: 'admin', password: 'admin123' }

async function login(page: Page) {
  await page.goto(`${BASE_URL}/login`)
  await page.locator('input[placeholder="用户名"]').fill(ADMIN.username)
  await page.locator('input[placeholder="密码"]').fill(ADMIN.password)
  await page.locator('button:has-text("登录")').click()
  await page.waitForURL('**/dashboard', { timeout: 15000 })
  await page.waitForTimeout(800)
}

const pages = [
  { path: '/hospitals', name: '医院管理', file: 'hospitals' },
  { path: '/base/department', name: '科室管理', file: 'department' },
  { path: '/base/doctor', name: '医师管理', file: 'doctor' },
  { path: '/base/medicine', name: '药材管理', file: 'medicine' },
  { path: '/base/finished-shelf', name: '成品货架管理', file: 'finished-shelf' },
  { path: '/schemes', name: '煎药方案', file: 'schemes' },
  { path: '/water-formulas', name: '加水公式', file: 'water-formulas' },
  { path: '/formula/package-spec', name: '包装规格', file: 'package-spec' },
  { path: '/toxic-medicine', name: '毒性药材管理', file: 'toxic-medicine' },
]

test.describe('基础数据模块可视化审计', () => {
  test.beforeEach(async ({ page }) => { await login(page) })

  for (const p of pages) {
    test(`${p.name}: 页面加载截图`, async ({ page }) => {
      await page.goto(`${BASE_URL}${p.path}`)
      await page.waitForLoadState('networkidle')
      await page.waitForTimeout(2000)
      
      await page.screenshot({ 
        path: `/tmp/base-data-${p.file}.png`, 
        fullPage: true 
      })
      
      const text = await page.locator('body').textContent()
      expect(text).toContain(p.name)
    })
  }
})
