const SIZES = ['normal', 'large', 'xlarge']
const KEY = 'ygt-font-size'

export const FontSize = {
  current() {
    return uni.getStorageSync(KEY) || 'normal'
  },
  
  next() {
    const cur = this.current()
    const idx = SIZES.indexOf(cur)
    const next = SIZES[(idx + 1) % SIZES.length]
    this.set(next)
    return next
  },
  
  set(size) {
    uni.setStorageSync(KEY, size)
    this._apply(size)
  },
  
  _apply(size) {
    const pages = getCurrentPages()
    const page = pages[pages.length - 1]
    if (!page) return
    
    // 移除旧 class
    const pageEl = page.$page?.$el || page.$el
    if (pageEl) {
      pageEl.classList.remove('font-large', 'font-xlarge')
      if (size === 'large') pageEl.classList.add('font-large')
      if (size === 'xlarge') pageEl.classList.add('font-xlarge')
    }
  }
}

export default FontSize
