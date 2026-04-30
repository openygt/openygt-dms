/**
 * PDA Mock Server - 内存数据仓库 + 状态机引擎
 *
 * 设计说明：
 * - 所有数据保存在内存中，重启即重置
 * - 支持 resetAll() 一键恢复初始测试数据
 * - 内置状态机引擎，处理工序确认时的状态流转
 * - 支持离线模式模拟（延迟响应/随机失败）
 */

// ──────────────────────────────────────────────────────────
// 1. 状态机定义（与详设文档一致）
// ──────────────────────────────────────────────────────────
const TASK_STATUS = {
  PENDING:    { code: 'PENDING',    name: '待处理',   color: '#999',    icon: '⏳', next: 'SOAKING' },
  SOAKING:    { code: 'SOAKING',    name: '泡药中',   color: '#2196f3', icon: '💧', next: 'SOAKED' },
  SOAKED:     { code: 'SOAKED',     name: '已泡药',   color: '#64b5f6', icon: '✓',  next: 'DECOCTING' },
  DECOCTING:  { code: 'DECOCTING',  name: '煎药中',   color: '#ff9800', icon: '🔥', next: 'DECOCTED' },
  DECOCTED:   { code: 'DECOCTED',   name: '已煎药',   color: '#ffb74d', icon: '✓',  next: 'POURING' },
  POURING:    { code: 'POURING',    name: '出液中',   color: '#9c27b0', icon: '🏺', next: 'POURED' },
  POURED:     { code: 'POURED',     name: '已出液',   color: '#ba68c8', icon: '✓',  next: 'PACKAGING' },
  PACKAGING:  { code: 'PACKAGING',  name: '包装中',   color: '#795548', icon: '📦', next: 'PACKAGED' },
  PACKAGED:   { code: 'PACKAGED',   name: '已包装',   color: '#a1887f', icon: '✓',  next: 'LABELING' },
  LABELING:   { code: 'LABELING',   name: '贴标中',   color: '#607d8b', icon: '🏷', next: 'LABELED' },
  LABELED:    { code: 'LABELED',    name: '已贴标',   color: '#78909c', icon: '✓',  next: 'INSPECTING' },
  INSPECTING: { code: 'INSPECTING', name: '质检中',   color: '#e91e63', icon: '🔍', next: 'COMPLETED' },
  COMPLETED:  { code: 'COMPLETED',  name: '已完成',   color: '#4caf50', icon: '✅', next: null },
  CANCELLED:  { code: 'CANCELLED',  name: '已取消',   color: '#9e9e9e', icon: '🚫', next: null }
}

// 工序步骤定义
const TASK_STEPS = [
  { value: 'START_SOAK',    label: '开始泡药',   group: 'SOAK',     needDevice: false, needPhoto: false },
  { value: 'END_SOAK',      label: '结束泡药',   group: 'SOAK',     needDevice: false, needPhoto: false },
  { value: 'START_DECOCT',  label: '开始煎药',   group: 'DECOCT',   needDevice: true,  needPhoto: false },
  { value: 'END_DECOCT',    label: '结束煎药',   group: 'DECOCT',   needDevice: false, needPhoto: false },
  { value: 'START_POUR',    label: '开始出液',   group: 'POUR',     needDevice: false, needPhoto: false },
  { value: 'END_POUR',      label: '结束出液',   group: 'POUR',     needDevice: false, needPhoto: false },
  { value: 'START_PACKAGE', label: '开始包装',   group: 'PACKAGE',  needDevice: true,  needPhoto: false },
  { value: 'END_PACKAGE',   label: '结束包装',   group: 'PACKAGE',  needDevice: false, needPhoto: false },
  { value: 'LABEL_CONFIRM', label: '贴标确认',   group: 'LABEL',    needDevice: false, needPhoto: false },
  { value: 'INSPECT_PASS',  label: '质检通过',   group: 'INSPECT',  needDevice: false, needPhoto: true  }
]

// stepType → status 映射（确认某步骤后，任务进入什么状态）
const STEP_TO_STATUS = {
  'START_SOAK':    'SOAKING',
  'END_SOAK':      'SOAKED',
  'START_DECOCT':  'DECOCTING',
  'END_DECOCT':    'DECOCTED',
  'START_POUR':    'POURING',
  'END_POUR':      'POURED',
  'START_PACKAGE': 'PACKAGING',
  'END_PACKAGE':   'PACKAGED',
  'LABEL_CONFIRM': 'LABELED',
  'INSPECT_PASS':  'COMPLETED'
}

// status → 当前可确认的 stepType
const STATUS_CURRENT_STEP = {
  'PENDING':    'START_SOAK',
  'SOAKING':    'END_SOAK',
  'SOAKED':     'START_DECOCT',
  'DECOCTING':  'END_DECOCT',
  'DECOCTED':   'START_POUR',
  'POURING':    'END_POUR',
  'POURED':     'START_PACKAGE',
  'PACKAGING':  'END_PACKAGE',
  'PACKAGED':   'LABEL_CONFIRM',
  'LABELING':   'INSPECT_PASS',
  'INSPECTING': null, // 质检中已完成，等待交接签字
  'COMPLETED':  null,
  'CANCELLED':  null
}

// ──────────────────────────────────────────────────────────
// 2. 初始测试数据
// ──────────────────────────────────────────────────────────
const INITIAL_USERS = [
  { userId: 1001, userCode: 'EMP001', userName: '张三', password: 'admin123', barcode: 'EMP2024001', role: 'decoctor', permissions: ['task:scan','task:confirm','device:bind','photo:upload'] },
  { userId: 1002, userCode: 'EMP002', userName: '李四', password: 'packer123', barcode: 'EMP2024002', role: 'packer', permissions: ['task:scan','task:confirm','device:bind','handover:sign'] },
  { userId: 1003, userCode: 'EMP003', userName: '王五', password: 'inspect123', barcode: 'EMP2024003', role: 'inspector', permissions: ['task:scan','task:confirm','photo:upload','handover:sign'] },
  { userId: 1004, userCode: 'EMP004', userName: '赵六', password: 'leader123', barcode: 'EMP2024004', role: 'leader', permissions: ['*'] }
]

const INITIAL_DEVICES = [
  { deviceId: 1, deviceCode: 'JYJ-001', deviceName: '煎药机-01', deviceType: 'DECOCT', status: 'NORMAL' },
  { deviceId: 2, deviceCode: 'JYJ-002', deviceName: '煎药机-02', deviceType: 'DECOCT', status: 'NORMAL' },
  { deviceId: 3, deviceCode: 'BZB-001', deviceName: '包装机-01', deviceType: 'PACKAGE', status: 'NORMAL' },
  { deviceId: 4, deviceCode: 'BZB-002', deviceName: '包装机-02', deviceType: 'PACKAGE', status: 'MAINTENANCE' }
]

const INITIAL_TASKS = [
  {
    taskId: 1001, barcode: 'YP20250430001', status: 'PENDING', patientName: '张三', patientSex: 1, patientAge: 45,
    prescriptionNumber: 'CF20250430001', hospitalName: '测试中医院', prescriptionName: '感冒清热方',
    repetition: 7, currentStep: null, bindDeviceId: null, bindDeviceCode: null, bindDeviceName: null,
    operatorId: null, operatorName: null, medicines: [
      { name: '黄芪', dosage: 15, unit: 'g' },
      { name: '当归', dosage: 10, unit: 'g' }
    ],
    createdAt: '2025-04-30T08:00:00'
  },
  {
    taskId: 1002, barcode: 'YP20250430002', status: 'SOAKING', patientName: '李四', patientSex: 0, patientAge: 32,
    prescriptionNumber: 'CF20250430002', hospitalName: '测试中医院', prescriptionName: '四物汤',
    repetition: 5, currentStep: 'START_SOAK', bindDeviceId: null, bindDeviceCode: null, bindDeviceName: null,
    operatorId: 1001, operatorName: '张三', medicines: [
      { name: '熟地黄', dosage: 15, unit: 'g' },
      { name: '白芍', dosage: 10, unit: 'g' },
      { name: '当归', dosage: 10, unit: 'g' },
      { name: '川芎', dosage: 6, unit: 'g' }
    ],
    createdAt: '2025-04-30T07:30:00'
  },
  {
    taskId: 1003, barcode: 'YP20250430003', status: 'DECOCTING', patientName: '王五', patientSex: 1, patientAge: 28,
    prescriptionNumber: 'CF20250430003', hospitalName: '测试中医院', prescriptionName: '补中益气汤',
    repetition: 7, currentStep: 'START_DECOCT', bindDeviceId: 1, bindDeviceCode: 'JYJ-001', bindDeviceName: '煎药机-01',
    operatorId: 1001, operatorName: '张三', medicines: [
      { name: '黄芪', dosage: 20, unit: 'g' },
      { name: '人参', dosage: 10, unit: 'g' },
      { name: '白术', dosage: 10, unit: 'g' }
    ],
    createdAt: '2025-04-30T07:00:00'
  },
  {
    taskId: 1004, barcode: 'YP20250430004', status: 'LABELING', patientName: '赵六', patientSex: 0, patientAge: 55,
    prescriptionNumber: 'CF20250430004', hospitalName: '测试中医院', prescriptionName: '六味地黄丸汤剂',
    repetition: 3, currentStep: 'LABEL_CONFIRM', bindDeviceId: 3, bindDeviceCode: 'BZB-001', bindDeviceName: '包装机-01',
    operatorId: 1002, operatorName: '李四', medicines: [
      { name: '熟地黄', dosage: 24, unit: 'g' },
      { name: '山茱萸', dosage: 12, unit: 'g' },
      { name: '山药', dosage: 12, unit: 'g' }
    ],
    createdAt: '2025-04-30T06:30:00'
  },
  {
    taskId: 1005, barcode: 'YP20250430005', status: 'COMPLETED', patientName: '钱七', patientSex: 1, patientAge: 40,
    prescriptionNumber: 'CF20250430005', hospitalName: '测试中医院', prescriptionName: '逍遥散',
    repetition: 7, currentStep: 'INSPECT_PASS', bindDeviceId: 2, bindDeviceCode: 'JYJ-002', bindDeviceName: '煎药机-02',
    operatorId: 1003, operatorName: '王五', medicines: [
      { name: '柴胡', dosage: 10, unit: 'g' },
      { name: '当归', dosage: 10, unit: 'g' },
      { name: '白芍', dosage: 10, unit: 'g' }
    ],
    createdAt: '2025-04-30T06:00:00'
  }
]

// ──────────────────────────────────────────────────────────
// 3. 内存数据库
// ──────────────────────────────────────────────────────────
let db = {
  users: [],
  devices: [],
  tasks: [],
  stepLogs: [],
  photos: [],
  signs: [],
  operationLogs: [],
  loginRecords: [],
  reprintLogs: [],
  sessionCounter: 0
}

// ──────────────────────────────────────────────────────────
// 4. 状态机引擎
// ──────────────────────────────────────────────────────────
function getTask(taskId) {
  return db.tasks.find(t => t.taskId === Number(taskId))
}

function getTaskByBarcode(barcode) {
  return db.tasks.find(t => t.barcode === barcode)
}

function buildStepTimeline(task) {
  const currentIndex = TASK_STEPS.findIndex(s => s.value === STATUS_CURRENT_STEP[task.status])
  return TASK_STEPS.map((step, index) => {
    const log = db.stepLogs.find(l => l.taskId === task.taskId && l.stepType === step.value)
    return {
      value: step.value,
      label: step.label,
      completed: index < currentIndex || (log && log.status === '1'),
      current: step.value === STATUS_CURRENT_STEP[task.status],
      completedAt: log ? log.createdAt : null,
      operatorName: log ? log.operatorName : null,
      needDevice: step.needDevice,
      needPhoto: step.needPhoto
    }
  })
}

function buildNextAction(task) {
  const currentStepType = STATUS_CURRENT_STEP[task.status]
  if (!currentStepType) {
    // 特殊状态处理
    if (task.status === 'INSPECTING') {
      return { text: '交接签字', stepType: 'HANDOVER_SIGN', needDevice: false, needPhoto: false }
    }
    return null
  }
  const stepDef = TASK_STEPS.find(s => s.value === currentStepType)
  return {
    text: stepDef.label,
    stepType: stepDef.value,
    needDevice: stepDef.needDevice,
    needPhoto: stepDef.needPhoto
  }
}

function confirmStep(taskId, stepType, operator, deviceCode, remark) {
  const task = getTask(taskId)
  if (!task) return { success: false, code: 404, msg: '任务不存在' }
  if (task.status === 'COMPLETED' || task.status === 'CANCELLED') {
    return { success: false, code: 400, msg: '任务已结束，无法操作' }
  }

  const expectedStep = STATUS_CURRENT_STEP[task.status]
  if (stepType !== expectedStep) {
    return { success: false, code: 400, msg: `当前只能确认【${TASK_STEPS.find(s=>s.value===expectedStep)?.label || '无'}】` }
  }

  const stepDef = TASK_STEPS.find(s => s.value === stepType)

  // 设备绑定校验
  if (stepDef.needDevice && !task.bindDeviceCode && !deviceCode) {
    return { success: false, code: 400, msg: '该工序需要先绑定设备', needDevice: true }
  }

  // 拍照校验（质检通过前必须拍照）
  if (stepDef.needPhoto) {
    const hasPhoto = db.photos.some(p => p.taskId === task.taskId && p.stepType === stepType)
    if (!hasPhoto) {
      return { success: false, code: 400, msg: '该工序需要先拍照留档', needPhoto: true }
    }
  }

  // 执行状态流转
  const newStatus = STEP_TO_STATUS[stepType]
  const oldStatus = task.status
  task.status = newStatus
  task.currentStep = stepType
  task.operatorId = operator.userId
  task.operatorName = operator.userName
  if (deviceCode) {
    const device = db.devices.find(d => d.deviceCode === deviceCode)
    if (device) {
      task.bindDeviceId = device.deviceId
      task.bindDeviceCode = device.deviceCode
      task.bindDeviceName = device.deviceName
    }
  }

  // 记录工序日志
  const logId = db.stepLogs.length + 1
  db.stepLogs.push({
    id: logId,
    taskId: task.taskId,
    stepType: stepType,
    stepName: stepDef.label,
    operatorId: operator.userId,
    operatorName: operator.userName,
    deviceCode: deviceCode || task.bindDeviceCode,
    deviceName: task.bindDeviceName,
    remark: remark || '',
    photoCount: db.photos.filter(p => p.taskId === task.taskId && p.stepType === stepType).length,
    source: 'PDA',
    status: '1',
    createdAt: new Date().toISOString()
  })

  // 记录操作日志
  addOperationLog(operator, task.taskId, 'TASK_CONFIRM', { stepType, oldStatus, newStatus, remark })

  return {
    success: true,
    code: 200,
    data: {
      taskId: task.taskId,
      newStatus: newStatus,
      newStatusName: TASK_STATUS[newStatus]?.name,
      stepType: stepType,
      stepName: stepDef.label
    }
  }
}

function addOperationLog(user, taskId, type, detail) {
  db.operationLogs.push({
    id: db.operationLogs.length + 1,
    userId: user?.userId || null,
    userName: user?.userName || null,
    deviceCode: null,
    taskId: taskId || null,
    operationType: type,
    operationDetail: JSON.stringify(detail),
    ipAddress: '127.0.0.1',
    createdAt: new Date().toISOString()
  })
}

// ──────────────────────────────────────────────────────────
// 5. 序列化响应构建
// ──────────────────────────────────────────────────────────
function buildTaskResponse(task) {
  const statusMeta = TASK_STATUS[task.status] || TASK_STATUS.PENDING
  const steps = buildStepTimeline(task)
  const nextAction = buildNextAction(task)
  const currentStepIndex = steps.findIndex(s => s.current)

  return {
    taskId: task.taskId,
    barcode: task.barcode,
    status: task.status,
    statusName: statusMeta.name,
    statusColor: statusMeta.color,
    patientName: task.patientName,
    patientSex: task.patientSex,
    patientAge: task.patientAge,
    prescriptionNumber: task.prescriptionNumber,
    hospitalName: task.hospitalName,
    prescriptionName: task.prescriptionName,
    repetition: task.repetition,
    currentStep: task.currentStep,
    currentStepIndex: currentStepIndex,
    deviceCode: task.bindDeviceCode,
    deviceName: task.bindDeviceName,
    operatorId: task.operatorId,
    operatorName: task.operatorName,
    steps: steps,
    medicines: task.medicines || [],
    nextAction: nextAction,
    createdAt: task.createdAt,
    updatedAt: task.updatedAt || task.createdAt
  }
}

// ──────────────────────────────────────────────────────────
// 6. 离线模式控制
// ──────────────────────────────────────────────────────────
let offlineMode = false
let latencyMs = 0
let failureRate = 0 // 0~1

function setOfflineMode(enabled) { offlineMode = enabled }
function setLatency(ms) { latencyMs = ms }
function setFailureRate(rate) { failureRate = Math.max(0, Math.min(1, rate)) }

function shouldFail() {
  if (offlineMode) return true
  if (failureRate > 0 && Math.random() < failureRate) return true
  return false
}

function simulateDelay() {
  return new Promise(r => setTimeout(r, latencyMs))
}

// ──────────────────────────────────────────────────────────
// 7. Reset 接口
// ──────────────────────────────────────────────────────────
function resetAll() {
  db.users = JSON.parse(JSON.stringify(INITIAL_USERS))
  db.devices = JSON.parse(JSON.stringify(INITIAL_DEVICES))
  db.tasks = JSON.parse(JSON.stringify(INITIAL_TASKS))
  db.stepLogs = []
  db.photos = []
  db.signs = []
  db.operationLogs = []
  db.loginRecords = []
  db.reprintLogs = []
  db.sessionCounter = 0
  offlineMode = false
  latencyMs = 0
  failureRate = 0
  console.log('[MockServer] 数据已重置为初始状态')
  return { success: true, message: 'All data reset to initial state' }
}

// ──────────────────────────────────────────────────────────
// 8. 初始化
// ──────────────────────────────────────────────────────────
resetAll()

// ──────────────────────────────────────────────────────────
// 9. 导出
// ──────────────────────────────────────────────────────────
module.exports = {
  // 状态机
  TASK_STATUS,
  TASK_STEPS,
  STEP_TO_STATUS,
  STATUS_CURRENT_STEP,

  // 数据访问
  db,
  getTask,
  getTaskByBarcode,
  buildTaskResponse,
  buildStepTimeline,
  buildNextAction,
  confirmStep,
  addOperationLog,

  // 模拟控制
  offlineMode: () => offlineMode,
  setOfflineMode,
  setLatency,
  setFailureRate,
  shouldFail,
  simulateDelay,

  // 管理
  resetAll
}
