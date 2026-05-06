import { test, expect } from '@playwright/test'
import { DeviceListPage } from '../poms/DeviceListPage'

test.describe('设备列表管理', () => {
  test('页面加载显示设备表格', async ({ page }) => {
    const list = new DeviceListPage(page)
    await list.goto()
    await expect(list.deviceTable).toBeVisible()
  })

  test('搜索功能可用', async ({ page }) => {
    const list = new DeviceListPage(page)
    await list.goto()
    await list.search('EQ')
    await page.waitForTimeout(500)
  })

  test('分页组件存在', async ({ page }) => {
    const list = new DeviceListPage(page)
    await list.goto()
    await expect(list.pagination).toBeVisible()
  })
})
