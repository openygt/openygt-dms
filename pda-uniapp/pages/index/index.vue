<template>
  <view class="container">
    <view class="header">
      <text class="greeting">{{ greeting }}，{{ userInfo.userCode || '操作员' }}</text>
      <text class="device">设备：{{ userInfo.deviceCode || '未绑定' }}</text>
    </view>
    
    <view class="quick-actions">
      <view class="action-grid">
        <view class="action-item" @click="goScan">
          <view class="action-icon scan">📷</view>
          <text class="action-text">扫码查询</text>
        </view>
        <view class="action-item" @click="goConfirm">
          <view class="action-icon confirm">✓</view>
          <text class="action-text">工序确认</text>
        </view>
        <view class="action-item" @click="goPhoto">
          <view class="action-icon photo">📸</view>
          <text class="action-text">拍照上传</text>
        </view>
        <view class="action-item" @click="goLog">
          <view class="action-icon log">📋</view>
          <text class="action-text">操作日志</text>
        </view>
      </view>
    </view>
    
    <view class="recent-tasks" v-if="recentTasks.length > 0">
      <view class="section-title">
        <text>最近处理任务</text>
        <text class="more" @click="goScan">查看更多 ></text>
      </view>
      <view class="task-list">
        <view class="task-item" v-for="task in recentTasks" :key="task.id" @click="goTaskDetail(task)">
          <view class="task-info">
            <text class="task-name">{{ task.name }}</text>
            <text class="task-status" :class="task.status">{{ task.statusText }}</text>
          </view>
          <text class="task-time">{{ task.time }}</text>
        </view>
      </view>
    </view>
    
    <view class="sync-status" v-if="pendingCount > 0">
      <view class="sync-bar" @click="doSync">
        <text class="sync-text">{{ pendingCount }} 条数据待同步</text>
        <text class="sync-btn">立即同步</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onShow } from 'vue'
import config from '../../utils/config.js'
import { getQueue } from '../../utils/storage.js'
import { syncAll } from '../../utils/sync.js'

const userInfo = ref(uni.getStorageSync(config.userInfoKey) || {})
const pendingCount = ref(0)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const recentTasks = ref([
  { id: 1, name: '任务-JY-20240428-001', status: 'processing', statusText: '煎药中', time: '10:30' },
  { id: 2, name: '任务-JY-20240428-002', status: 'pending', statusText: '待泡药', time: '09:15' }
])

onShow(() => {
  userInfo.value = uni.getStorageSync(config.userInfoKey) || {}
  pendingCount.value = getQueue().length
})

function goScan() {
  uni.switchTab({ url: '/pages/task/scan' })
}

function goConfirm() {
  uni.navigateTo({ url: '/pages/task/confirm' })
}

function goPhoto() {
  uni.navigateTo({ url: '/pages/photo/upload' })
}

function goLog() {
  uni.switchTab({ url: '/pages/log/list' })
}

function goTaskDetail(task) {
  uni.navigateTo({ url: `/pages/task/detail?taskId=${task.id}` })
}

async function doSync() {
  uni.showLoading({ title: '同步中...' })
  try {
    await syncAll()
    pendingCount.value = getQueue().length
  } finally {
    uni.hideLoading()
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40rpx;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60rpx 40rpx 80rpx;
  color: #fff;
}

.greeting {
  font-size: 40rpx;
  font-weight: bold;
}

.device {
  font-size: 26rpx;
  opacity: 0.8;
  margin-top: 10rpx;
}

.quick-actions {
  margin: -40rpx 30rpx 30rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.08);
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 0;
}

.action-icon {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  margin-bottom: 10rpx;
}

.scan { background: #e3f2fd; }
.confirm { background: #e8f5e9; }
.photo { background: #fff3e0; }
.log { background: #f3e5f5; }

.action-text {
  font-size: 24rpx;
  color: #666;
}

.recent-tasks {
  margin: 0 30rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
  font-size: 32rpx;
  font-weight: bold;
}

.more {
  font-size: 26rpx;
  color: #667eea;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.task-info {
  display: flex;
  flex-direction: column;
}

.task-name {
  font-size: 28rpx;
  color: #333;
}

.task-status {
  font-size: 24rpx;
  margin-top: 6rpx;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}

.task-status.processing {
  color: #ff9800;
  background: #fff3e0;
}

.task-status.pending {
  color: #2196f3;
  background: #e3f2fd;
}

.task-time {
  font-size: 24rpx;
  color: #999;
}

.sync-status {
  margin: 30rpx;
}

.sync-bar {
  background: #fff3e0;
  border-radius: 12rpx;
  padding: 24rpx 30rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sync-text {
  font-size: 28rpx;
  color: #e65100;
}

.sync-btn {
  font-size: 26rpx;
  color: #fff;
  background: #ff9800;
  padding: 10rpx 24rpx;
  border-radius: 8rpx;
}
</style>
