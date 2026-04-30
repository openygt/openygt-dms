/**
 * 本地存储封装
 * - 统一使用 uni.setStorageSync / uni.getStorageSync
 * - 支持对象自动序列化
 */

const QUEUE_KEY = 'pda_offline_queue'
const PHOTO_QUEUE_KEY = 'pda_photo_queue'

export function setItem(key, value) {
  try {
    uni.setStorageSync(key, value)
  } catch (e) {
    console.error('Storage set error:', e)
  }
}

export function getItem(key, defaultValue = null) {
  try {
    const value = uni.getStorageSync(key)
    return value !== undefined && value !== '' ? value : defaultValue
  } catch (e) {
    console.error('Storage get error:', e)
    return defaultValue
  }
}

export function removeItem(key) {
  try {
    uni.removeStorageSync(key)
  } catch (e) {
    console.error('Storage remove error:', e)
  }
}

export function clear() {
  try {
    uni.clearStorageSync()
  } catch (e) {
    console.error('Storage clear error:', e)
  }
}

// 离线操作队列
export function getQueue() {
  return getItem(QUEUE_KEY, [])
}

export function setQueue(queue) {
  setItem(QUEUE_KEY, queue)
}

export function addToQueue(operation) {
  const queue = getQueue()
  queue.push({
    id: generateId(),
    ...operation,
    timestamp: Date.now(),
    retryCount: 0
  })
  setQueue(queue)
}

export function removeFromQueue(id) {
  const queue = getQueue().filter(item => item.id !== id)
  setQueue(queue)
}

export function updateQueueItem(item) {
  const queue = getQueue()
  const idx = queue.findIndex(q => q.id === item.id)
  if (idx >= 0) {
    queue[idx] = item
    setQueue(queue)
  }
}

// 照片队列
export function getPhotoQueue() {
  return getItem(PHOTO_QUEUE_KEY, [])
}

export function setPhotoQueue(queue) {
  setItem(PHOTO_QUEUE_KEY, queue)
}

export function addPhotoToQueue(photoItem) {
  const queue = getPhotoQueue()
  queue.push({
    id: generateId(),
    ...photoItem,
    timestamp: Date.now(),
    retryCount: 0
  })
  setPhotoQueue(queue)
}

export function removePhotoFromQueue(id) {
  const queue = getPhotoQueue().filter(item => item.id !== id)
  setPhotoQueue(queue)
}

function generateId() {
  return 'pda_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6)
}

export default {
  setItem,
  getItem,
  removeItem,
  clear,
  getQueue,
  setQueue,
  addToQueue,
  removeFromQueue,
  updateQueueItem,
  getPhotoQueue,
  setPhotoQueue,
  addPhotoToQueue,
  removePhotoFromQueue
}
