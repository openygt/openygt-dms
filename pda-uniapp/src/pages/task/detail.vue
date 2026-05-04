<template>
  <view class="container">
    <!-- 任务信息卡 -->
    <view class="task-card">
      <view class="task-header">
        <view class="task-status-row">
          <text class="task-status-dot" :style="{ background: statusColor }"></text>
          <text class="task-status-name" data-testid="task-status">{{ task.statusName || task.status }}</text>
        </view>
        <text class="task-barcode task-num-highlight" data-testid="task-barcode">{{ task.barcode }}</text>
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

    <!-- 煎药流程进度 -->
    <view class="progress-section">
      <text class="section-title">煎药流程进度</text>
      <view class="progress-steps">
        <view
          class="step-node"
          v-for="(step, idx) in decoctionSteps"
          :key="step.key"
          :class="{
            'step-done': step.status === 'done',
            'step-current': step.status === 'current',
            'step-pending': step.status === 'pending'
          }"
        >
          <!-- 连接线 -->
          <view class="step-connector" v-if="idx > 0">
            <view class="connector-line" :class="{ 'line-active': step.status !== 'pending' }"></view>
          </view>

          <view class="step-marker">
            <view class="step-circle">
              <text class="step-check" v-if="step.status === 'done'">✓</text>
              <text class="step-num" v-else>{{ idx + 1 }}</text>
            </view>
          </view>

          <view class="step-body">
            <text class="step-name">{{ step.name }}</text>
            <text class="step-plan-time" v-if="step.planTime">计划: {{ step.planTime }}</text>
            <text class="step-actual-time" v-if="step.actualTime">实际: {{ step.actualTime }}</text>
            <text class="step-hint" v-if="step.status === 'current'">进行中</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 药材分组展示 -->
    <view class="medicine-group-section" v-if="prescription && prescription.groups && prescription.groups.length > 0">
      <text class="section-title">药材分组 ({{ prescription.groups.length }}组)</text>
      <view class="group-list">
        <view
          class="group-card"
          v-for="group in prescription.groups"
          :key="group.type"
          :class="getGroupClass(group.type)"
        >
          <view class="group-header">
            <text class="group-tag" :class="getGroupClass(group.type)">{{ group.name }}</text>
            <text class="group-count">{{ group.medicines.length }}味</text>
          </view>
          <view class="group-medicines">
            <view class="group-med-item" v-for="(med, mIdx) in group.medicines" :key="mIdx">
              <text class="group-med-name">{{ med.name }}</text>
              <text class="group-med-dosage">{{ med.dosage }}{{ med.unit }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 药材清单（无分组时显示） -->
    <view class="medicine-section" v-else-if="task.medicines && task.medicines.length > 0">
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
        <button class="sub-btn" @click="goTemperature" data-testid="btn-go-temp">温度曲线</button>
        <button class="sub-btn" @click="goLog" data-testid="btn-go-log">操作日志</button>
        <button class="sub-btn" @click="goReprint" data-testid="btn-reprint">重打印</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get } from '../../utils/request.js'

const task = ref({})
const prescription = ref({})
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

// 煎药流程步骤：泡药→头煎→二煎→合并→过滤→包装
const decoctionSteps = computed(() => {
  const stepDefs = [
    { key: 'SOAK', name: '泡药' },
    { key: 'FIRST_DECOCT', name: '头煎' },
    { key: 'SECOND_DECOCT', name: '二煎' },
    { key: 'MERGE', name: '合并' },
    { key: 'FILTER', name: '过滤' },
    { key: 'PACKAGE', name: '包装' }
  ]
  const steps = task.value.decoctionSteps || task.value.steps || []
  let currentFound = false
  return stepDefs.map(def => {
    const s = steps.find(x => x.key === def.key || x.stepType === def.key || x.label === def.name)
    let status = 'pending'
    if (s) {
      if (s.completed === true || s.status === 'done') {
        status = 'done'
      } else if (s.current === true || s.status === 'current') {
        status = 'current'
        currentFound = true
      } else if (!currentFound) {
        status = 'done'
      }
    } else if (!currentFound && task.value.status === 'COMPLETED') {
      status = 'done'
    }
    return {
      ...def,
      status,
      planTime: s ? (s.planTime || s.planTimeStr) : '',
      actualTime: s ? (s.actualTime || s.actualTimeStr || s.time) : ''
    }
  })
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
    // 如果有关联处方信息，赋值给 prescription
    if (res.prescription) {
      prescription.value = res.prescription
    } else if (res.medicines && res.medicines.length > 0) {
      // 构造默认不分组的药材清单，用于兼容旧数据
      prescription.value = {
        groups: [{
          type: 'NORMAL',
          name: '群煎组',
          medicines: res.medicines
        }]
      }
    }
  } catch (e) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function getGroupClass(type) {
  const map = {
    PRE_DECOCT: 'group-red',
    NORMAL: 'group-blue',
    POST_DECOCT: 'group-green',
    WRAP: 'group-orange',
    DISSOLVE: 'group-purple'
  }
  return map[type] || 'group-blue'
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
function goTemperature() {
  uni.navigateTo({ url: `/pages/task/temperature?barcode=${barcode.value}` })
}
function goLog() {
  uni.switchTab({ url: '/pages/log/list' })
}
function goReprint() {
  uni.navigateTo({ url: `/pages/reprint/index?barcode=${barcode.value}` })
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
.task-num-highlight {
  display: inline-block;
  background: #0066CC;
  color: #fff;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  font-size: 26rpx;
}
.task-info { border-top: 1rpx solid #f0f0f0; padding-top: 20rpx; }
.info-row { display: flex; justify-content: space-between; padding: 12rpx 0; }
.info-label { font-size: 28rpx; color: #666; }
.info-value { font-size: 28rpx; color: #333; font-weight: 500; }

/* 进度区域 */
.progress-section { background: #fff; margin: 20rpx; border-radius: 16rpx; padding: 30rpx; }
.progress-steps { display: flex; flex-wrap: wrap; }
.step-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 33.33%;
  padding: 20rpx 0;
  position: relative;
}
.step-connector {
  position: absolute;
  left: -50%;
  top: 46rpx;
  width: 100%;
  height: 4rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.connector-line {
  width: 60%;
  height: 4rpx;
  background: #e0e0e0;
  border-radius: 2rpx;
}
.connector-line.line-active {
  background: #4caf50;
}
.step-marker { margin-bottom: 12rpx; }
.step-circle {
  width: 68rpx;
  height: 68rpx;
  border-radius: 50%;
  background: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.step-done .step-circle {
  background: #4caf50;
}
.step-current .step-circle {
  background: #0066CC;
  animation: pulse-ring 2s ease-in-out infinite;
}
.step-check {
  font-weight: 600;
  font-size: 32rpx;
  color: #fff;
}
.step-num {
  font-size: 28rpx;
  font-weight: 600;
  color: #999;
}
.step-current .step-num {
  color: #fff;
}
.step-body {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.step-name {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
}
.step-plan-time {
  font-size: 22rpx;
  color: #999;
  margin-top: 6rpx;
}
.step-actual-time {
  font-size: 22rpx;
  color: #4caf50;
  margin-top: 4rpx;
}
.step-hint {
  font-size: 22rpx;
  color: #0066CC;
  margin-top: 4rpx;
}
.step-pending .step-name { color: #999; }
.step-pending .step-num { color: #bbb; }

@keyframes pulse-ring {
  0%, 100% { box-shadow: 0 0 0 0 rgba(0, 102, 204, 0.2); }
  50% { box-shadow: 0 0 0 10rpx rgba(0, 102, 204, 0); }
}

/* 药材分组 */
.medicine-group-section { background: #fff; margin: 20rpx; border-radius: 16rpx; padding: 30rpx; }
.group-list { display: flex; flex-direction: column; gap: 20rpx; }
.group-card {
  border-radius: 12rpx;
  padding: 24rpx;
  border-left: 8rpx solid #ccc;
  background: #fafafa;
}
.group-card.group-red { border-left-color: #ef5350; background: #fff5f5; }
.group-card.group-blue { border-left-color: #2196f3; background: #f0f7ff; }
.group-card.group-green { border-left-color: #4caf50; background: #f0fff0; }
.group-card.group-orange { border-left-color: #ff9800; background: #fff8e1; }
.group-card.group-purple { border-left-color: #9c27b0; background: #f3e5f5; }
.group-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.group-tag {
  font-size: 26rpx;
  font-weight: 600;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
}
.group-tag.group-red { color: #c62828; background: #ffcdd2; }
.group-tag.group-blue { color: #1565c0; background: #bbdefb; }
.group-tag.group-green { color: #2e7d32; background: #c8e6c9; }
.group-tag.group-orange { color: #e65100; background: #ffe0b2; }
.group-tag.group-purple { color: #6a1b9a; background: #e1bee7; }
.group-count { font-size: 24rpx; color: #999; }
.group-medicines { display: flex; flex-wrap: wrap; gap: 12rpx; }
.group-med-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  background: rgba(255,255,255,0.8);
  padding: 8rpx 16rpx;
  border-radius: 8rpx;
}
.group-med-name { font-size: 26rpx; color: #333; }
.group-med-dosage { font-size: 24rpx; color: #666; }

/* 原药材清单 */
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
