/**
 * PDA 操作反馈系统
 * 关键场景必有声+振动，工业级体验
 */

export const Feedback = {
  // 扫码成功:短促一声 + 短振
  scanSuccess() {
    uni.vibrateShort({ type: 'light' })
    this._playSound('beep-short.mp3')
  },
  
  // 操作成功:双声 + 中振
  success() {
    uni.vibrateShort({ type: 'medium' })
    this._playSound('success.mp3')
  },
  
  // 错误:低沉一声 + 长振
  error() {
    uni.vibrateLong()
    this._playSound('error.mp3')
  },
  
  // 警告:中振
  warning() {
    uni.vibrateShort({ type: 'medium' })
    this._playSound('warning.mp3')
  },
  
  // 偏差超出:连续三声+长振(称重场景)
  deviation() {
    uni.vibrateLong()
    this._playSound('deviation.mp3')
  },
  
  _playSound(filename) {
    const enabled = uni.getStorageSync('ygt-sound-enabled')
    if (enabled === false) return
    const audio = uni.createInnerAudioContext()
    audio.src = `/static/sounds/${filename}`
    audio.play()
    audio.onEnded(() => audio.destroy())
  }
}

// 全局挂载
uni.$ygtFeedback = Feedback
