<template>
  <view class="container">
    <view class="user-card">
      <view class="avatar">
        <text class="avatar-text">👤</text>
      </view>
      <view class="user-info">
        <text class="user-name">{{ userInfo.userCode || '未登录' }}</text>
        <text class="user-device">{{ userInfo.deviceCode || '未绑定设备' }}</text>
      </view>
    </view>
    
    <view class="menu-list">
      <view class="menu-item" @click="goOnlineList">
        <text class="menu-icon">👥</text>
        <text class="menu-text">在线用户</text>
        <text class="menu-arrow">></text>
      </view>
      <view class="menu-item" @click="syncData">
        <text class="menu-icon">🔄</text>
        <text class="menu-text">同步离线数据</text>
        <text class="menu-badge" v-if="pendingCount > 0">{{ pendingCount }}</text>
        <text class="menu-arrow" v-else>></text>
      </view>
      <view class="menu-item" @click="clearCache">
        <text class="menu-icon">🗑</text>
        <text class="menu-text">清理缓存</text>
        <text class="menu-arrow">></text>
      </view>
    </view>
    
    <view class="logout-area">
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </view>
    
    <view class="version">
      <text>版本 v1.0.0</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onShow } from 'vue'
import config from '../../utils/config.js'
import { getQueue } from '../../utils/storage.js'
import { syncAll } from '../../utils/sync.js'
import { stopHeartbeat } from '../../utils/heartbeat.js'

const userInfo = ref({})
const pendingCount = ref(0)

onShow(() => {
  userInfo.value = uni.getStorageSync(config.userInfoKey) || {}
  pendingCount.value = getQueue().length
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
  } finally {
    uni.hideLoading()
  }
}

function clearCache() {
  uni.showModal({
    title: '确认清理',
    content: '将清理所有本地缓存（不含待同步数据）',
    success: (res) => {
      if (res.confirm) {
        uni.clearStorageSync()
        // 保留用户信息和待同步数据
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

<style scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40rpx;
}

.user-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 80rpx 40rpx 60rpx;
  display: flex;
  align-items: center;
  gap: 30rpx;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  background: rgba(255,255,255,0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-text {
  font-size: 60rpx;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 36rpx;
  font-weight: bold;
  color: #fff;
}

.user-device {
  font-size: 26rpx;
  color: rgba(255,255,255,0.8);
  margin-top: 8rpx;
}

.menu-list {
  margin: 30rpx;
  background: #fff;
  border-radius: 16rpx;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  font-size: 40rpx;
  margin-right: 20rpx;
}

.menu-text {
  flex: 1;
  font-size: 30rpx;
  color: #333;
}

.menu-badge {
  background: #ff5252;
  color: #fff;
  font-size: 24rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
  margin-right: 10rpx;
}

.menu-arrow {
  font-size: 28rpx;
  color: #999;
}

.logout-area {
  margin: 40rpx 30rpx;
}

.logout-btn {
  height: 90rpx;
  line-height: 90rpx;
  background: #fff;
  color: #ff5252;
  font-size: 30rpx;
  border-radius: 12rpx;
}

.version {
  text-align: center;
  padding: 20rpx;
}

.version text {
  font-size: 24rpx;
  color: #999;
}
</style>
