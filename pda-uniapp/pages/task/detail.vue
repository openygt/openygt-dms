<template>
  <view class="container">
    <view class="task-card">
      <view class="task-header">
        <text class="task-code">{{ task.barcode || taskCode }}</text>
        <text class="task-status" :class="task.status">{{ task.statusText || task.status }}</text>
      </view>
      
      <view class="task-info">
        <view class="info-row">
          <text class="info-label">处方</text>
          <text class="info-value">{{ task.prescriptionName || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">医院</text>
          <text class="info-value">{{ task.hospitalName || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">患者</text>
          <text class="info-value">{{ task.patientName || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">当前工序</text>
          <text class="info-value highlight">{{ task.currentStep || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">关联设备</text>
          <text class="info-value">{{ task.deviceName || '-' }}</text>
        </view>
      </view>
    </view>
    
    <view class="action-area">
      <button class="action-btn primary" @click="goConfirm">工序确认</button>
      <button class="action-btn" @click="goPhoto">拍照留档</button>
    </view>
    
    <view class="step-timeline">
      <text class="section-title">工序进度</text>
      <view class="timeline">
        <view class="timeline-item" v-for="(step, idx) in steps" :key="idx" :class="{ active: step.completed, current: step.isCurrent }">
          <view class="timeline-dot"></view>
          <view class="timeline-content">
            <text class="step-name">{{ step.name }}</text>
            <text class="step-time" v-if="step.time">{{ step.time }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onLoad } from 'vue'

const taskCode = ref('')
const task = ref({})

const steps = ref([
  { name: '处方录入', completed: true, time: '08:30' },
  { name: '泡药', completed: true, time: '09:00' },
  { name: '煎药', completed: false, isCurrent: true },
  { name: '出液', completed: false },
  { name: '包装', completed: false },
  { name: '贴标', completed: false },
  { name: '质检', completed: false },
  { name: '交接', completed: false }
])

onLoad((options) => {
  taskCode.value = options.barcode || options.taskId || ''
  // 模拟数据，实际应调用 API
  task.value = {
    barcode: taskCode.value,
    status: 'processing',
    statusText: '煎药中',
    prescriptionName: '感冒清热方',
    hospitalName: '中医院',
    patientName: '张三',
    currentStep: '煎药',
    deviceName: '煎药机-01'
  }
})

function goConfirm() {
  uni.navigateTo({ url: `/pages/task/confirm?taskId=${task.value.taskId || taskCode.value}` })
}

function goPhoto() {
  uni.navigateTo({ url: `/pages/photo/upload?taskId=${task.value.taskId || taskCode.value}` })
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 30rpx;
}

.task-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 40rpx;
  margin-bottom: 30rpx;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.task-code {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.task-status {
  font-size: 24rpx;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
}

.task-status.processing {
  color: #ff9800;
  background: #fff3e0;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
}

.info-label {
  font-size: 28rpx;
  color: #666;
}

.info-value {
  font-size: 28rpx;
  color: #333;
}

.info-value.highlight {
  color: #0066CC;
  font-weight: bold;
}

.action-area {
  display: flex;
  gap: 20rpx;
  margin-bottom: 30rpx;
}

.action-btn {
  flex: 1;
  height: 90rpx;
  line-height: 90rpx;
  background: #fff;
  color: #0066CC;
  font-size: 30rpx;
  border-radius: 12rpx;
  border: 2rpx solid #0066CC;
}

.action-btn.primary {
  background: #0066CC;
  color: #fff;
}

.step-timeline {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
}

.timeline-item {
  display: flex;
  align-items: flex-start;
  padding: 20rpx 0;
  position: relative;
}

.timeline-item:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 20rpx;
  top: 50rpx;
  width: 2rpx;
  height: 60rpx;
  background: #e0e0e0;
}

.timeline-item.active:not(:last-child)::before {
  background: #0066CC;
}

.timeline-dot {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #e0e0e0;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.timeline-item.active .timeline-dot {
  background: #0066CC;
}

.timeline-item.current .timeline-dot {
  background: #ff9800;
  box-shadow: 0 0 10rpx rgba(255,152,0,0.4);
}

.timeline-content {
  display: flex;
  flex-direction: column;
}

.step-name {
  font-size: 28rpx;
  color: #333;
}

.step-time {
  font-size: 24rpx;
  color: #999;
  margin-top: 6rpx;
}
</style>
