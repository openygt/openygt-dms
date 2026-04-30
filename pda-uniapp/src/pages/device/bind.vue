<template>
  <view class="container">
    <view class="task-info">
      <text class="task-label" data-testid="bind-task-barcode">任务: {{ taskBarcode }}</text>
      <text class="step-label">当前工序: {{ stepLabel }}</text>
    </view>

    <view class="scan-area">
      <text class="scan-title">请扫描设备条码:</text>
      <view class="scan-box" @click="handleScan" data-testid="btn-scan-device">
        <text class="scan-icon">📷</text>
        <text class="scan-text">{{ deviceCode || '点击扫描设备码' }}</text>
      </view>
    </view>

    <!-- 最近使用设备 -->
    <view class="recent-devices" v-if="recentDevices.length > 0">
      <text class="section-title">或选择最近使用设备:</text>
      <view class="device-list">
        <view class="device-item" v-for="dev in recentDevices" :key="dev.deviceCode" @click="selectDevice(dev)">
          <text class="device-name">{{ dev.deviceName }}</text>
          <text class="device-code">{{ dev.deviceCode }}</text>
        </view>
      </view>
    </view>

    <button class="bind-btn" :disabled="!deviceCode || loading" :loading="loading" @click="handleBind" data-testid="btn-bind">确认绑定</button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { post } from '../../utils/request.js'

const taskId = ref('')
const taskBarcode = ref('')
const stepType = ref('')
const stepLabel = ref('')
const deviceCode = ref('')
const loading = ref(false)
const recentDevices = ref([])

onLoad((options) => {
  taskId.value = options.taskId || ''
  taskBarcode.value = options.barcode || ''
  stepType.value = options.stepType || ''
  stepLabel.value = getStepLabel(stepType.value)
  loadRecentDevices()
})

function getStepLabel(type) {
  const map = { 'START_DECOCT': '开始煎药', 'START_PACKAGE': '开始包装' }
  return map[type] || '设备绑定'
}

function loadRecentDevices() {
  const devices = uni.getStorageSync('pda_recent_devices') || []
  const expectedType = stepType.value === 'START_DECOCT' ? 'DECOCT' : 'PACKAGE'
  recentDevices.value = devices.filter(d => d.deviceType === expectedType).slice(0, 5)
}

function handleScan() {
  uni.scanCode({
    onlyFromCamera: true,
    scanType: ['qrCode', 'barCode'],
    success: (res) => { deviceCode.value = res.result; uni.vibrateShort() },
    fail: () => { uni.showToast({ title: '扫码失败', icon: 'none' }) }
  })
}

function selectDevice(dev) { deviceCode.value = dev.deviceCode }

async function handleBind() {
  if (!deviceCode.value) { uni.showToast({ title: '请先扫描或选择设备', icon: 'none' }); return }
  loading.value = true
  try {
    await post(`/tasks/${taskId.value}/bind-device`, {
      deviceCode: deviceCode.value,
      bindType: stepType.value === 'START_DECOCT' ? 'DECOCT' : 'PACKAGE'
    })
    cacheRecentDevice(deviceCode.value, stepType.value)
    uni.showToast({ title: '绑定成功', icon: 'success' })
    setTimeout(() => { uni.navigateBack() }, 800)
  } catch (e) {
    uni.showToast({ title: e.message || '绑定失败', icon: 'none' })
  } finally { loading.value = false }
}

function cacheRecentDevice(code, step) {
  const type = step === 'START_DECOCT' ? 'DECOCT' : 'PACKAGE'
  const name = type === 'DECOCT' ? '煎药机' : '包装机'
  let devices = uni.getStorageSync('pda_recent_devices') || []
  devices = devices.filter(d => d.deviceCode !== code)
  devices.unshift({ deviceCode: code, deviceName: name + '-' + code, deviceType: type, lastUsed: new Date().toISOString() })
  if (devices.length > 10) devices = devices.slice(0, 10)
  uni.setStorageSync('pda_recent_devices', devices)
}
</script>

<style scoped>
.container { padding: 30rpx; }
.task-info { background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 30rpx; }
.task-label { font-size: 30rpx; color: #333; font-weight: 500; display: block; }
.step-label { font-size: 28rpx; color: #0066CC; margin-top: 12rpx; display: block; }
.scan-area { background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 30rpx; }
.scan-title { font-size: 30rpx; color: #333; margin-bottom: 20rpx; display: block; }
.scan-box { height: 200rpx; border: 2rpx dashed #0066CC; border-radius: 16rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; background: #f0f7ff; }
.scan-icon { font-size: 56rpx; margin-bottom: 12rpx; }
.scan-text { font-size: 30rpx; color: #0066CC; }
.recent-devices { background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 30rpx; }
.section-title { font-size: 28rpx; color: #666; margin-bottom: 20rpx; display: block; }
.device-list { display: flex; flex-direction: column; gap: 16rpx; }
.device-item { padding: 24rpx; background: #f5f5f5; border-radius: 12rpx; display: flex; justify-content: space-between; align-items: center; }
.device-name { font-size: 30rpx; color: #333; }
.device-code { font-size: 26rpx; color: #999; }
.bind-btn { height: 96rpx; background: #0066CC; color: #fff; font-size: 34rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; }
.bind-btn[disabled] { background: #99c2e6; }
</style>
