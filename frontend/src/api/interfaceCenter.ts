import request from './request'

export function getInterfaceConfigs(params?: any) {
  return request.get('/v1/sys/interfaces', { params })
}

export function getInterfaceConfig(id: number | string) {
  return request.get(`/v1/sys/interfaces/${id}`)
}

export function createInterfaceConfig(data: any) {
  return request.post('/v1/sys/interfaces', data)
}

export function updateInterfaceConfig(id: number | string, data: any) {
  return request.put(`/v1/sys/interfaces/${id}`, data)
}

export function deleteInterfaceConfig(id: number | string) {
  return request.delete(`/v1/sys/interfaces/${id}`)
}

export function getInterfaceLogs(params?: any) {
  return request.get('/v1/sys/interface-logs', { params })
}
