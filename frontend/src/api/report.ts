import request from './request'

// ========== 质检合格率 ==========
export function getQcRateTrend(params: { dateStart?: string; dateEnd?: string; groupBy?: string }) {
  return request.get('/v1/qt/report/trend', { params })
}

export function getQcRateReason(params: { dateStart?: string; dateEnd?: string }) {
  return request.get('/v1/qt/report/reason', { params })
}

export function getQcRateSummary(params: { dateStart?: string; dateEnd?: string }) {
  return request.get('/v1/qt/report/summary', { params })
}
