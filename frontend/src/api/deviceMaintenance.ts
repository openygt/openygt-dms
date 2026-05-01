import request from './request'

export function getDeviceMaintenanceList(params?: any) {
  return request.get('/v1/eq/device-maintenances', { params })
}

export function getDeviceMaintenanceById(id: number | string) {
  return request.get(`/v1/eq/device-maintenances/${id}`)
}

export function createDeviceMaintenance(data: any) {
  return request.post('/v1/eq/device-maintenances', data)
}

export function updateDeviceMaintenance(id: number | string, data: any) {
  return request.put(`/v1/eq/device-maintenances/${id}`, data)
}

export function deleteDeviceMaintenance(id: number | string) {
  return request.delete(`/v1/eq/device-maintenances/${id}`)
}
