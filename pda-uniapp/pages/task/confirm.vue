<template>
  <view class="confirm-page">
    <!-- 任务信息卡 -->
    <view class="task-info-card">
      <view class="task-row">
        <text class="task-label">任务编号</text>
        <text class="task-value ygt-num">{{ taskId || '-' }}</text>
      </view>
      <view class="task-row">
        <text class="task-label">当前状态</text>
        <text class="task-status" :class="currentStatusClass">{{ currentStep }}</text>
      </view>
    </view>

    <!-- 工序时间线 -->
    <view class="step-timeline">
      <view
        v-for="(step, idx) in steps"
        :key="step.value"
        class="step-node"
        :class="{
          'step-done': step.completed,
          'step-current': step.current,
          'step-pending': !step.completed && !step.current
        }"
        @click="selectStep(step)"
      >
        <!-- 连接线 -->
        <view class="step-line" v-if="idx > 0"></view>

        <view class="step-left">
          <view class="step-circle">
            <text class="step-check" v-if="step.completed">✓</text>
            <text class="step-num" v-else>{{ idx + 1 }}</text>
          </view>
        </view>

        <view class="step-content">
          <text class="step-label">{{ step.label }}</text>
          <text class="step-time ygt-num" v-if="step.completedAt">
            {{ step.completedAt }}
          </text>
          <text class="step-hint" v-else-if="step.current">点击确认</text>
        </view>

        <view class="step-arrow" v-if="step.current">
          <text class="arrow-icon">›</text>
        </view>
      </view>
    </view>

    <!-- 备注 -->
    <view class="remark-card" v-if="selectedStep">
      <text class="remark-label">备注（{{ selectedLabel }}）</text>
      <textarea
        class="remark-input"
        v-model="remark"
        placeholder="请输入备注信息，如异常说明、操作人等"
        maxlength="200"
      />
      <text class="remark-count">{{ remark.length }}/200</text>
    </view>

    <!-- 提交 -->
    <view class="submit-area">
      <button
        class="ygt-btn-primary submit-btn"
        :disabled="loading || !selectedStep"
        :loading="loading"
        @click="handleConfirm"
      >
        <text v-if="!loading">确认工序</text>
        <text v-else>提交中...</text>
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get, post } from '../../utils/request.js'

const taskId = ref('')
const barcode = ref('')
const currentStep = ref('待泡药')
const selectedStep = ref('')
const remark = ref('')
const loading = ref(false)

const steps = ref([
  { value: 'START_SOAK', label: '开始泡药', completed: false, current: false, completedAt: '' },
  { value: 'END_SOAK', label: '结束泡药', completed: false, current: false, completedAt: '' },
  { value: 'START_DECOCT', label: '开始煎药', completed: false, current: false, completedAt: '' },
  { value: 'END_DECOCT', label: '结束煎药', completed: false, current: false, completedAt: '' },
  { value: 'START_POUR', label: '开始出液', completed: false, current: false, completedAt: '' },
  { value: 'END_POUR', label: '结束出液', completed: false, current: false, completedAt: '' },
  { value: 'START_PACKAGE', label: '开始包装', completed: false, current: false, completedAt: '' },
  { value: 'END_PACKAGE', label: '结束包装', completed: false, current: false, completedAt: '' },
  { value: 'LABEL_CONFIRM', label: '贴标确认', completed: false, current: false, completedAt: '' },
  { value: 'INSPECT_PASS', label: '质检通过', completed: false, current: false, completedAt: '' }
])

const selectedLabel = computed(() => {
  const s = steps.value.find(s => s.value === selectedStep.value)
  return s ? s.label : ''
})

const currentStatusClass = computed(() => {
  const current = steps.value.find(s => s.current)
  if (!current) return 'status-done'
  if (current.value.includes('START')) return 'status-running'
  return 'status-pending'
})

onLoad((options) => {
  taskId.value = options.taskId || ''
  barcode.value = options.barcode || ''
  const preselectStep = options.stepType || ''
  loadTaskProgress(preselectStep)
})

async function loadTaskProgress(preselectStep) {
  if (!barcode.value) return
  try {
    uni.showLoading({ title: '加载中' })
    const res = await get(`/task/${barcode.value}`)
    currentStep.value = res.statusName || res.status || '待泡药'
    // 同步后端步骤状态
    if (res.steps && res.steps.length > 0) {
      steps.value = res.steps.map(s => ({
        value: s.value,
        label: s.label,
        completed: s.completed,
        current: s.current,
        completedAt: s.completedAt
      }))
    }
    // 如果有预选中步骤，自动选中
    if (preselectStep) {
      selectedStep.value = preselectStep
    }
  } catch (e) {
    console.error('加载任务进度失败', e)
  } finally {
    uni.hideLoading()
  }
}

function selectStep(step) {
  if (step.completed) {
    uni.showToast({ title: '该工序已完成', icon: 'none' })
    return
  }
  // 只能确认当前工序
  if (!step.current) {
    const currentIdx = steps.value.findIndex(s => s.current)
    const targetIdx = steps.value.indexOf(step)
    if (targetIdx > currentIdx) {
      uni.showToast({ title: '请先完成前置工序', icon: 'none' })
      return
    }
  }
  selectedStep.value = step.value
}

async function handleConfirm() {
  if (!selectedStep.value) {
    if (uni.$ygtFeedback) uni.$ygtFeedback.warning()
    uni.showToast({ title: '请选择要确认的工序', icon: 'none' })
    return
  }

  // 二次确认
  const stepLabel = steps.value.find(s => s.value === selectedStep.value)?.label || ''
  const needConfirm = ['END_DECOCT', 'END_PACKAGE', 'INSPECT_PASS'].includes(selectedStep.value)
  if (needConfirm) {
    const [confirmErr, confirmRes] = await uni.showModal({
      title: '⚠ 确认操作',
      content: `您即将确认: ${stepLabel}，确认后不可撤销，是否继续？`
    })
    if (!confirmRes || !confirmRes.confirm) return
  }

  loading.value = true
  try {
    await post('/task/confirm', {
      taskId: taskId.value,
      stepType: selectedStep.value,
      remark: remark.value
    })

    if (uni.$ygtFeedback) uni.$ygtFeedback.success()
    uni.vibrateShort()
    uni.showToast({ title: `已确认：${stepLabel}`, icon: 'success' })

    // 更新本地状态
    const step = steps.value.find(s => s.value === selectedStep.value)
    if (step) {
      step.completed = true
      step.current = false
      const now = new Date()
      step.completedAt = String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0')

      // 激活下一个
      const nextIdx = steps.value.indexOf(step) + 1
      if (nextIdx < steps.value.length) {
        steps.value[nextIdx].current = true
        currentStep.value = steps.value[nextIdx].label
      } else {
        currentStep.value = '已完成'
      }
    }
    selectedStep.value = ''
    remark.value = ''

    setTimeout(() => {
      uni.navigateBack()
    }, 1200)
  } catch (e) {
    console.error('Confirm failed:', e)
    if (uni.$ygtFeedback) uni.$ygtFeedback.error()
    uni.vibrateLong()
    uni.showToast({ title: '确认失败：' + (e.message || '请重试'), icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
@import "../../uni.scss";

.confirm-page {
  min-height: 100vh;
  padding: 24rpx;
}

/* 任务信息卡 */
.task-info-card {
  background: #fff;
  border-radius: $ygt-radius-lg;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: $ygt-shadow-card;
}

.task-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
}

.task-row:first-child {
  border-bottom: 1rpx solid $ygt-gray-100;
}

.task-label {
  font-size: 28rpx;
  color: $ygt-gray-500;
}

.task-value {
  font-size: 32rpx;
  font-weight: 600;
  color: $ygt-gray-900;
}

.task-status {
  font-size: 28rpx;
  font-weight: 500;
  padding: 8rpx 20rpx;
  border-radius: 24rpx;
}

.status-pending {
  background: rgba(0, 102, 204, 0.1);
  color: $ygt-primary;
}

.status-running {
  background: rgba(217, 119, 6, 0.1);
  color: $ygt-warning;
}

.status-done {
  background: rgba(22, 163, 74, 0.1);
  color: $ygt-success;
}

/* 工序时间线 */
.step-timeline {
  background: #fff;
  border-radius: $ygt-radius-lg;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: $ygt-shadow-card;
}

.step-node {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 24rpx;
  border-radius: $ygt-radius-md;
  margin-bottom: 8rpx;
  position: relative;
  transition: all 0.2s;
}

.step-node:last-child {
  margin-bottom: 0;
}

.step-node.step-done {
  background: rgba(22, 163, 74, 0.04);
}

.step-node.step-current {
  background: rgba(0, 102, 204, 0.06);
  border: 2rpx solid rgba(0, 102, 204, 0.3);
  animation: pulse-current 2s ease-in-out infinite;
}

.step-node.step-pending {
  opacity: 0.6;
}

@keyframes pulse-current {
  0%, 100% { box-shadow: 0 0 0 0 rgba(0, 102, 204, 0.15); }
  50% { box-shadow: 0 0 0 8rpx rgba(0, 102, 204, 0); }
}

/* 连接线 */
.step-line {
  position: absolute;
  left: 56rpx;
  top: -16rpx;
  width: 2rpx;
  height: 32rpx;
  background: $ygt-gray-200;
}

.step-done .step-line {
  background: $ygt-success;
}

.step-circle {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: $ygt-gray-200;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s;
}

.step-done .step-circle {
  background: $ygt-success;
}

.step-current .step-circle {
  background: $ygt-primary;
}

.step-check {
  font-weight: 500;
  font-size: 36rpx;
  color: #fff;
}

.step-num {
  font-size: 30rpx;
  font-weight: 600;
  color: $ygt-gray-500;
}

.step-done .step-num,
.step-current .step-num {
  color: #fff;
}

.step-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.step-label {
  font-size: 30rpx;
  font-weight: 500;
  color: $ygt-gray-900;
}

.step-time {
  font-size: 24rpx;
  color: $ygt-success;
}

.step-hint {
  font-size: 24rpx;
  color: $ygt-primary;
}

.step-arrow {
  flex-shrink: 0;
}

.arrow-icon {
  font-weight: 500;
  font-size: 32rpx;
  color: $ygt-primary;
}

/* 备注 */
.remark-card {
  background: #fff;
  border-radius: $ygt-radius-lg;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: $ygt-shadow-card;
}

.remark-label {
  font-size: 28rpx;
  font-weight: 500;
  color: $ygt-gray-900;
  margin-bottom: 16rpx;
  display: block;
}

.remark-input {
  width: 100%;
  height: 180rpx;
  background: $ygt-gray-50;
  border-radius: $ygt-radius-md;
  padding: 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  border: 2rpx solid $ygt-gray-200;
}

.remark-count {
  font-size: 22rpx;
  color: $ygt-gray-500;
  text-align: right;
  margin-top: 8rpx;
  display: block;
}

/* 提交 */
.submit-area {
  padding: 24rpx 0 40rpx;
}

.submit-btn {
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  font-size: 32rpx;
}

.submit-btn[disabled] {
  background: $ygt-gray-300;
  color: $ygt-gray-500;
}
</style>
