import request from './request'

// ========== 批次追溯 ==========
export function searchBatchTrace(params: { batchNo?: string; prescriptionNo?: string; page?: number; size?: number }) {
  return request.get('/v1/eq/trace/batch/search', { params })
}

export function getBatchDetail(batchNo: string) {
  return request.get('/v1/eq/trace/batch/detail', { params: { batchNo } })
}

export function getBatchTimeline(prescriptionNo: string) {
  return request.get(`/v1/eq/trace/batch/timeline/${prescriptionNo}`)
}

export function getBatchNos() {
  return request.get('/v1/eq/trace/batch/batch-nos')
}

// ========== 异常追溯 ==========
export function getExceptionList(params: {
  page?: number
  size?: number
  keyword?: string
  exceptionType?: string
  handleStatus?: string
  dateStart?: string
  dateEnd?: string
}) {
  return request.get('/v1/eq/trace/exceptions/list', { params })
}

export function getExceptionById(id: number | string) {
  return request.get(`/v1/eq/trace/exceptions/${id}`)
}

export function getExceptionStat(params: { dateStart?: string; dateEnd?: string }) {
  return request.get('/v1/eq/trace/exceptions/stat', { params })
}
