import { MOCK_SHELVES } from '../data/shelves.js'
import { MOCK_TASKS } from '../data/tasks.js'

export const shelfHandlers = {
  /** GET /shelves */
  list(params) {
    let list = [...MOCK_SHELVES]
    if (params.keyword) {
      const kw = params.keyword.toLowerCase()
      list = list.filter(s =>
        s.shelfCode.toLowerCase().includes(kw) ||
        s.areaName.includes(kw)
      )
    }
    return { code: 200, message: 'success', data: list }
  },

  /** GET /bag/{barcode} */
  getBag(params, ctx) {
    const barcode = ctx.pathParams ? ctx.pathParams[0] : params.barcode
    const task = MOCK_TASKS.find(t => t.barcode === barcode)
    if (!task) {
      return { code: 404, message: '未找到该药袋', data: null }
    }
    return {
      code: 200,
      message: 'success',
      data: {
        bagId: task.taskId * 10,
        barcode: task.barcode,
        patientName: task.patientName,
        prescriptionNumber: task.prescriptionNumber,
        shelfCode: task.taskStatus === 'COMPLETED' ? 'A-002' : null
      }
    }
  },

  /** POST /shelf/put-on */
  putOn(params) {
    const shelf = MOCK_SHELVES.find(s => s.shelfCode === params.shelfCode)
    if (shelf && shelf.occupied < shelf.capacity) {
      shelf.occupied++
    }
    return { code: 200, message: '上架成功', data: { bagId: params.bagId, shelfCode: params.shelfCode } }
  },

  /** POST /shelf/take-off */
  takeOff(params) {
    const shelf = MOCK_SHELVES.find(s => s.shelfCode === params.shelfCode)
    if (shelf && shelf.occupied > 0) {
      shelf.occupied--
    }
    return { code: 200, message: '取走成功', data: { bagId: params.bagId, shelfCode: params.shelfCode } }
  }
}
