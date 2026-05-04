import { MOCK_TASKS, findTask, findTaskById, cloneTask } from '../data/tasks.js'

export const taskHandlers = {
  /** GET /task/{barcode} */
  getByBarcode(params, ctx) {
    const barcode = ctx.pathParams ? ctx.pathParams[0] : params.barcode
    const task = findTask(barcode)
    if (!task) {
      return { code: 404, message: '未找到该任务', data: null }
    }
    const t = cloneTask(task)
    t.nextAction = t.taskStatus === 'PENDING' ? '开始泡药' : t.taskStatus === 'SOAKING' ? '结束泡药' : '继续处理'
    return { code: 200, message: 'success', data: t }
  },

  /** GET /task/{barcode}/reprint-info */
  getReprintInfo(params, ctx) {
    const barcode = ctx.pathParams ? ctx.pathParams[0] : params.barcode
    const task = findTask(barcode)
    if (!task) {
      return { code: 404, message: '未找到该任务', data: null }
    }
    return {
      code: 200,
      message: 'success',
      data: {
        barcode: task.barcode,
        patientName: task.patientName,
        prescriptionNumber: task.prescriptionNumber,
        decoctionDate: '2026-05-04',
        status: task.statusName
      }
    }
  },

  /** GET /task/{barcode}/temperature-curve */
  temperatureCurve(params, ctx) {
    const taskId = ctx.pathParams ? ctx.pathParams[0] : params.taskId
    const now = Date.now()
    return {
      code: 200,
      message: 'success',
      data: {
        maxTemp: 105.2,
        avgTemp: 98.5,
        minTemp: 92.1,
        duration: 45,
        points: Array.from({ length: 30 }, (_, i) => ({
          time: new Date(now - (30 - i) * 120000).toISOString(),
          temperature: Number((95 + Math.sin(i / 3) * 5 + Math.random() * 2 - 1).toFixed(1))
        }))
      }
    }
  },

  /** GET /tasks */
  list(params) {
    let list = [...MOCK_TASKS]
    if (params.status) {
      list = list.filter(t => t.taskStatus === params.status)
    }
    if (params.keyword) {
      const kw = params.keyword.toLowerCase()
      list = list.filter(t =>
        t.barcode.toLowerCase().includes(kw) ||
        t.patientName.includes(kw) ||
        t.prescriptionNumber.toLowerCase().includes(kw)
      )
    }
    const page = Number(params.page) || 1
    const size = Number(params.size) || 10
    const total = list.length
    const records = list.slice((page - 1) * size, page * size)
    return {
      code: 200,
      message: 'success',
      data: { records, total, page, size }
    }
  },

  /** GET /tasks/recent */
  recent(params) {
    const limit = Number(params.limit) || 20
    const sorted = [...MOCK_TASKS]
      .filter(t => t.taskStatus !== 'CANCELLED')
      .sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt))
    return {
      code: 200,
      message: 'success',
      data: sorted.slice(0, limit).map(t => ({
        taskId: t.taskId,
        barcode: t.barcode,
        patientName: t.patientName,
        status: t.taskStatus,
        statusName: t.statusName,
        updatedAt: t.updatedAt
      }))
    }
  },

  /** GET /tasks/today-pending */
  todayPending() {
    const count = MOCK_TASKS.filter(t =>
      t.taskStatus === 'PENDING' || t.taskStatus === 'SOAKING'
    ).length
    return { code: 200, message: 'success', data: { count } }
  }
}
