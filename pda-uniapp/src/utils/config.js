// API 配置
const isH5 = typeof window !== 'undefined'
const API_BASE_URL_DEV = isH5 ? '/api/v1/pda' : 'http://47.95.216.32:8081/api/v1/pda'
// E2E 测试环境用 Mock Server（自动识别，也可手动切换）
const API_BASE_URL_MOCK = 'http://localhost:8082/api/v1/pda'

const isMockEnv = typeof uni !== 'undefined' && uni.getStorageSync && uni.getStorageSync('pda_mock_env') === '1'
const API_BASE_URL = isMockEnv ? API_BASE_URL_MOCK : API_BASE_URL_DEV

export const config = {
  baseUrl: API_BASE_URL,
  mockBaseUrl: API_BASE_URL_MOCK,
  heartbeatInterval: 30000,
  heartbeatTimeoutIdle: 120000,
  heartbeatTimeoutTransport: 600000,
  photoMaxSize: 5 * 1024 * 1024,
  photoMaxCount: 3,
  syncBatchSize: 50,
  recentLimit: 20,
  tokenKey: 'pda_token',
  userInfoKey: 'pda_user_info'
}

export default config
