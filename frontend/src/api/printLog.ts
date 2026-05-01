import request from './request'

export function getPrintLogList(params?: any) {
  return request.get('/v1/eq/print-logs', { params })
}
