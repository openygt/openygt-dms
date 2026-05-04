/**
 * PDA 操作反馈系统
 * 关键场景必有声+振动，工业级体验
 */

function tryVibrate(type) {
  try {
    if (type === 'long') uni.vibrateLong()
    else uni.vibrateShort({ type: type || 'medium' })
  } catch (e) {
    // H5 不支持振动，静默忽略
  }
}

export const Feedback = {
  // 扫码成功:短促一声 + 短振
  scanSuccess() {
    tryVibrate('light')
    this._playSound('beep-short.mp3')
  },

  // 操作成功:双声 + 中振
  success() {
    tryVibrate('medium')
    this._playSound('success.mp3')
  },

  // 错误:低沉一声 + 长振
  error() {
    tryVibrate('long')
    this._playSound('error.mp3')
  },

  // 警告:中振
  warning() {
    tryVibrate('medium')
    this._playSound('warning.mp3')
  },

  // 偏差超出:连续三声+长振(称重场景)
  deviation() {
    tryVibrate('long')
    this._playSound('deviation.mp3')
  },
  
  _playSound(filename) {
    try {
      const enabled = uni.getStorageSync('ygt-sound-enabled')
      if (enabled === false) return
      const audio = uni.createInnerAudioContext()
      audio.src = `/static/sounds/${filename}`
      audio.play()
      audio.onEnded(() => audio.destroy())
    } catch (e) {
      // H5 或低版本环境不支持音频播放，静默忽略
    }
  }
}

// 全局挂载
uni.$ygtFeedback = Feedback
