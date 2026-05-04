import config from './config.js'

/**
 * 统一 HTTP 请求封装
 * - 自动附加 JWT Token
 * - 401 时自动跳转登录页
 * - POST 请求失败时缓存到离线队列
 */

function request(options) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync(config.tokenKey)
    
    uni.request({
      url: options.url.startsWith('http') ? options.url : config.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        ...(options.header || {})
      },
      timeout: options.timeout || 10000,
      success: (res) => {
        if (res.statusCode === 200) {
          const data = res.data
          if (data && data.code === 200) {
            resolve(data.data)
          } else {
            const errMsg = data && (data.message || data.msg) ? (data.message || data.msg) : '请求失败'
            uni.showToast({ title: errMsg, icon: 'none' })
            reject(new Error(errMsg))
          }
        } else if (res.statusCode === 401) {
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          uni.removeStorageSync(config.tokenKey)
          uni.removeStorageSync(config.userInfoKey)
          setTimeout(() => {
            uni.reLaunch({ url: '/pages/login/index' })
          }, 1500)
          reject(new Error('Unauthorized'))
        } else {
          const errMsg = `HTTP ${res.statusCode}`
          uni.showToast({ title: errMsg, icon: 'none' })
          reject(new Error(errMsg))
        }
      },
      fail: (err) => {
        // 网络失败时，POST 请求缓存到离线队列
        if (options.method === 'POST' || options.method === 'post') {
          import('./sync.js').then(({ addToQueue }) => {
            addToQueue({
              url: options.url,
              method: options.method,
              data: options.data,
              header: options.header
            })
          })
          uni.showToast({ title: '网络异常，已缓存到本地', icon: 'none' })
        } else {
          uni.showToast({ title: '网络请求失败', icon: 'none' })
        }
        reject(err)
      }
    })
  })
}

export const get = (url, params = {}) => request({ url, method: 'GET', data: params })
export const post = (url, data = {}) => request({ url, method: 'POST', data })

/**
 * 上传文件（multipart/form-data）
 */
export function upload(url, filePath, formData = {}) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync(config.tokenKey)
    const uploadUrl = url.startsWith('http') ? url : config.baseUrl + url
    uni.uploadFile({
      url: uploadUrl,
      filePath,
      name: 'file',
      formData,
      header: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          try {
            const data = JSON.parse(res.data)
            if (data.code === 200) {
              resolve(data.data)
            } else {
              reject(new Error(data.message || '上传失败'))
            }
          } catch (e) {
            reject(new Error('上传响应解析失败'))
          }
        } else if (res.statusCode === 401) {
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          uni.removeStorageSync(config.tokenKey)
          uni.removeStorageSync(config.userInfoKey)
          setTimeout(() => uni.reLaunch({ url: '/pages/login/index' }), 1500)
          reject(new Error('Unauthorized'))
        } else {
          reject(new Error(`HTTP ${res.statusCode}`))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常，文件上传失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

export default request
