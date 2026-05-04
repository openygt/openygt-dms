import { findTask } from '../data/tasks.js'

export const patientHandlers = {
  /** GET /patient/query */
  queryByBarcode(params) {
    const task = findTask(params.barcode)
    if (!task) {
      return { code: 404, message: '未找到该处方信息', data: null }
    }
    return {
      code: 200,
      message: 'success',
      data: {
        prescriptionId: task.taskId * 10 + 5,
        prescriptionNumber: task.prescriptionNumber,
        patientName: task.patientName,
        hospitalName: task.hospitalName,
        repetition: task.repetition,
        prescriptionDate: '2026-05-04',
        status: task.taskStatus,
        statusName: task.statusName,
        estimatedFinishTime: '2026-05-04 16:00',
        pickupWindow: '3号窗口',
        deliveryType: 'SELF',
        steps: [
          { label: '处方审核', completed: true, current: false, time: '07:00' },
          { label: task.statusName, completed: task.taskStatus !== 'PENDING', current: task.taskStatus === 'PENDING' || task.taskStatus === 'SOAKING', time: '' },
          { label: '煎药中', completed: task.taskStatus === 'DECOCTING' || task.taskStatus === 'DECOCTED' || task.taskStatus === 'COMPLETED', current: task.taskStatus === 'DECOCTING', time: '' },
          { label: '包装中', completed: task.taskStatus === 'PACKAGING' || task.taskStatus === 'PACKAGED' || task.taskStatus === 'COMPLETED', current: task.taskStatus === 'PACKAGING', time: '' },
          { label: '质检中', completed: task.taskStatus === 'INSPECTING' || task.taskStatus === 'COMPLETED', current: task.taskStatus === 'INSPECTING', time: '' },
          { label: '已完成', completed: task.taskStatus === 'COMPLETED', current: task.taskStatus === 'COMPLETED', time: task.taskStatus === 'COMPLETED' ? '16:00' : '' }
        ]
      }
    }
  },

  /** GET /patient/query-by-phone */
  queryByPhone() {
    return {
      code: 200,
      message: 'success',
      data: {
        prescriptionId: 5005,
        prescriptionNumber: 'CH20260505001',
        patientName: '测试用户',
        hospitalName: '市中医院',
        repetition: 7,
        prescriptionDate: '2026-05-04',
        status: 'DECOCTING',
        statusName: '煎药中',
        estimatedFinishTime: '2026-05-04 15:30',
        pickupWindow: '3号窗口',
        deliveryType: 'SELF',
        steps: [
          { label: '处方审核', completed: true, current: false, time: '08:00' },
          { label: '泡药', completed: true, current: false, time: '09:00' },
          { label: '煎药中', completed: false, current: true, time: '' },
          { label: '包装', completed: false, current: false, time: '' },
          { label: '质检', completed: false, current: false, time: '' },
          { label: '已完成', completed: false, current: false, time: '' }
        ]
      }
    }
  },

  /** POST /patient/verify-code */
  verifyCode() {
    return { code: 200, message: '验证码已发送', data: { success: true } }
  }
}
