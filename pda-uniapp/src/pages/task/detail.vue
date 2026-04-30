<template>
  <view class="container">
    <!-- 任务信息卡 -->
    <view class="task-card">
      <view class="task-header">
        <view class="task-status-row">
          <text class="task-status-dot" :style="{ background: statusColor }"></text>
          <text class="task-status-name" data-testid="task-status">{{ task.statusName || task.status }}</text>
        </view>
        <text class="task-barcode" data-testid="task-barcode">{{ task.barcode }}</text>
      </view>
      <view class="task-info">
        <view class="info-row">
          <text class="info-label">患者</text>
          <text class="info-value">{{ task.patientName || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">处方</text>
          <text class="info-value">{{ task.prescriptionNumber || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">医院</text>
          <text class="info-value">{{ task.hospitalName || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">剂数</text>
          <text class="info-value">{{ task.repetition || '-' }} 剂</text>
        </view>
        <view class="info-row" v-if="task.deviceName">
          <text class="info-label">设备</text>
          <text class="info-value">{{ task.deviceName }}</text>
        </view>
      </view>
    </view>

    <!-- 药材清单 -->
    <view class="medicine-section" v-if="task.medicines && task.medicines.length > 0">
      <text class="section-title">药材清单 ({{ task.medicines.length }}味)</text>
      <view class="medicine-list">
        <view class="medicine-item" v-for="(med, idx) in task.medicines" :key="idx">
          <text class="med-name">{{ med.name }}</text>
          <text class="med-dosage">{{ med.dosage }}{{ med.unit }}</text>
        </view>
      </view>
    </view>

    <!-- 工序时间线 -->
    <view class="timeline-section">
      <text class="section-title">工序进度</text>
      <ygt-step-timeline :steps="task.steps || []" @click-step="onClickStep" />
    </view>

    <!-- 底部操作区 -->
    <view class="action-footer">
      <button class="main-btn" :class="{ disabled: !canAction }" :disabled="!canAction" @click="handleMainAction" data-testid="btn-main-action">
        {{ mainActionText }}
      </button>
      <view class="sub-actions">
        <button class="sub-btn" @click="goPhoto" data-testid="btn-go-photo">拍照留档</button>
        <button class="sub-btn" @click="goLog" data-testid="btn-go-log">操作日志</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get } from '../../utils/request.js'

const task = ref({})
const barcode = ref('')

const statusColor = computed(() => {
  const colors = {
    PENDING: '#999', SOAKING: '#2196f3', SOAKED: '#64b5f6',
    DECOCTING: '#ff9800', DECOCTED: '#ffb74d', POURING: '#9c27b0',
    POURED: '#ba68c8', PACKAGING: '#795548', PACKAGED: '#a1887f',
    LABELING: '#607d8b', LABELED: '#78909c', INSPECTING: '#e91e63',
    COMPLETED: '#4caf50', CANCELLED: '#9e9e9e'
  }
  return colors[task.value.status] || '#999'
})

const mainActionText = computed(() => {
  const action = task.value.nextAction
  if (!action) {
    if (task.value.status === 'COMPLETED') return '已完成'
    if (task.value.status === 'CANCELLED') return '已取消'
    return '无操作'
  }
  return action.text
})

const canAction = computed(() => {
  return task.value.nextAction != null && task.value.status !== 'COMPLETED' && task.value.status !== 'CANCELLED'
})

onLoad((options) => {
  barcode.value = options.barcode || ''
  if (barcode.value) {
    loadTask()
  }
})

async function loadTask() {
  try {
    uni.showLoading({ title: '加载中' })
    const res = await get(`/task/${barcode.value}`)
    task.value = res
  } catch (e) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function handleMainAction() {
  const action = task.value.nextAction
  if (!action) return

  // 交接签字
  if (action.stepType === 'HANDOVER_SIGN') {
    uni.navigateTo({
      url: `/pages/handover/sign?taskId=${task.value.taskId}&barcode=${barcode.value}`
    })
    return
  }

  // 需要设备绑定 → 跳转设备绑定页
  if (action.needDevice && !task.value.deviceCode) {
    uni.navigateTo({
      url: `/pages/device/bind?taskId=${task.value.taskId}&stepType=${action.stepType}&barcode=${barcode.value}`
    })
    return
  }

  // 需要拍照 → 跳转拍照页（强制模式）
  if (action.needPhoto) {
    uni.navigateTo({
      url: `/pages/photo/upload?taskId=${task.value.taskId}&stepType=${action.stepType}&mode=force`
    })
    return
  }

  // 普通工序确认
  const deviceId = task.value.decoctDeviceId || task.value.packageDeviceId || ''
  uni.navigateTo({
    url: `/pages/task/confirm?taskId=${task.value.taskId}&stepType=${action.stepType}&barcode=${barcode.value}&deviceId=${deviceId}`
  })
}

function onClickStep(step) {
  if (!step.completed && !step.current) {
    uni.showToast({ title: '请先完成前置工序', icon: 'none' })
    uni.vibrateShort()
  }
}

function goPhoto() {
  uni.navigateTo({ url: `/pages/photo/upload?taskId=${task.value.taskId}` })
}
function goLog() {
  uni.switchTab({ url: '/pages/log/list' })
}
</script>

<style scoped>
.container { padding-bottom: 200rpx; }
.task-card { background: #fff; margin: 20rpx; border-radius: 16rpx; padding: 30rpx; }
.task-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.task-status-row { display: flex; align-items: center; }
.task-status-dot { width: 16rpx; height: 16rpx; border-radius: 50%; margin-right: 12rpx; }
.task-status-name { font-size: 32rpx; font-weight: 600; color: #333; }
.task-barcode { font-size: 26rpx; color: #999; }
.task-info { border-top: 1rpx solid #f0f0f0; padding-top: 20rpx; }
.info-row { display: flex; justify-content: space-between; padding: 12rpx 0; }
.info-label { font-size: 28rpx; color: #666; }
.info-value { font-size: 28rpx; color: #333; font-weight: 500; }
.medicine-section { background: #fff; margin: 20rpx; border-radius: 16rpx; padding: 30rpx; }
.section-title { font-size: 30rpx; font-weight: 600; color: #333; margin-bottom: 20rpx; display: block; }
.medicine-list { display: flex; flex-wrap: wrap; gap: 16rpx; }
.medicine-item { background: #f5f5f5; padding: 12rpx 20rpx; border-radius: 8rpx; display: flex; align-items: center; gap: 12rpx; }
.med-name { font-size: 28rpx; color: #333; }
.med-dosage { font-size: 26rpx; color: #666; }
.timeline-section { background: #fff; margin: 20rpx; border-radius: 16rpx; padding: 30rpx; }
.action-footer { position: fixed; bottom: 0; left: 0; right: 0; background: #fff; padding: 20rpx 30rpx; box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.06); }
.main-btn { height: 96rpx; background: #0066CC; color: #fff; font-size: 34rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; margin-bottom: 16rpx; }
.main-btn.disabled { background: #ccc; }
.sub-actions { display: flex; gap: 20rpx; }
.sub-btn { flex: 1; height: 80rpx; background: #f5f5f5; color: #555; font-size: 28rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; }
</style>
