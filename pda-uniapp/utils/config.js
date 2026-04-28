// API 配置
const API_BASE_URL = 'http://localhost:8080/api/v1/pda'

export const config = {
  baseUrl: API_BASE_URL,
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
