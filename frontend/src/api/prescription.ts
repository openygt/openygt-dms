import request from './request'

export function getPrescriptionReceiveList(params?: any) {
  return request.get('/v1/prod/prescriptions/receive-list', { params })
}

export function getPrescriptionById(id: number | string) {
  return request.get(`/v1/prod/prescriptions/${id}`)
}

export function receivePrescription(id: number | string) {
  return request.post(`/v1/prod/prescriptions/${id}/receive`)
}

export function rejectPrescription(id: number | string, data: { rejectType: string; reason: string }) {
  return request.post(`/v1/prod/prescriptions/${id}/reject`, data)
}
