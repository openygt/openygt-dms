<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { startHeartbeat, stopHeartbeat } from './utils/heartbeat.js'
import './utils/feedback.js'
import config from './utils/config.js'

function applyFontSize(size) {
  // #ifdef H5
  const html = document.documentElement
  if (html) {
    html.classList.remove('font-large', 'font-xlarge')
    if (size === 'large') html.classList.add('font-large')
    if (size === 'xlarge') html.classList.add('font-xlarge')
  }
  // #endif
}

onLaunch(() => {
  console.log('App Launch')
  const token = uni.getStorageSync('pda_token')
  if (token) {
    startHeartbeat()
  }
  // 初始化字号
  const fontSize = uni.getStorageSync('ygt-font-size') || 'normal'
  applyFontSize(fontSize)
  // 监听字号变化
  uni.$on('ygt-font-size-change', (size) => {
    applyFontSize(size)
  })
})

onShow(() => {
  console.log('App Show')
  // 检查登录状态：非登录页没有 token 时重定向
  const token = uni.getStorageSync(config.tokenKey)
  if (!token) {
    const pages = getCurrentPages()
    if (pages.length > 0) {
      const route = pages[pages.length - 1].route || ''
      if (!route.includes('login')) {
        uni.reLaunch({ url: '/pages/login/index' })
      }
    }
  }
})

onHide(() => {
  console.log('App Hide')
})
</script>

<style lang="scss">
@import "uni.scss";

page {
  background: $ygt-gray-50;
  color: $ygt-text-primary;
  font-family: -apple-system, "SF Pro Text", "HarmonyOS Sans SC", "PingFang SC", "Hiragino Sans GB", "Noto Sans SC", "Roboto", "Segoe UI Symbol", sans-serif;
}

/* === 安全区域适配 === */
.safe-area-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

/* === 大字模式(三档) === */
page.font-large { font-size: 32rpx; }
page.font-xlarge { font-size: 36rpx; }

/* === 数字等宽 === */
.ygt-num { 
  font-variant-numeric: tabular-nums;
  font-feature-settings: "tnum";
}

/* === 通用卡片 === */
.ygt-card {
  background: #fff;
  border-radius: $ygt-radius-lg;
  box-shadow: $ygt-shadow-card;
  padding: 32rpx;
}

/* === 主按钮 === */
.ygt-btn-primary {
  background: $ygt-primary;
  color: #fff;
  border-radius: $ygt-radius-md;
  height: 96rpx;
  line-height: 96rpx;
  font-size: 32rpx;
  font-weight: 500;
}

/* === 状态色块 === */
.ygt-tag {
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  font-size: 24rpx;
  display: inline-block;
}
.ygt-tag-success { background: rgba(22, 163, 74, 0.1); color: $ygt-success; }
.ygt-tag-warning { background: rgba(217, 119, 6, 0.1); color: $ygt-warning; }
.ygt-tag-danger  { background: rgba(220, 38, 38, 0.1); color: $ygt-danger; }
</style>
