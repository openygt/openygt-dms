export const deviceHandlers = {
  /** POST /tasks/{taskId}/bind-device */
  bind(params) {
    return { code: 200, message: '绑定成功', data: { success: true } }
  }
}
