import request from './request'

// ========== 设备管理 ==========
export function getDeviceList(params: any) {
  return request.get('/v1/eq/devices', { params })
}

/** 全部设备组（设备管理 / 煎药选机等） */
export function getDeviceGroupsAll() {
  return request.get('/v1/eq/groups/all')
}

export function getDeviceById(id: number | string) {
  return request.get(`/v1/eq/devices/${id}`)
}

export function getDeviceDetail(id: number | string) {
  return request.get(`/v1/eq/devices/${id}/detail`)
}

export function createDevice(data: any) {
  return request.post('/v1/eq/devices', data)
}

export function updateDevice(id: number | string, data: any) {
  return request.put(`/v1/eq/devices/${id}`, data)
}

export function deleteDevice(id: number | string) {
  return request.delete(`/v1/eq/devices/${id}`)
}

// ========== 标签打印 ==========
export function getDeviceQrCode(id: number | string, size = 200) {
  return request.get(`/v1/eq/devices/${id}/label-qr`, { params: { size } })
}

export function getDeviceBarcode(id: number | string, width = 300, height = 100) {
  return request.get(`/v1/eq/devices/${id}/label-barcode`, { params: { width, height } })
}

// ========== MQTT配置 ==========
export function getMqttConfig(deviceCode: string) {
  return request.get(`/v1/eq/devices/${deviceCode}/mqtt-config`)
}

export function saveMqttConfig(deviceCode: string, data: any) {
  return request.post(`/v1/eq/devices/${deviceCode}/mqtt-config`, data)
}

// ========== 操作人/换班 ==========
export function getCurrentOperator(deviceCode: string) {
  return request.get(`/v1/eq/devices/${deviceCode}/operator/current`)
}

export function shiftHandover(deviceCode: string, operatorId: number, operatorName: string) {
  return request.post(`/v1/eq/devices/${deviceCode}/operator/shift-handover`, {
    operatorId,
    operatorName
  })
}

// ========== 指令下发 ==========
export function createCommand(data: any) {
  return request.post('/v1/eq/commands', data)
}

export function getPendingCommands(deviceCode: string) {
  return request.get(`/v1/eq/commands/device/${deviceCode}/pending`)
}

export function getRecentCommands(deviceCode: string, limit = 10) {
  return request.get(`/v1/eq/commands/device/${deviceCode}/recent`, { params: { limit } })
}

// ========== 状态快照 ==========
export function getLatestStatus(deviceCode: string) {
  return request.get(`/v1/eq/devices/${deviceCode}/status-snapshot/latest`)
}

// ========== 温度聚合 ==========
export function getTemperatureAggregation(deviceId: string | number, interval: string, start: string, end: string) {
  return request.get(`/v1/eq/devices/${deviceId}/temperature-aggregation`, {
    params: { interval, start, end }
  })
}

// ========== 煎药方案 ==========
export function getSchemeList(params?: any) {
  return request.get('/v1/md/schemes', { params })
}

export function getSchemeById(id: number | string) {
  return request.get(`/v1/md/schemes/${id}`)
}

export function createScheme(data: any) {
  return request.post('/v1/md/schemes', data)
}

export function updateScheme(id: number | string, data: any) {
  return request.put(`/v1/md/schemes/${id}`, data)
}

export function deleteScheme(id: number | string) {
  return request.delete(`/v1/md/schemes/${id}`)
}

// ========== 煎药过程追溯 ==========
export function getTraces(params: any) {
  return request.get('/v1/eq/traces', { params })
}

export function getTraceByPrescriptionNo(prescriptionNo: string) {
  return request.get(`/v1/eq/traces/${prescriptionNo}`)
}

export function getTraceEvents(prescriptionNo: string) {
  return request.get(`/v1/eq/traces/${prescriptionNo}/events`)
}

export function getTraceTemperatureCurve(prescriptionNo: string, granularity = '1min') {
  return request.get(`/v1/eq/traces/${prescriptionNo}/temperature-curve`, { params: { granularity } })
}

export function updateTraceStep(prescriptionNo: string, stepCode: string, data: any) {
  return request.put(`/v1/eq/traces/${prescriptionNo}/step/${stepCode}`, data)
}

// ========== 时间校验 ==========
export function getTimeCheckRules() {
  return request.get('/v1/eq/time-check/rules')
}

export function validateTimeCheck(prescriptionNo: string, toStep: string) {
  return request.post('/v1/eq/time-check/validate', null, { params: { prescriptionNo, toStep } })
}

// ========== Dashboard ==========
export function getDashboardMetrics() {
  return request.get('/v1/eq/dashboard/metrics')
}

export function getStageDistribution() {
  return request.get('/v1/eq/dashboard/stage-distribution')
}

export function getWorkerEfficiency(date?: string) {
  return request.get('/v1/eq/dashboard/worker-efficiency', { params: { date } })
}

export function getHourlyTrend(date?: string) {
  return request.get('/v1/eq/dashboard/hourly-trend', { params: { date } })
}

export function getDashboardDeviceUtilization(date?: string) {
  return request.get('/v1/eq/dashboard/device-utilization', { params: { date } })
}

// ========== 工作量统计 ==========
export function getWorkloadStats(params: any) {
  return request.get('/v1/eq/workload/stats', { params })
}

export function getWorkloadSummary(params: any) {
  return request.get('/v1/eq/workload/stats/summary', { params })
}

// ========== 加水量公式 ==========
export function getWaterFormulas() {
  return request.get('/v1/eq/water-formulas')
}

export function createWaterFormula(data: any) {
  return request.post('/v1/eq/water-formulas', data)
}

export function updateWaterFormula(id: number, data: any) {
  return request.put(`/v1/eq/water-formulas/${id}`, data)
}

export function deleteWaterFormula(id: number) {
  return request.delete(`/v1/eq/water-formulas/${id}`)
}

export function calculateWaterFormula(id: number, variables: any) {
  return request.post(`/v1/eq/water-formulas/${id}/calculate`, variables)
}

// ========== 处方默认设置 ==========
export function getPrescriptionDefaults() {
  return request.get('/v1/eq/prescription-defaults')
}

export function updatePrescriptionDefault(settingKey: string, data: any) {
  return request.put(`/v1/eq/prescription-defaults/${settingKey}`, data)
}

// ========== 设备利用率 ==========
export function getDeviceUtilizationStats(params: any) {
  return request.get('/v1/eq/device-utilization', { params })
}

export function getDeviceUtilizationTrend(deviceCode: string, days = 7) {
  return request.get('/v1/eq/device-utilization/trend', { params: { deviceCode, days } })
}

// ========== 告警配置 ==========
export function getAlarmConfigs() {
  return request.get('/v1/eq/alarm-configs')
}

export function createAlarmConfig(data: any) {
  return request.post('/v1/eq/alarm-configs', data)
}

export function updateAlarmConfig(id: number, data: any) {
  return request.put(`/v1/eq/alarm-configs/${id}`, data)
}

export function deleteAlarmConfig(id: number) {
  return request.delete(`/v1/eq/alarm-configs/${id}`)
}
