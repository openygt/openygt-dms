/**
 * PDA E2E 测试数据工厂
 *
 * 数据来源：mock-server/store.js 中的 INITIAL_*
 * 修改前请先 reset Mock Server：POST /__control/reset
 */

export const testUsers = {
  admin: { userId: 1001, userCode: 'EMP001', password: 'admin123', name: '张三', barcode: 'EMP2024001' },
  packer: { userId: 1002, userCode: 'EMP002', password: 'packer123', name: '李四', barcode: 'EMP2024002' },
  inspector: { userId: 1003, userCode: 'EMP003', password: 'inspect123', name: '王五', barcode: 'EMP2024003' },
  leader: { userId: 1004, userCode: 'EMP004', password: 'leader123', name: '赵六', barcode: 'EMP2024004' }
}

export const testTasks = {
  pending: {
    taskId: 1001,
    barcode: 'YP20250430001',
    patientName: '张三',
    status: 'PENDING',
    statusName: '待处理',
    prescriptionName: '感冒清热方'
  },
  soaking: {
    taskId: 1002,
    barcode: 'YP20250430002',
    patientName: '李四',
    status: 'SOAKING',
    statusName: '泡药中',
    prescriptionName: '四物汤'
  },
  decocting: {
    taskId: 1003,
    barcode: 'YP20250430003',
    patientName: '王五',
    status: 'DECOCTING',
    statusName: '煎药中',
    deviceCode: 'JYJ-001',
    deviceName: '煎药机-01',
    prescriptionName: '补中益气汤'
  },
  labeling: {
    taskId: 1004,
    barcode: 'YP20250430004',
    patientName: '赵六',
    status: 'LABELING',
    statusName: '贴标中',
    deviceCode: 'BZB-001',
    deviceName: '包装机-01',
    prescriptionName: '六味地黄丸汤剂'
  },
  completed: {
    taskId: 1005,
    barcode: 'YP20250430005',
    patientName: '钱七',
    status: 'COMPLETED',
    statusName: '已完成',
    prescriptionName: '逍遥散'
  }
}

export const testDevices = {
  decoctor1: { deviceId: 1, deviceCode: 'JYJ-001', deviceName: '煎药机-01', type: 'DECOCT' },
  decoctor2: { deviceId: 2, deviceCode: 'JYJ-002', deviceName: '煎药机-02', type: 'DECOCT' },
  packer1: { deviceId: 3, deviceCode: 'BZB-001', deviceName: '包装机-01', type: 'PACKAGE' },
  packer2: { deviceId: 4, deviceCode: 'BZB-002', deviceName: '包装机-02', type: 'PACKAGE', status: 'MAINTENANCE' }
}

/**
 * 重置 Mock Server 数据到初始状态
 */
export async function resetMockServer() {
  const res = await fetch('http://localhost:3456/__control/reset', { method: 'POST' })
  return res.json()
}

/**
 * 强制设置任务状态（用于测试不同状态分支）
 */
export async function setTaskStatus(barcode, status) {
  const detailRes = await fetch(`http://localhost:3456/api/v1/pda/tasks/${barcode}`)
  const detail = await detailRes.json()
  const taskId = detail.data?.taskId
  if (!taskId) throw new Error(`Task not found: ${barcode}`)

  const res = await fetch(`http://localhost:3456/__control/tasks/${taskId}/status`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status })
  })
  return res.json()
}

/**
 * 登录并返回 token
 */
export async function loginAs(userKey) {
  const user = testUsers[userKey]
  const res = await fetch('http://localhost:3456/api/v1/pda/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ loginType: 'account', username: user.userCode, password: user.password })
  })
  const data = await res.json()
  return data.data?.token || null
}
