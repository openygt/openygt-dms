import request from './request'

// ========== 标签模板 ==========
export function getLabelTemplateList(params: { page?: number; size?: number; keyword?: string; templateType?: string }) {
  return request.get('/v1/eq/label-templates', { params })
}

export function getLabelTemplateById(id: number | string) {
  return request.get(`/v1/eq/label-templates/${id}`)
}

export function createLabelTemplate(data: any) {
  return request.post('/v1/eq/label-templates', data)
}

export function updateLabelTemplate(id: number | string, data: any) {
  return request.put(`/v1/eq/label-templates/${id}`, data)
}

export function deleteLabelTemplate(id: number | string) {
  return request.delete(`/v1/eq/label-templates/${id}`)
}

// ========== 打印机管理 ==========
export function getPrinterList(params: { page?: number; size?: number; keyword?: string; deviceType?: string | number }) {
  return request.get('/v1/eq/printers/list', { params })
}

export function getPrinterById(id: number | string) {
  return request.get(`/v1/eq/printers/${id}`)
}

export function testPrinter(id: number | string) {
  return request.post(`/v1/eq/printers/${id}/test`)
}
