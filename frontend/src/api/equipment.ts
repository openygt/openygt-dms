import request from './request'

// ========== 设备管理 ==========
export function getDeviceList(params: any) {
  return request.get('/v1/eq/devices', { params })
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
  return request.get('/v1/md/decoct-schemes', { params })
}

export function getSchemeById(id: number | string) {
  return request.get(`/v1/md/decoct-schemes/${id}`)
}

export function createScheme(data: any) {
  return request.post('/v1/md/decoct-schemes', data)
}

export function updateScheme(id: number | string, data: any) {
  return request.put(`/v1/md/decoct-schemes/${id}`, data)
}

export function deleteScheme(id: number | string) {
  return request.delete(`/v1/md/decoct-schemes/${id}`)
}
