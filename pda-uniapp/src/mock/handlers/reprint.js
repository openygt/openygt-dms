export const reprintHandlers = {
  /** POST /task/reprint */
  reprint() {
    return { code: 200, message: '补打已提交', data: { printJobId: 'PJ' + Date.now(), status: 'SUBMITTED' } }
  }
}
