<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { startHeartbeat, stopHeartbeat } from './utils/heartbeat.js'

onLaunch(() => {
  console.log('App Launch')
  // 如果有token，启动心跳
  const token = uni.getStorageSync('pda_token')
  if (token) {
    startHeartbeat()
  }
})

onShow(() => {
  console.log('App Show')
})

onHide(() => {
  console.log('App Hide')
})
</script>

<style>
/* 全局样式 */
page {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f5f5;
}

/* 安全区域适配 */
.safe-area-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}
</style>
