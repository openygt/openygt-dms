<template>
  <view class="container">
    <view class="task-info">
      <text class="task-code">任务：{{ taskId }}</text>
      <text class="current-step">当前工序：{{ currentStep }}</text>
    </view>
    
    <view class="step-selector">
      <text class="section-title">选择要确认的工序</text>
      <view class="step-grid">
        <view 
          class="step-item" 
          v-for="step in steps" 
          :key="step.value"
          :class="{ active: selectedStep === step.value, disabled: step.disabled }"
          @click="selectStep(step)"
        >
          <text class="step-icon">{{ step.icon }}</text>
          <text class="step-name">{{ step.label }}</text>
        </view>
      </view>
    </view>
    
    <view class="remark-area">
      <text class="label">备注（可选）</text>
      <textarea 
        class="remark-input" 
        v-model="remark"
        placeholder="请输入备注信息"
        maxlength="200"
      />
    </view>
    
    <view class="submit-area">
      <button 
        class="submit-btn" 
        :loading="loading"
        :disabled="loading || !selectedStep"
        @click="handleConfirm"
      >
        确认工序
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { post } from '../../utils/request.js'

const taskId = ref('')
const currentStep = ref('煎药')
const selectedStep = ref('')
const remark = ref('')
const loading = ref(false)

const steps = ref([
  { value: 'START_SOAK', label: '开始泡药', icon: '💧', disabled: false },
  { value: 'END_SOAK', label: '结束泡药', icon: '✓', disabled: false },
  { value: 'START_DECOCT', label: '开始煎药', icon: '🔥', disabled: false },
  { value: 'END_DECOCT', label: '结束煎药', icon: '✓', disabled: false },
  { value: 'START_POUR', label: '开始出液', icon: '🍵', disabled: false },
  { value: 'END_POUR', label: '结束出液', icon: '✓', disabled: false },
  { value: 'START_PACKAGE', label: '开始包装', icon: '📦', disabled: false },
  { value: 'END_PACKAGE', label: '结束包装', icon: '✓', disabled: false },
  { value: 'LABEL_CONFIRM', label: '贴标确认', icon: '🏷', disabled: false }
])

onLoad((options) => {
  taskId.value = options.taskId || ''
})

function selectStep(step) {
  if (step.disabled) return
  selectedStep.value = step.value
}

async function handleConfirm() {
  if (!selectedStep.value) {
    uni.showToast({ title: '请选择工序', icon: 'none' })
    return
  }
  
  loading.value = true
  try {
    await post('/task/confirm', {
      taskId: taskId.value,
      stepType: selectedStep.value,
      remark: remark.value
    })
    uni.showToast({ title: '确认成功', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1000)
  } catch (e) {
    console.error('Confirm failed:', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 30rpx;
}

.task-info {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
}

.task-code {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
}

.current-step {
  font-size: 26rpx;
  color: #667eea;
  margin-top: 10rpx;
}

.step-selector {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
}

.step-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20rpx;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30rpx 0;
  background: #f8f8f8;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}

.step-item.active {
  border-color: #667eea;
  background: #e8eaf6;
}

.step-item.disabled {
  opacity: 0.4;
}

.step-icon {
  font-size: 48rpx;
  margin-bottom: 10rpx;
}

.step-name {
  font-size: 24rpx;
  color: #666;
}

.remark-area {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
}

.label {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 10rpx;
  display: block;
}

.remark-input {
  width: 100%;
  height: 160rpx;
  background: #f5f5f5;
  border-radius: 10rpx;
  padding: 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.submit-area {
  padding: 0 0 40rpx;
}

.submit-btn {
  height: 100rpx;
  line-height: 100rpx;
  background: #667eea;
  color: #fff;
  font-size: 32rpx;
  border-radius: 12rpx;
}

.submit-btn[disabled] {
  background: #a0a0a0;
}
</style>
