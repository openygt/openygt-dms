/**
 * PDA Mock Server - 测试控制路由
 *
 * 仅供 E2E 测试和开发调试使用
 */

const express = require('express')
const router = express.Router()
const store = require('../store')

// POST /__control/reset - 重置所有数据
router.post('/reset', (req, res) => {
  const result = store.resetAll()
  res.json(result)
})

// POST /__control/offline - 开启离线模式
router.post('/offline', (req, res) => {
  store.setOfflineMode(true)
  res.json({ success: true, offlineMode: true })
})

// POST /__control/online - 关闭离线模式
router.post('/online', (req, res) => {
  store.setOfflineMode(false)
  res.json({ success: true, offlineMode: false })
})

// POST /__control/latency - 设置延迟
router.post('/latency', (req, res) => {
  const { ms } = req.body
  store.setLatency(ms || 0)
  res.json({ success: true, latencyMs: ms || 0 })
})

// POST /__control/failure-rate - 设置随机失败率
router.post('/failure-rate', (req, res) => {
  const { rate } = req.body
  store.setFailureRate(rate || 0)
  res.json({ success: true, failureRate: rate || 0 })
})

// GET /__control/status - 查看当前模拟状态
router.get('/status', (req, res) => {
  res.json({
    success: true,
    offlineMode: store.offlineMode(),
    latencyMs: store.latencyMs || 0,
    failureRate: store.failureRate || 0,
    stats: {
      users: store.db.users.length,
      devices: store.db.devices.length,
      tasks: store.db.tasks.length,
      stepLogs: store.db.stepLogs.length,
      photos: store.db.photos.length,
      signs: store.db.signs.length,
      operationLogs: store.db.operationLogs.length,
      loginRecords: store.db.loginRecords.length,
      reprintLogs: store.db.reprintLogs.length
    },
    tasks: store.db.tasks.map(t => ({
      taskId: t.taskId,
      barcode: t.barcode,
      patientName: t.patientName,
      status: t.status,
      statusName: store.TASK_STATUS[t.status]?.name,
      currentStep: t.currentStep,
      bindDevice: t.bindDeviceName,
      operator: t.operatorName
    }))
  })
})

// GET /__control/tasks/:taskId - 查看单个任务详情
router.get('/tasks/:taskId', (req, res) => {
  const taskId = parseInt(req.params.taskId)
  const task = store.getTask(taskId)
  if (!task) return res.status(404).json({ success: false, msg: 'Task not found' })

  res.json({
    success: true,
    task: store.buildTaskResponse(task),
    stepLogs: store.db.stepLogs.filter(l => l.taskId === taskId),
    photos: store.db.photos.filter(p => p.taskId === taskId),
    signs: store.db.signs.filter(s => s.taskId === taskId)
  })
})

// POST /__control/tasks/:taskId/status - 强制设置任务状态（用于测试状态分支）
router.post('/tasks/:taskId/status', (req, res) => {
  const taskId = parseInt(req.params.taskId)
  const { status } = req.body
  const task = store.getTask(taskId)
  if (!task) return res.status(404).json({ success: false, msg: 'Task not found' })

  if (!store.TASK_STATUS[status]) {
    return res.status(400).json({ success: false, msg: 'Invalid status' })
  }

  task.status = status
  task.currentStep = store.STATUS_CURRENT_STEP[status]
  task.updatedAt = new Date().toISOString()

  res.json({ success: true, task: store.buildTaskResponse(task) })
})

module.exports = router
