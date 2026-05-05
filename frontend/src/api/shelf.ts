import request from './request'

export function getShelfPage(params?: {
  areaCode?: string
  status?: number
  keyword?: string
  page?: number
  size?: number
}) {
  return request.get('/v1/prod/shelf/page', { params })
}

export function createShelfMaster(data: Record<string, unknown>) {
  return request.post('/v1/prod/shelf', data)
}

export function updateShelfMaster(id: number | string, data: Record<string, unknown>) {
  return request.put(`/v1/prod/shelf/${id}`, data)
}

export function deleteShelfMaster(id: number | string) {
  return request.delete(`/v1/prod/shelf/${id}`)
}
