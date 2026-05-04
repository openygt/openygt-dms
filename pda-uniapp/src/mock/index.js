import { isMockEnabled } from '../utils/config.js'
import { MOCK_USERS } from './data/users.js'
import { authHandlers } from './handlers/auth.js'
import { dashboardHandlers } from './handlers/dashboard.js'
import { taskHandlers } from './handlers/tasks.js'
import { shelfHandlers } from './handlers/shelves.js'
import { patientHandlers } from './handlers/patient.js'
import { deviceHandlers } from './handlers/device.js'
import { reprintHandlers } from './handlers/reprint.js'
import { processHandlers } from './handlers/process.js'
import { commonHandlers } from './handlers/common.js'

const handlerMap = [
  // Auth
  { method: 'POST', pattern: '/auth/login', handler: authHandlers.login },
  { method: 'POST', pattern: '/auth/scan-login', handler: authHandlers.scanLogin },
  { method: 'POST', pattern: '/auth/logout', handler: authHandlers.logout },
  { method: 'POST', pattern: '/change-password', handler: authHandlers.changePassword },

  // Dashboard
  { method: 'GET', pattern: '/dashboard/stats', handler: dashboardHandlers.stats },
  { method: 'GET', pattern: '/dashboard/task-status-count', handler: dashboardHandlers.taskStatusCount },

  // Tasks
  { method: 'GET', pattern: /^\/tasks\/(\d+)\/bind-device$/, handler: deviceHandlers.bind },
  { method: 'POST', pattern: /^\/tasks\/(\d+)\/bind-device$/, handler: deviceHandlers.bind },
  { method: 'POST', pattern: /^\/tasks\/(\d+)\/status$/, handler: processHandlers.advanceStatus },
  { method: 'GET', pattern: /^\/tasks\/(\d+)\/reprint-info$/, handler: taskHandlers.getReprintInfo },
  { method: 'GET', pattern: /^\/tasks\/(\d+)$/, handler: taskHandlers.getByBarcode },
  { method: 'POST', pattern: /^\/tasks\/(\d+)\/reprint$/, handler: reprintHandlers.reprint },
  { method: 'GET', pattern: '/tasks/recent', handler: taskHandlers.recent },
  { method: 'GET', pattern: '/tasks/today-pending', handler: taskHandlers.todayPending },
  { method: 'GET', pattern: '/tasks', handler: taskHandlers.list },

  // Scan
  { method: 'POST', pattern: '/scan', handler: unifiedScan },

  // Task by barcode
  { method: 'GET', pattern: /^\/task\/([^\/]+)\/reprint-info$/, handler: taskHandlers.getReprintInfo },
  { method: 'GET', pattern: /^\/task\/([^\/]+)\/temperature-curve$/, handler: taskHandlers.temperatureCurve },
  { method: 'GET', pattern: /^\/task\/([^\/]+)$/, handler: taskHandlers.getByBarcode },

  // Task confirm
  { method: 'POST', pattern: '/task/confirm', handler: processHandlers.confirm },
  { method: 'POST', pattern: '/task/reprint', handler: reprintHandlers.reprint },

  // Shelf
  { method: 'GET', pattern: '/shelves', handler: shelfHandlers.list },
  { method: 'GET', pattern: /^\/bag\/([^\/]+)$/, handler: shelfHandlers.getBag },
  { method: 'POST', pattern: '/shelf/put-on', handler: shelfHandlers.putOn },
  { method: 'POST', pattern: '/shelf/take-off', handler: shelfHandlers.takeOff },

  // Patient
  { method: 'GET', pattern: '/patient/query', handler: patientHandlers.queryByBarcode },
  { method: 'GET', pattern: '/patient/query-by-phone', handler: patientHandlers.queryByPhone },
  { method: 'POST', pattern: '/patient/verify-code', handler: patientHandlers.verifyCode },

  // Weight
  { method: 'POST', pattern: '/weight/record', handler: processHandlers.weightRecord },
  { method: 'GET', pattern: /^\/weight\/(\d+)$/, handler: processHandlers.getWeight },

  // QC
  { method: 'GET', pattern: /^\/qc\/(\d+)$/, handler: processHandlers.getQc },
  { method: 'POST', pattern: '/qc/submit', handler: processHandlers.qcSubmit },

  // Process
  { method: 'GET', pattern: /^\/process\/(\d+)$/, handler: processHandlers.getProcessSteps },

  // Common
  { method: 'GET', pattern: '/config/version', handler: commonHandlers.version },
  { method: 'POST', pattern: '/heartbeat', handler: commonHandlers.heartbeat },
  { method: 'POST', pattern: '/file/upload', handler: commonHandlers.fileUpload },
  { method: 'POST', pattern: '/photo/upload', handler: commonHandlers.photoUpload }
]

function unifiedScan(params) {
  const task = MOCK_USERS.find(u => u.userCode === 'admin')
  const barcode = params.barcode || ''
  const taskData = task ? {
    taskId: 1001,
    taskStatus: 'PENDING',
    barcode,
    display: { statusName: '待泡药', statusColor: '#999', description: '等待开始泡药' },
    actionName: '开始泡药'
  } : {}
  return { code: 200, message: 'success', data: taskData }
}

function findMockHandler(url, method) {
  for (const entry of handlerMap) {
    if (entry.method !== method) continue
    if (typeof entry.pattern === 'string' && entry.pattern === url) {
      return entry.handler
    }
    if (entry.pattern instanceof RegExp) {
      const match = url.match(entry.pattern)
      if (match) {
        const original = entry.handler
        return (params, ctx) => original(params, { ...ctx, pathParams: match.slice(1) })
      }
    }
  }
  return null
}

export function handleMockRequest(options) {
  return new Promise((resolve, reject) => {
    setTimeout(async () => {
      try {
        const path = options.url.startsWith('/') ? options.url : '/' + options.url
        const handler = findMockHandler(path, options.method || 'GET')
        if (handler) {
          const delay = 200 + Math.random() * 400
          await new Promise(r => setTimeout(r, delay))
          const result = handler(options.data || {}, { url: path, method: options.method })
          if (result && result.code === 200) {
            resolve(result.data)
          } else {
            const errMsg = result && (result.message || '请求失败')
            uni.showToast({ title: errMsg, icon: 'none' })
            reject(new Error(errMsg))
          }
        } else {
          reject(new Error(`Mock: 未找到处理器 ${options.method} ${path}`))
        }
      } catch (e) {
        reject(e)
      }
    }, 50)
  })
}

export function initMockEnvironment() {
  const token = uni.getStorageSync('pda_token')
  if (!token) {
    const admin = MOCK_USERS[0]
    uni.setStorageSync('pda_token', 'mock_token_' + admin.userCode + '_' + Date.now())
    uni.setStorageSync('pda_user_info', {
      userId: admin.userId,
      userCode: admin.userCode,
      userName: admin.userName,
      deviceCode: admin.deviceCode,
      recordId: Date.now(),
      permissions: admin.permissions
    })
  }
}

export function mockUpload(options) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ url: '/mock/uploads/file_' + Date.now() + '.jpg', size: 12345 })
    }, 300)
  })
}
