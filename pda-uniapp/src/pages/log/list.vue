<template>
  <view class="container">
    <view class="filter-bar">
      <view class="filter-item" :class="{ active: filter === 'all' }" @click="filter = 'all'">
        <text>全部</text>
      </view>
      <view class="filter-item" :class="{ active: filter === 'today' }" @click="filter = 'today'">
        <text>今天</text>
      </view>
      <view class="filter-item" :class="{ active: filter === 'week' }" @click="filter = 'week'">
        <text>本周</text>
      </view>
    </view>
    
    <view class="log-list">
      <view class="log-item" v-for="(log, idx) in filteredLogs" :key="idx">
        <view class="log-header">
          <view class="log-type" :class="log.operResult">{{ log.operType }}</view>
          <text class="log-time">{{ log.operTime }}</text>
        </view>
        <view class="log-body">
          <text class="log-desc">{{ log.operDesc }}</text>
          <text class="log-detail" v-if="log.taskId">任务：{{ log.taskId }}</text>
        </view>
        <view class="log-footer">
          <text class="log-device" v-if="log.deviceCode">设备：{{ log.deviceCode }}</text>
          <text class="log-result" :class="log.operResult">{{ log.operResult === 'SUCCESS' ? '成功' : '失败' }}</text>
        </view>
      </view>
    </view>
    
    <view class="empty-state" v-if="filteredLogs.length === 0">
      <text class="empty-icon">📋</text>
      <text class="empty-text">暂无操作记录</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { get } from '../../utils/request.js'

const logs = ref([])
const filter = ref('all')

const filteredLogs = computed(() => {
  if (filter.value === 'all') return logs.value
  
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  
  if (filter.value === 'today') {
    return logs.value.filter(log => new Date(log.operTime) >= today)
  }
  
  if (filter.value === 'week') {
    const weekStart = new Date(today)
    weekStart.setDate(weekStart.getDate() - weekStart.getDay())
    return logs.value.filter(log => new Date(log.operTime) >= weekStart)
  }
  
  return logs.value
})

onShow(() => {
  loadLogs()
})

async function loadLogs() {
  try {
    const res = await get('/log/recent', { limit: 50 })
    logs.value = res || []
  } catch (e) {
    // 离线时显示本地缓存
    logs.value = uni.getStorageSync('pda_local_logs') || []
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
}

.filter-bar {
  display: flex;
  background: #fff;
  padding: 20rpx 30rpx;
  gap: 20rpx;
}

.filter-item {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  background: #f5f5f5;
  border-radius: 8rpx;
  font-size: 28rpx;
  color: #666;
}

.filter-item.active {
  background: #0066CC;
  color: #fff;
}

.log-list {
  padding: 20rpx 30rpx;
}

.log-item {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.log-type {
  font-size: 24rpx;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  background: #e3f2fd;
  color: #2196f3;
}

.log-type.FAILED {
  background: #ffebee;
  color: #f44336;
}

.log-time {
  font-size: 24rpx;
  color: #999;
}

.log-body {
  margin-bottom: 16rpx;
}

.log-desc {
  font-size: 28rpx;
  color: #333;
  display: block;
}

.log-detail {
  font-size: 24rpx;
  color: #666;
  margin-top: 8rpx;
  display: block;
}

.log-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16rpx;
  border-top: 1rpx solid #f0f0f0;
}

.log-device {
  font-size: 24rpx;
  color: #999;
}

.log-result {
  font-size: 24rpx;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
}

.log-result.SUCCESS {
  color: #4caf50;
  background: #e8f5e9;
}

.log-result.FAILED {
  color: #f44336;
  background: #ffebee;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 40rpx;
}

.empty-icon {
  font-size: 100rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}
</style>
