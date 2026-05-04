export const commonHandlers = {
  /** GET /config/version */
  version() {
    return { code: 200, message: 'success', data: { version: '1.2.0', buildTime: '2026-05-01' } }
  },

  /** POST /heartbeat */
  heartbeat() {
    return { code: 200, message: 'success', data: { serverTime: new Date().toISOString() } }
  },

  /** POST /file/upload */
  fileUpload() {
    return { code: 200, message: 'success', data: { url: '/mock/uploads/file_' + Date.now() + '.jpg', size: 12345 } }
  },

  /** POST /photo/upload */
  photoUpload() {
    return { code: 200, message: 'success', data: { photoId: Date.now(), url: '/mock/uploads/photo_' + Date.now() + '.jpg' } }
  }
}
