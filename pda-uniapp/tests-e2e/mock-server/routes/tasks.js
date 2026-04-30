/**
 * PDA Mock Server - 任务路由
 */

const express = require('express')
const router = express.Router()
const store = require('../store')

// GET /api/v1/pda/tasks/recent - 最近处理任务
router.get('/recent', (req, res) => {
  const limit = parseInt(req.query.limit) || 20
  const sorted = [...store.db.tasks]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, limit)

  const data = sorted.map(t => ({
    taskId: t.taskId,
    barcode: t.barcode,
    patientName: t.patientName,
    status: t.status,
    statusName: store.TASK_STATUS[t.status]?.name,
    statusColor: store.TASK_STATUS[t.status]?.color,
    updatedAt: t.updatedAt || t.createdAt
  }))

  res.jsonOk(data)
})

// GET /api/v1/pda/tasks/:barcode - 扫码查询任务详情
router.get('/:barcode', (req, res) => {
  const { barcode } = req.params
  const brief = req.query.brief === 'true'

  const task = store.getTaskByBarcode(barcode)
  if (!task) {
    return res.jsonErr(404, '任务不存在')
  }

  if (brief) {
    return res.jsonOk({
      taskId: task.taskId,
      barcode: task.barcode,
      status: task.status,
      statusName: store.TASK_STATUS[task.status]?.name,
      patientName: task.patientName
    })
  }

  return res.jsonOk(store.buildTaskResponse(task))
})

// POST /api/v1/pda/tasks/:taskId/confirm - 工序确认
router.post('/:taskId/confirm', (req, res) => {
  const taskId = parseInt(req.params.taskId)
  const { stepType, remark, deviceCode, operatorId, operatorName } = req.body

  const operator = operatorId
    ? store.db.users.find(u => u.userId === operatorId)
    : { userId: 1001, userName: operatorName || '张三' }

  const result = store.confirmStep(taskId, stepType, operator, deviceCode, remark)

  if (!result.success) {
    return res.jsonErr(result.code, result.msg)
  }

  res.jsonOk(result.data)
})

// POST /api/v1/pda/tasks/:taskId/bind-device - 绑定设备
router.post('/:taskId/bind-device', (req, res) => {
  const taskId = parseInt(req.params.taskId)
  const { deviceCode, bindType } = req.body

  const task = store.getTask(taskId)
  if (!task) {
    return res.jsonErr(404, '任务不存在')
  }

  const device = store.db.devices.find(d => d.deviceCode === deviceCode)
  if (!device) {
    return res.jsonErr(404, '设备不存在')
  }

  // 校验设备类型是否匹配当前工序
  const currentStepType = store.STATUS_CURRENT_STEP[task.status]
  const stepDef = store.TASK_STEPS.find(s => s.value === currentStepType)
  if (stepDef && stepDef.needDevice) {
    const expectedType = currentStepType === 'START_DECOCT' ? 'DECOCT' : 'PACKAGE'
    if (device.deviceType !== expectedType) {
      return res.jsonErr(400, `当前工序需要${expectedType === 'DECOCT' ? '煎药机' : '包装机'}，不能绑定${device.deviceType === 'DECOCT' ? '煎药机' : '包装机'}`)
    }
  }

  task.bindDeviceId = device.deviceId
  task.bindDeviceCode = device.deviceCode
  task.bindDeviceName = device.deviceName
  task.updatedAt = new Date().toISOString()

  store.addOperationLog(
    { userId: task.operatorId, userName: task.operatorName },
    taskId,
    'DEVICE_BIND',
    { deviceCode, deviceName: device.deviceName, bindType }
  )

  res.jsonOk({
    deviceId: device.deviceId,
    deviceCode: device.deviceCode,
    deviceName: device.deviceName,
    deviceType: device.deviceType,
    bindTime: task.updatedAt
  })
})

// POST /api/v1/pda/tasks/:taskId/sign - 交接签字
router.post('/:taskId/sign', (req, res) => {
  const taskId = parseInt(req.params.taskId)
  const { signType, signImageBase64, handoverFrom, handoverTo, remark } = req.body

  const task = store.getTask(taskId)
  if (!task) {
    return res.jsonErr(404, '任务不存在')
  }

  const signId = store.db.signs.length + 1
  store.db.signs.push({
    id: signId,
    taskId: taskId,
    signType: signType || 'HANDOVER',
    signImageBase64: signImageBase64 || null,
    handoverFrom: handoverFrom || task.operatorName,
    handoverTo: handoverTo || null,
    remark: remark || '',
    createdAt: new Date().toISOString()
  })

  // 签字后任务状态变为已完成（如果是交接签字）
  if (task.status === 'INSPECTING') {
    task.status = 'COMPLETED'
    task.updatedAt = new Date().toISOString()
  }

  store.addOperationLog(
    { userId: task.operatorId, userName: task.operatorName },
    taskId,
    'SIGN_SUBMIT',
    { signType, handoverFrom, handoverTo }
  )

  res.jsonOk({
    signId: signId,
    taskId: taskId,
    status: task.status,
    createdAt: new Date().toISOString()
  })
})

// POST /api/v1/pda/tasks/:taskId/reprint - 重打印
router.post('/:taskId/reprint', (req, res) => {
  const taskId = parseInt(req.params.taskId)
  const { printType, printCount } = req.body

  const task = store.getTask(taskId)
  if (!task) {
    return res.jsonErr(404, '任务不存在')
  }

  if (task.status !== 'COMPLETED' && printType === 'HANDOVER') {
    return res.jsonErr(400, '该处方尚未完成，无法打印交接单')
  }

  const printJobId = store.db.reprintLogs.length + 1
  store.db.reprintLogs.push({
    id: printJobId,
    taskId: taskId,
    printType: printType || 'LABEL',
    printCount: printCount || 1,
    printedAt: new Date().toISOString(),
    status: 'SUCCESS'
  })

  store.addOperationLog(
    { userId: task.operatorId, userName: task.operatorName },
    taskId,
    'REPRINT',
    { printType, printCount }
  )

  res.jsonOk({
    printJobId: printJobId,
    status: 'PRINTING',
    message: `${printType === 'LABEL' ? '标签' : '交接单'}打印任务已下发`
  })
})

module.exports = router
