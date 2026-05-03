import request from './request'

// ========== 步骤可视化 ==========
export function getTaskSteps(taskId: number | string) {
  return request.get(`/v1/prod/task/${taskId}/steps`)
}

export function getStepDetail(taskId: number | string, stepCode: string) {
  return request.get(`/v1/prod/task/${taskId}/step-detail`, { params: { stepCode } })
}

// ========== 药材分组 ==========
export function getHerbGroups(prescriptionId: number | string) {
  return request.get(`/v1/prod/prescription/${prescriptionId}/herb-groups`)
}

export function confirmHerbGroup(groupId: number | string) {
  return request.post(`/v1/prod/herb-group/${groupId}/confirm`)
}

// ========== 时效预警 ==========
export function getTimeMonitorDashboard() {
  return request.get('/v1/prod/monitor/time-monitor/dashboard')
}

export function getTimeMonitor(taskId: number | string) {
  return request.get(`/v1/prod/monitor/time-monitor/${taskId}`)
}

export function createTimeRule(data: any) {
  return request.post('/v1/prod/monitor/time-rule', data)
}

export function updateTimeRule(id: number | string, data: any) {
  return request.put(`/v1/prod/monitor/time-rule/${id}`, data)
}

export function getActiveAlerts() {
  return request.get('/v1/prod/monitor/alert/active')
}

export function resolveAlert(alertId: number | string) {
  return request.post(`/v1/prod/monitor/alert/${alertId}/resolve`)
}

export function getAlertStatistics() {
  return request.get('/v1/prod/monitor/alert/statistics')
}

// ========== 急诊处方 ==========
export function getEmergencyPrescriptions(params?: any) {
  return request.get('/v1/prod/prescription/emergency', { params })
}

export function markEmergency(prescriptionId: number | string, emergencyLevel: number, extra?: { deliveryType?: string; deliveryLocation?: string; delayReason?: string }) {
  const params = new URLSearchParams()
  params.append('emergencyLevel', String(emergencyLevel))
  if (extra?.deliveryType) params.append('deliveryType', extra.deliveryType)
  if (extra?.deliveryLocation) params.append('deliveryLocation', extra.deliveryLocation)
  if (extra?.delayReason) params.append('delayReason', extra.delayReason)
  return request.post(`/v1/prod/prescription/${prescriptionId}/emergency?${params.toString()}`)
}

export function signEmergency(emergencyId: number | string, nurseName: string) {
  return request.post(`/v1/prod/emergency/${emergencyId}/sign`, { nurseName })
}

// ========== 智能分配 ==========
export function autoAssign(data: any) {
  return request.post('/v1/prod/assignment/auto', data)
}

export function manualAssign(data: any) {
  return request.post('/v1/prod/assignment/manual', data)
}

export function reassign(assignmentId: number | string, data: any) {
  return request.put(`/v1/prod/assignment/${assignmentId}/reassign`, data)
}

export function getSchedule(params?: any) {
  return request.get('/v1/prod/assignment/schedule', { params })
}

export function getEmployeeLoad(date: string) {
  return request.get(`/v1/prod/assignment/employee-load`, { params: { date } })
}

export function getDeviceLoad(date: string) {
  return request.get(`/v1/prod/assignment/device-load`, { params: { date } })
}

// ========== 任务回退 ==========
export function rollbackTask(taskId: number | string, data: any) {
  return request.post(`/v1/prod/task/${taskId}/rollback`, data)
}

export function approveRollback(rollbackId: number | string, data: any) {
  return request.put(`/v1/prod/rollback/${rollbackId}/approve`, data)
}

export function getRollbackList(params?: any) {
  return request.get('/v1/prod/rollback/list', { params })
}

// ========== 患者端 ==========
export function queryByCode(data: any) {
  return request.post('/v1/prod/patient/query-by-code', data)
}

export function queryByPhone(data: any) {
  return request.post('/v1/prod/patient/query-by-phone', data)
}

export function getPatientProgress(token: string) {
  return request.get(`/v1/prod/patient/progress/${token}`)
}

export function getPrescriptionTrace(prescriptionId: number | string) {
  return request.get(`/v1/prod/patient/prescription/${prescriptionId}/trace`)
}

// ========== 货架管理 ==========
export function getShelfList(params?: any) {
  return request.get('/v1/prod/shelf/list', { params })
}

export function putOnShelf(data: any) {
  return request.post('/v1/prod/shelf/put-on', data)
}

export function takeOffShelf(data: any) {
  return request.post('/v1/prod/shelf/take-off', data)
}

// ========== PDA语音 ==========
export function tts(data: any) {
  return request.post('/v1/prod/voice/tts', data)
}

export function getVoiceSettings() {
  return request.get('/v1/prod/voice/settings')
}

export function updateVoiceSettings(data: any) {
  return request.post('/v1/prod/voice/settings', data)
}

export function deleteVoiceSetting(id: number | string) {
  return request.delete(`/v1/prod/voice/settings/${id}`)
}

// ========== 员工条码 ==========
export function getEmployeeBarcode(employeeId: number | string) {
  return request.get(`/v1/prod/employee/${employeeId}/barcode`)
}

export function printEmployeeBarcode(employeeId: number | string) {
  return request.post(`/v1/prod/employee/${employeeId}/barcode/print`)
}
