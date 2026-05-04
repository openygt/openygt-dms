import config from './config.js'
import { getQueue, setQueue, removeFromQueue, updateQueueItem, getPhotoQueue, setPhotoQueue, removePhotoFromQueue } from './storage.js'
import { post, upload } from './request.js'

/**
 * 离线数据同步管理
 * - 网络恢复时自动批量提交
 * - 单条失败重试 3 次后丢弃
 */

let isSyncing = false

/**
 * 执行批量同步
 */
export async function syncAll() {
  if (isSyncing) {
    console.log('Sync in progress, skip')
    return
  }
  
  isSyncing = true
  try {
    await syncOperations()
    await syncPhotos()
  } finally {
    isSyncing = false
  }
}

/**
 * 同步操作队列
 */
async function syncOperations() {
  const queue = getQueue()
  if (queue.length === 0) return
  
  const batch = queue.slice(0, config.syncBatchSize)
  console.log(`Syncing ${batch.length} operations...`)
  
  for (const item of batch) {
    try {
      await post(item.url, item.data)
      removeFromQueue(item.id)
      console.log('Synced operation:', item.id)
    } catch (e) {
      item.retryCount++
      if (item.retryCount >= 3) {
        console.warn('Operation failed after 3 retries, removed:', item.id)
        removeFromQueue(item.id)
      } else {
        updateQueueItem(item)
        console.warn('Operation sync failed, retry count:', item.retryCount, item.id)
      }
    }
  }
  
  const remaining = getQueue()
  if (remaining.length > 0) {
    uni.showToast({ title: `还有 ${remaining.length} 条待同步`, icon: 'none' })
  } else {
    uni.showToast({ title: '同步完成', icon: 'success' })
  }
}

/**
 * 同步照片队列
 * 先上传文件到服务器，再保存元数据
 */
async function syncPhotos() {
  const queue = getPhotoQueue()
  if (queue.length === 0) return

  for (const item of queue) {
    try {
      // 上传文件
      const uploadRes = await upload('/file/upload', item.path, {
        taskId: item.taskId,
        photoType: item.photoType || 'REVIEW',
        remark: item.remark || ''
      })
      // 保存元数据
      await post('/photo/upload', {
        taskId: item.taskId,
        photoUrl: uploadRes.url,
        photoType: item.photoType || 'REVIEW',
        fileSize: uploadRes.size || 0,
        remark: item.remark
      })
      removePhotoFromQueue(item.id)
      console.log('Synced photo:', item.id)
    } catch (e) {
      item.retryCount++
      if (item.retryCount >= 3) {
        console.warn('Photo failed after 3 retries, removed:', item.id)
        removePhotoFromQueue(item.id)
      } else {
        console.warn('Photo sync failed, retry count:', item.retryCount, item.id)
      }
    }
  }
}

/**
 * 监听网络状态，自动触发同步
 */
export function watchNetwork() {
  uni.onNetworkStatusChange((res) => {
    if (res.isConnected) {
      console.log('Network connected, trigger sync')
      syncAll()
    }
  })
}

/**
 * 手动添加操作到队列（供 request.js 使用）
 */
export function addToQueue(operation) {
  const queue = getQueue()
  queue.push({
    id: 'pda_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6),
    url: operation.url,
    method: operation.method,
    data: operation.data,
    header: operation.header,
    timestamp: Date.now(),
    retryCount: 0
  })
  setQueue(queue)
}

export default {
  syncAll,
  watchNetwork,
  addToQueue
}
