import request from './request'

// ========== 药材目录 ==========
export function getMedicineList(params?: any) {
  return request.get('/v1/eq/medicines', { params })
}

export function getMedicineById(id: number | string) {
  return request.get(`/v1/eq/medicines/${id}`)
}

export function createMedicine(data: any) {
  return request.post('/v1/eq/medicines', data)
}

export function updateMedicine(id: number | string, data: any) {
  return request.put(`/v1/eq/medicines/${id}`, data)
}

export function deleteMedicine(id: number | string) {
  return request.delete(`/v1/eq/medicines/${id}`)
}

// ========== 医院管理 ==========
export function getAllHospitals() {
  return request.get('/v1/md/hospitals/all')
}

// ========== 科室管理 ==========
export function getDepartmentList(params?: any) {
  return request.get('/v1/eq/departments', { params })
}

export function getAllDepartments() {
  return request.get('/v1/eq/departments/all')
}

export function createDepartment(data: any) {
  return request.post('/v1/eq/departments', data)
}

export function updateDepartment(id: number | string, data: any) {
  return request.put(`/v1/eq/departments/${id}`, data)
}

export function deleteDepartment(id: number | string) {
  return request.delete(`/v1/eq/departments/${id}`)
}

// ========== 医师管理 ==========
export function getDoctorList(params?: any) {
  return request.get('/v1/eq/doctors', { params })
}

export function createDoctor(data: any) {
  return request.post('/v1/eq/doctors', data)
}

export function updateDoctor(id: number | string, data: any) {
  return request.put(`/v1/eq/doctors/${id}`, data)
}

export function deleteDoctor(id: number | string) {
  return request.delete(`/v1/eq/doctors/${id}`)
}

// ========== 包装规格 ==========
export function getPackageSpecList(params?: any) {
  return request.get('/v1/eq/package-specs', { params })
}

export function createPackageSpec(data: any) {
  return request.post('/v1/eq/package-specs', data)
}

export function updatePackageSpec(id: number | string, data: any) {
  return request.put(`/v1/eq/package-specs/${id}`, data)
}

export function deletePackageSpec(id: number | string) {
  return request.delete(`/v1/eq/package-specs/${id}`)
}
