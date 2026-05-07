import request from './request'

// ========== 标准药材主数据 ==========
export function getMdMedicineList(params?: any) {
  return request.get('/v1/md/medicines', { params })
}

export function getMdMedicineById(id: number | string) {
  return request.get(`/v1/md/medicines/${id}`)
}

// ========== 通用字典查询 ==========
export function getDictList(type: string) {
  return request.get(`/v1/md/dict/${type}`)
}
