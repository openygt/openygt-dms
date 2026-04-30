/**
 * PDA 移动端 E2E Playwright 配置
 *
 * 使用 Mock Server 作为后端，无需启动真实 Java 服务
 * Mock Server 启动命令：npm run mock --prefix tests-e2e/mock-server
 */
module.exports = {
  testDir: './specs',
  outputDir: './test-results',
  reporter: [
    ['html', { open: 'never' }],
    ['list']
  ],
  use: {
    baseURL: 'http://localhost:5173',
    viewport: { width: 390, height: 844 },
    deviceScaleFactor: 3,
    userAgent: 'Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    hasTouch: true,
    isMobile: true,
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai',
    video: 'retain-on-failure',
    screenshot: 'only-on-failure',
    trace: 'on-first-retry',
    actionTimeout: 10000,
    navigationTimeout: 15000
  },
  projects: [
    {
      name: 'chromium-mobile',
      use: { browserName: 'chromium' }
    },
    {
      name: 'firefox-mobile',
      use: { browserName: 'firefox' }
    },
    {
      name: 'webkit-mobile',
      use: { browserName: 'webkit' }
    }
  ],
  workers: process.env.CI ? 2 : 4,
  retries: process.env.CI ? 1 : 0,
  // 全局前置：启动 Mock Server
  globalSetup: require.resolve('./global-setup.js'),
  // 全局后置：关闭 Mock Server
  globalTeardown: require.resolve('./global-teardown.js')
}
