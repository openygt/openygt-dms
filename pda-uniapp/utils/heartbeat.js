import config from './config.js'
import { post } from './request.js'

/**
 * PDA 心跳保活管理
 * - 每 30s 发送一次心跳
 * - 应用隐藏时停止心跳，显示时恢复
 */

let heartbeatTimer = null
let isRunning = false

export function startHeartbeat() {
  if (isRunning) {
    console.log('Heartbeat already running')
    return
  }
  isRunning = true
  
  const doHeartbeat = async () => {
    if (!isRunning) return
    try {
      await post('/heartbeat', {})
      console.log('Heartbeat sent')
    } catch (e) {
      // 心跳失败不阻断，由后端超时检测
      console.warn('Heartbeat failed:', e.message)
    }
  }
  
  // 立即执行一次，然后定时
  doHeartbeat()
  heartbeatTimer = setInterval(doHeartbeat, config.heartbeatInterval)
}

export function stopHeartbeat() {
  isRunning = false
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
    console.log('Heartbeat stopped')
  }
}

export function isHeartbeatRunning() {
  return isRunning
}

export default {
  startHeartbeat,
  stopHeartbeat,
  isHeartbeatRunning
}
