/**
 * PDA 扫码工具封装
 * - H5 环境不支持摄像头，提示使用手动输入
 * - 原生 App/小程序使用 uni.scanCode
 */

export function startScan(options = {}) {
  return new Promise((resolve, reject) => {
    // #ifdef H5
    uni.showToast({ title: '请在真机（App/小程序）中使用扫码功能', icon: 'none' })
    reject(new Error('H5 不支持摄像头扫码'))
    // #endif
    // #ifndef H5
    uni.scanCode({
      scanType: options.scanType || ['barCode', 'qrCode'],
      onlyFromCamera: options.onlyFromCamera !== false,
      success: (res) => {
        uni.vibrateShort()
        resolve(res.result)
      },
      fail: (err) => {
        uni.showToast({ title: '扫码失败', icon: 'none' })
        reject(err)
      }
    })
    // #endif
  })
}

export default { startScan }
