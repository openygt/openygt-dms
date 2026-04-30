<template>
  <view class="mine-page">
    <!-- 用户信息卡 -->
    <view class="user-card">
      <view class="avatar">
        <text class="avatar-text">☺</text>
      </view>
      <view class="user-info">
        <text class="user-name">{{ userInfo.userCode || '未登录' }}</text>
        <text class="user-device">{{ userInfo.deviceCode || '未绑定设备' }}</text>
      </view>
    </view>

    <!-- 设置列表 -->
    <view class="setting-group">
      <view class="setting-item" @click="goOnlineList">
        <view class="setting-left">
          <view class="setting-icon" style="background: rgba(0,102,204,0.1)">
            <text class="setting-icon-text" style="color: $ygt-primary">●</text>
          </view>
          <text class="setting-label">在线用户</text>
        </view>
        <text class="setting-arrow">›</text>
      </view>

      <view class="setting-item" @click="syncData">
        <view class="setting-left">
          <view class="setting-icon" style="background: rgba(22,163,74,0.1)">
            <text class="setting-icon-text" style="color: $ygt-success">⟳</text>
          </view>
          <text class="setting-label">同步离线数据</text>
        </view>
        <view class="setting-right">
          <text class="setting-badge" v-if="pendingCount > 0">{{ pendingCount }}</text>
          <text class="setting-arrow">›</text>
        </view>
      </view>

      <view class="setting-item" @click="toggleFontSize">
        <view class="setting-left">
          <view class="setting-icon" style="background: rgba(217,119,6,0.1)">
            <text class="setting-icon-text" style="color: $ygt-warning">⚙</text>
          </view>
          <text class="setting-label">字号大小</text>
        </view>
        <view class="setting-right">
          <text class="setting-value">{{ fontSizeLabel }}</text>
          <text class="setting-arrow">›</text>
        </view>
      </view>

      <view class="setting-item" @click="toggleSound">
        <view class="setting-left">
          <view class="setting-icon" style="background: rgba(8,145,178,0.1)">
            <text class="setting-icon-text" style="color: $ygt-info">♪</text>
          </view>
          <text class="setting-label">音效反馈</text>
        </view>
        <view class="setting-right">
          <text class="setting-value">{{ soundEnabled ? '开启' : '关闭' }}</text>
          <text class="setting-arrow">›</text>
        </view>
      </view>

      <view class="setting-item" @click="clearCache">
        <view class="setting-left">
          <view class="setting-icon" style="background: rgba(220,38,38,0.1)">
            <text class="setting-icon-text" style="color: $ygt-danger">✕</text>
          </view>
          <text class="setting-label">清理缓存</text>
        </view>
        <text class="setting-arrow">›</text>
      </view>
    </view>

    <!-- 退出 -->
    <view class="logout-area">
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </view>

    <view class="version">
      <text>OpenYGT PDA v1.0.0</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import config from '../../utils/config.js'
import { getQueue } from '../../utils/storage.js'
import { syncAll } from '../../utils/sync.js'
import { stopHeartbeat } from '../../utils/heartbeat.js'

const userInfo = ref({})
const pendingCount = ref(0)
const fontSizeLabel = ref('标准')
const soundEnabled = ref(true)

const FONT_LABELS = { normal: '标准', large: '大', xlarge: '特大' }

onShow(() => {
  userInfo.value = uni.getStorageSync(config.userInfoKey) || {}
  pendingCount.value = getQueue().length

  const size = uni.getStorageSync('ygt-font-size') || 'normal'
  fontSizeLabel.value = FONT_LABELS[size] || '标准'

  const sound = uni.getStorageSync('ygt-sound-enabled')
  soundEnabled.value = sound !== false
})

function goOnlineList() {
  uni.navigateTo({ url: '/pages/online/list' })
}

async function syncData() {
  if (pendingCount.value === 0) {
    uni.showToast({ title: '没有待同步数据', icon: 'none' })
    return
  }
  uni.showLoading({ title: '同步中...' })
  try {
    await syncAll()
    pendingCount.value = getQueue().length
    if (uni.$ygtFeedback) uni.$ygtFeedback.success()
  } catch (e) {
    if (uni.$ygtFeedback) uni.$ygtFeedback.error()
  } finally {
    uni.hideLoading()
  }
}

function toggleFontSize() {
  const SIZES = ['normal', 'large', 'xlarge']
  const cur = uni.getStorageSync('ygt-font-size') || 'normal'
  const next = SIZES[(SIZES.indexOf(cur) + 1) % SIZES.length]

  uni.setStorageSync('ygt-font-size', next)
  fontSizeLabel.value = FONT_LABELS[next]
  uni.$emit('ygt-font-size-change', next)

  uni.showToast({ title: '已切换：' + fontSizeLabel.value, icon: 'none' })
}

function toggleSound() {
  const next = !soundEnabled.value
  uni.setStorageSync('ygt-sound-enabled', next)
  soundEnabled.value = next
  uni.showToast({ title: next ? '音效已开启' : '音效已关闭', icon: 'none' })
}

function clearCache() {
  uni.showModal({
    title: '确认清理',
    content: '将清理所有本地缓存（不含待同步数据）',
    success: (res) => {
      if (res.confirm) {
        const token = uni.getStorageSync(config.tokenKey)
        const user = uni.getStorageSync(config.userInfoKey)
        const queue = getQueue()

        uni.clearStorageSync()

        // 保留关键数据
        if (token) uni.setStorageSync(config.tokenKey, token)
        if (user) uni.setStorageSync(config.userInfoKey, user)
        if (queue.length) uni.setStorageSync('sync_queue', JSON.stringify(queue))

        uni.showToast({ title: '清理完成', icon: 'success' })
      }
    }
  })
}

async function handleLogout() {
  uni.showModal({
    title: '确认退出',
    content: '退出后将停止心跳保活',
    success: async (res) => {
      if (res.confirm) {
        try {
          await uni.request({
            url: config.baseUrl + '/auth/logout',
            method: 'POST',
            header: {
              'Authorization': `Bearer ${uni.getStorageSync(config.tokenKey)}`
            }
          })
        } catch (e) {
          // 忽略网络错误
        }

        stopHeartbeat()
        uni.removeStorageSync(config.tokenKey)
        uni.removeStorageSync(config.userInfoKey)
        uni.reLaunch({ url: '/pages/login/index' })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
@import "../../uni.scss";

.mine-page {
  min-height: 100vh;
  padding-bottom: 40rpx;
}

/* 用户信息卡 */
.user-card {
  background: linear-gradient(135deg, $ygt-primary 0%, $ygt-primary-dark 100%);
  padding: 80rpx 40rpx 60rpx;
  display: flex;
  align-items: center;
  gap: 30rpx;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-text {
  font-weight: 500;
  font-size: 60rpx;
  color: #fff;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 36rpx;
  font-weight: 600;
  color: #fff;
}

.user-device {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 8rpx;
}

/* 设置列表 */
.setting-group {
  margin: 24rpx;
  background: #fff;
  border-radius: $ygt-radius-lg;
  box-shadow: $ygt-shadow-card;
  overflow: hidden;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid $ygt-gray-100;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.setting-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: $ygt-radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
}


.setting-label {
  font-size: 30rpx;
  color: $ygt-gray-900;
}

.setting-right {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.setting-value {
  font-size: 26rpx;
  color: $ygt-gray-500;
}

.setting-badge {
  background: $ygt-danger;
  color: #fff;
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
  min-width: 32rpx;
  text-align: center;
}

.setting-arrow {
  font-weight: 500;
  font-size: 28rpx;
  color: $ygt-gray-400;
}

/* 退出 */
.logout-area {
  margin: 40rpx 24rpx;
}

.logout-btn {
  height: 90rpx;
  line-height: 90rpx;
  background: #fff;
  color: $ygt-danger;
  font-size: 30rpx;
  border-radius: $ygt-radius-lg;
  box-shadow: $ygt-shadow-card;
  font-weight: 500;
}

.version {
  text-align: center;
  padding: 20rpx;
}

.version text {
  font-size: 24rpx;
  color: $ygt-gray-500;
}
</style>
