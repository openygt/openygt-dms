// PDA API 配置
// 优先使用环境变量 VITE_API_BASE_URL，否则使用默认值
const isH5 = typeof window !== 'undefined'

// H5 开发环境通过 Vite proxy 转发，生产环境由 Nginx 统一代理
const API_BASE_URL_DEV = isH5 ? '/api/v1/pda' : '/api/v1/pda'
const API_BASE_URL_MOCK = 'http://localhost:8082/api/v1/pda'

const isMockEnv = typeof uni !== 'undefined' && uni.getStorageSync && uni.getStorageSync('pda_mock_env') === '1'
const API_BASE_URL = isMockEnv ? API_BASE_URL_MOCK :
    (import.meta.env.VITE_API_BASE_URL || API_BASE_URL_DEV)

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
