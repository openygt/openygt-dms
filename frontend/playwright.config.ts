import { defineConfig, devices } from '@playwright/test'
import { fileURLToPath } from 'url'
import path from 'path'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,  // Tests depend on mock server state
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,  // Sequential to avoid mock server conflicts
  reporter: [
    ['list'],
    ['html', { open: 'never' }],
    ['allure-playwright', { outputFolder: './allure-results' }],
  ],
  use: {
    baseURL: 'http://localhost:5176',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'on-first-retry',
  },

  projects: [
    {
      name: 'setup',
      testMatch: /auth\.setup\.ts/,
    },
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        storageState: 'e2e/.auth/user.json',
        viewport: { width: 1440, height: 900 },
      },
      dependencies: ['setup'],
    },
  ],

  webServer: [
    {
      command: 'npx tsx ../tests/mock-server/index.ts',
      port: 8082,
      reuseExistingServer: true,
      cwd: __dirname,
    },
    {
      command: 'npm run dev',
      url: 'http://localhost:5176',
      reuseExistingServer: true,
    },
  ],
})
