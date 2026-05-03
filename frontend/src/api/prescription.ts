import request from './request'

// ==================== 基础 CRUD ====================

export function getPrescriptionReceiveList(params?: any) {
  return request.get('/v1/prod/prescriptions/receive-list', { params })
}

export function getPrescriptionById(id: number | string) {
  return request.get(`/v1/prod/prescriptions/${id}`)
}

export function receivePrescription(id: number | string) {
  return request.post(`/v1/prod/prescriptions/${id}/receive`)
}

export function rejectPrescription(id: number | string, data: { rejectType: string; reason: string; operatorId: number; operatorName: string }) {
  return request.post(`/v1/prod/prescriptions/${id}/reject?operatorId=${data.operatorId}&operatorName=${encodeURIComponent(data.operatorName)}`, data)
}

export function getPrescriptionDetail(id: number | string) {
  return request.get(`/v1/prod/prescriptions/${id}/detail`)
}

export function listPrescriptions(params?: any) {
  return request.get('/v1/prod/prescriptions', { params })
}

export function createPrescription(data: any) {
  return request.post('/v1/prod/prescriptions', data)
}

export function updatePrescription(id: number | string, data: any) {
  return request.put(`/v1/prod/prescriptions/${id}`, data)
}

// ==================== 结构化创建 ====================

export function createStructuredPrescription(data: any) {
  return request.post('/v1/prod/prescriptions/structured', data)
}

// ==================== 药品目录查询 ====================

export function searchMedicines(keyword: string) {
  return request.get('/v1/eq/medicines', { params: { keyword, page: 1, size: 20 } })
}

// ==================== CSV 导入 ====================

export function importPrescriptionCsv(file: File, hospitalId?: number) {
  const formData = new FormData()
  formData.append('file', file)
  if (hospitalId != null) formData.append('hospitalId', String(hospitalId))
  return request.post('/v1/prod/prescriptions/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

// ==================== HIS 推送 ====================

export function pushPrescriptions(data: any) {
  return request.post('/v1/prod/prescriptions/push', data)
}

export function pushSinglePrescription(hospitalCode: string, data: any) {
  return request.post(`/v1/prod/prescriptions/push-single?hospitalCode=${hospitalCode}`, data)
}

// ==================== OCR ====================

export function createPrescriptionFromOcr(data: any) {
  return request.post('/v1/prod/prescriptions/ocr', data)
}

export function markEmergency(prescriptionId: number | string, emergencyLevel: number, extra?: { deliveryType?: string; deliveryLocation?: string; delayReason?: string }) {
  const params = new URLSearchParams()
  params.append('emergencyLevel', String(emergencyLevel))
  if (extra?.deliveryType) params.append('deliveryType', extra.deliveryType)
  if (extra?.deliveryLocation) params.append('deliveryLocation', extra.deliveryLocation)
  if (extra?.delayReason) params.append('delayReason', extra.delayReason)
  return request.post(`/v1/prod/prescription/${prescriptionId}/emergency?${params.toString()}`)
}

export function reactivatePrescription(id: number | string) {
  return request.post(`/v1/prod/prescriptions/${id}/reactivate`)
}

export function cancelPrescription(id: number | string) {
  return request.post(`/v1/prod/prescriptions/${id}/cancel`)
}

export function uploadOcrImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/v1/prod/prescriptions/ocr/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
