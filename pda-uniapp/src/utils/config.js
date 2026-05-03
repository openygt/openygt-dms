// API 配置
// 开发环境用真实后端
const API_BASE_URL_DEV = 'http://47.95.216.32:8081/api/v1/pda'
// E2E 测试环境用 Mock Server（自动识别，也可手动切换）
const API_BASE_URL_MOCK = 'http://localhost:8082/api/v1/pda'

const isMockEnv = typeof uni !== 'undefined' && uni.getStorageSync && uni.getStorageSync('pda_mock_env') === '1'
const API_BASE_URL = isMockEnv ? API_BASE_URL_MOCK : API_BASE_URL_DEV

export const config = {
  baseUrl: API_BASE_URL,
  mockBaseUrl: API_BASE_URL_MOCK,
  heartbeatInterval: 30000,      // 心跳间隔 30s
  heartbeatTimeoutIdle: 120000,  // 空闲超时 120s
  heartbeatTimeoutTransport: 600000, // 搬运超时 600s
  photoMaxSize: 5 * 1024 * 1024, // 单张照片最大 5MB
  photoMaxCount: 3,              // 单次最多上传 3 张
  syncBatchSize: 50,             // 离线同步批量大小
  recentLimit: 20,               // 首页最近记录数量
  tokenKey: 'pda_token',
  userInfoKey: 'pda_user_info'
}

export default config
