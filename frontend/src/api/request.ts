import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

function applyAuthorizationHeader(headers: any, token: string) {
  if (!headers || !token) return
  if (typeof headers.set === 'function') {
    headers.set('Authorization', 'Bearer ' + token)
    return
  }
  headers.Authorization = 'Bearer ' + token
}

export function syncRequestAuthorization(token?: string) {
  const auth = token ? 'Bearer ' + token : ''
  if (auth) {
    request.defaults.headers.common.Authorization = auth
  } else {
    delete request.defaults.headers.common.Authorization
  }
}

syncRequestAuthorization(localStorage.getItem('token') || '')

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers = config.headers || {}
    applyAuthorizationHeader(config.headers, token)
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data && data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(data)
    }
    return data
  },
  (err) => {
    if (err.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      useUserStore().logout()
    } else {
      const body = err.response?.data
      const msg =
        typeof body?.message === 'string'
          ? body.message
          : err.response?.status
            ? `请求失败 (${err.response.status})`
            : err.message || '网络错误'
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  }
)

export default request
