import { findTaskById } from '../data/tasks.js'

export const processHandlers = {
  /** POST /task/confirm */
  confirm(params) {
    const task = findTaskById(params.taskId)
    if (task) {
      // 模拟推进状态：当前步骤完成，激活下一步
      const currentIdx = task.steps.findIndex(s => s.current)
      if (currentIdx >= 0) {
        task.steps[currentIdx].completed = true
        task.steps[currentIdx].current = false
        task.steps[currentIdx].time = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
        if (currentIdx + 1 < task.steps.length) {
          task.steps[currentIdx + 1].current = true
        }
      }
    }
    return { code: 200, message: '确认成功', data: { success: true } }
  },

  /** POST /tasks/{taskId}/status */
  advanceStatus(params, ctx) {
    const taskId = ctx.pathParams ? ctx.pathParams[0] : params.taskId
    const task = findTaskById(Number(taskId))
    if (task) {
      const currentIdx = task.steps.findIndex(s => s.current)
      if (currentIdx >= 0) {
        task.steps[currentIdx].completed = true
        task.steps[currentIdx].current = false
        task.steps[currentIdx].time = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
        if (currentIdx + 1 < task.steps.length) {
          task.steps[currentIdx + 1].current = true
        }
      }
    }
    return { code: 200, message: '操作成功', data: { success: true } }
  },

  /** POST /weight/record */
  weightRecord() {
    return { code: 200, message: '称重记录成功', data: { recordId: Date.now() } }
  },

  /** GET /weight/{taskId} */
  getWeight() {
    return {
      code: 200,
      message: 'success',
      data: {
        records: [
          { weight: 1250, time: '2026-05-04 10:00', operator: '王师傅' },
          { weight: 1248, time: '2026-05-04 10:05', operator: '王师傅' }
        ],
        totalWeight: 2498
      }
    }
  },

  /** GET /qc/{taskId} */
  getQc() {
    return {
      code: 200,
      message: 'success',
      data: {
        qcId: 1,
        taskId: 1004,
        status: 'PASSED',
        inspector: '李质检',
        checkTime: '2026-05-04 11:00',
        result: '合格',
        items: [
          { name: '外观检查', result: '合格', remark: '' },
          { name: '重量检查', result: '合格', remark: '符合标准' },
          { name: '包装检查', result: '合格', remark: '' }
        ]
      }
    }
  },

  /** POST /qc/submit */
  qcSubmit() {
    return { code: 200, message: '质检提交成功', data: { success: true } }
  },

  /** GET /process/{taskId} */
  getProcessSteps(params, ctx) {
    const taskId = ctx.pathParams ? ctx.pathParams[0] : params.taskId
    const task = findTaskById(Number(taskId))
    if (!task) {
      return { code: 200, message: 'success', data: [] }
    }
    return {
      code: 200,
      message: 'success',
      data: task.steps.map(s => ({
        stepName: s.label,
        status: s.completed ? 'DONE' : s.current ? 'CURRENT' : 'PENDING',
        operator: s.completed ? '王师傅' : '',
        time: s.time || ''
      }))
    }
  }
}
