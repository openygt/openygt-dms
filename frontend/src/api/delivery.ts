import request from './request'

export function getDeliveryRecordList(params?: any) {
  return request.get('/v1/prod/delivery-records', { params })
}

export function getDeliveryRecordById(id: number | string) {
  return request.get(`/v1/prod/delivery-records/${id}`)
}

export function createDeliveryRecord(data: any) {
  return request.post('/v1/prod/delivery-records', data)
}

export function updateDeliveryRecord(id: number | string, data: any) {
  return request.put(`/v1/prod/delivery-records/${id}`, data)
}

export function deleteDeliveryRecord(id: number | string) {
  return request.delete(`/v1/prod/delivery-records/${id}`)
}

export function confirmDelivery(id: number | string, data: { operatorId: string; receiverName: string; receiverPhone?: string; remark?: string }) {
  return request.post(`/v1/prod/delivery-records/${id}/confirm`, null, { params: data })
}
