// API 配置
const isH5 = typeof window !== 'undefined'
const API_BASE_URL = isH5 ? '/api/v1/pda' : 'http://47.95.216.32:8081/api/v1/pda'

export function isMockEnabled() {
  try {
    return typeof uni !== 'undefined' && uni.getStorageSync && uni.getStorageSync('pda_mock_env') === '1'
  } catch (e) {
    return false
  }
}

export const config = {
  baseUrl: API_BASE_URL,
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
