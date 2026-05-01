import request from './request'

// ========== 步骤可视化 ==========
export function getTaskSteps(taskId: number | string) {
  return request.get(`/v1/prod/tasks/${taskId}/steps`)
}

export function getStepDetail(taskId: number | string, stepCode: string) {
  return request.get(`/v1/prod/tasks/${taskId}/steps/${stepCode}`)
}

// ========== 药材分组 ==========
export function getHerbGroups(prescriptionId: number | string) {
  return request.get(`/v1/prod/prescriptions/${prescriptionId}/herb-groups`)
}

export function confirmHerbGroup(groupId: number | string) {
  return request.post(`/v1/prod/herb-groups/${groupId}/confirm`)
}

// ========== 时效预警 ==========
export function getTimeMonitorDashboard() {
  return request.get('/v1/monitor/time-monitor/dashboard')
}

export function getTimeMonitor(taskId: number | string) {
  return request.get(`/v1/monitor/time-monitor/tasks/${taskId}`)
}

export function createTimeRule(data: any) {
  return request.post('/v1/monitor/time-rules', data)
}

export function updateTimeRule(id: number | string, data: any) {
  return request.put(`/v1/monitor/time-rules/${id}`, data)
}

export function getActiveAlerts() {
  return request.get('/v1/monitor/time-monitor/alerts/active')
}

export function resolveAlert(alertId: number | string) {
  return request.post(`/v1/monitor/time-monitor/alerts/${alertId}/resolve`)
}

export function getAlertStatistics() {
  return request.get('/v1/monitor/time-monitor/alerts/statistics')
}

// ========== 急诊处方 ==========
export function getEmergencyPrescriptions(params?: any) {
  return request.get('/v1/prod/emergency-prescriptions', { params })
}

export function markEmergency(prescriptionId: number | string) {
  return request.post(`/v1/prod/prescriptions/${prescriptionId}/mark-emergency`)
}

export function signEmergency(emergencyId: number | string) {
  return request.post(`/v1/prod/emergency-prescriptions/${emergencyId}/sign`)
}

// ========== 智能分配 ==========
export function autoAssign(data: any) {
  return request.post('/v1/prod/assignments/auto', data)
}

export function manualAssign(data: any) {
  return request.post('/v1/prod/assignments/manual', data)
}

export function reassign(assignmentId: number | string, data: any) {
  return request.put(`/v1/prod/assignments/${assignmentId}/reassign`, data)
}

export function getSchedule(params?: any) {
  return request.get('/v1/prod/schedule', { params })
}

export function getEmployeeLoad(date: string) {
  return request.get(`/v1/prod/employees/load`, { params: { date } })
}

export function getDeviceLoad(date: string) {
  return request.get(`/v1/prod/devices/load`, { params: { date } })
}

// ========== 任务回退 ==========
export function rollbackTask(taskId: number | string, data: any) {
  return request.post(`/v1/prod/tasks/${taskId}/rollback`, data)
}

export function approveRollback(rollbackId: number | string, data: any) {
  return request.put(`/v1/prod/rollbacks/${rollbackId}/approve`, data)
}

export function getRollbackList(params?: any) {
  return request.get('/v1/prod/rollbacks', { params })
}

// ========== 患者端 ==========
export function queryByCode(data: any) {
  return request.post('/v1/patient/query-by-code', data)
}

export function queryByPhone(data: any) {
  return request.post('/v1/patient/query-by-phone', data)
}

export function getPatientProgress(token: string) {
  return request.get('/v1/patient/progress', { params: { token } })
}

export function getPrescriptionTrace(prescriptionId: number | string) {
  return request.get(`/v1/patient/prescriptions/${prescriptionId}/trace`)
}

// ========== 货架管理 ==========
export function getShelfList(params?: any) {
  return request.get('/v1/warehouse/shelves', { params })
}

export function putOnShelf(data: any) {
  return request.post('/v1/warehouse/shelves/put-on', data)
}

export function takeOffShelf(data: any) {
  return request.post('/v1/warehouse/shelves/take-off', data)
}

// ========== PDA语音 ==========
export function tts(data: any) {
  return request.post('/v1/pda/tts', data)
}

export function getVoiceSettings() {
  return request.get('/v1/pda/voice-settings')
}

export function updateVoiceSettings(data: any) {
  return request.put('/v1/pda/voice-settings', data)
}

// ========== 员工条码 ==========
export function getEmployeeBarcode(employeeId: number | string) {
  return request.get(`/v1/system/employees/${employeeId}/barcode`)
}

export function printEmployeeBarcode(employeeId: number | string) {
  return request.post(`/v1/system/employees/${employeeId}/barcode/print`)
}
